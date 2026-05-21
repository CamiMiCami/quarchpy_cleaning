"""
pam_quarchpy_stream.py  –  Native quarchpy PAM Stream Extension (No QIS)

This script uses the official `quarchpy` package for device discovery, USB
connection, and text commands (bypassing QIS by using the "PY" ConType).
It only injects a custom binary stream reader on top of the established connection.
"""

import struct, time, logging, sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Optional

import quarchpy

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s  %(levelname)-7s  %(message)s",
                    datefmt="%H:%M:%S")
log = logging.getLogger(__name__)

RAW_STRIPE_HEADER_LEN = 16

# ── Data Structures ──────────────────────────────────────────────────────────
@dataclass
class RawStripeDataHeader:
    header_length:            int
    elements_per_stripe:      int
    number_of_stripes:        int
    data_start_record_number: int

    @classmethod
    def from_bytes(cls, data: bytes) -> "RawStripeDataHeader":
        if len(data) < RAW_STRIPE_HEADER_LEN:
            raise ValueError("Incomplete header data")
        h, e, n, r = struct.unpack_from("<4i", data, 0)
        return cls(h, e, n, r)

@dataclass
class ChannelMeta:
    name:          str
    group:         str
    units:         str
    data_position: int

@dataclass
class StreamMeta:
    device_period_us: int = 4
    channels: list = field(default_factory=list)

# ── XML Parsers ──────────────────────────────────────────────────────────────
def parse_config_xml(xml_text: str) -> list:
    xml_text = xml_text.strip()
    start = xml_text.find("<?xml")
    if start == -1: start = xml_text.find("<")
    if start == -1: return []
    try:
        root = ET.fromstring(xml_text[start:])
    except ET.ParseError: return []

    channels, pos = [], 1
    for ch in root.iter("channel"):
        name = (ch.findtext("name") or "").strip()
        if name.lower() in ("status", "trigger", ""): continue
        channels.append(ChannelMeta(
            name=name,
            group=(ch.findtext("group") or "data").strip(),
            units=(ch.findtext("units") or "raw").strip(),
            data_position=pos
        ))
        pos += 1
    return channels

def generic_channels(n_elements: int) -> list:
    return [ChannelMeta(name=f"ch{i}", group="data", units="raw", data_position=i)
            for i in range(1, n_elements)]

# ── USB Recovery Function ────────────────────────────────────────────────────
def recover_quarch_usb(dev):
    """
    Aggressively flushes residual binary data and halts from previous
    crashed runs to ensure the pipe is pristine.
    """
    try:
        hw = dev.connectionObj.connection.Connection
        handle = hw.deviceHandle
        ep = hw.QCmdEP

        # Clear logical USB halts
        try: handle.clearHalt(ep)
        except: pass

        # Force hardware to halt any active streams natively
        try: hw.SendCommand("RECord TERMINATE")
        except: pass

        # Aggressively flush the pipe of all residual binary stripes
        while True:
            try:
                c = handle.bulkRead(ep, 4096, 50)
                if not c: break
            except Exception:
                break
    except Exception as e:
        log.debug("USB recovery skipped: %s", e)

# ── Quarchpy Stream Hijacker ─────────────────────────────────────────────────
class QuarchpyStreamer:
    """
    Hooks into an existing quarchpy connection to execute a high-speed
    binary stream without needing QIS.
    """
    def __init__(self, q_device):
        self.dev = q_device
        self.meta: Optional[StreamMeta] = None

        # Drill down through the quarchpy connection layers:
        # q_device -> PYConnection -> USBConn -> TQuarchUSB_IF
        try:
            self.hw_if = q_device.connectionObj.connection.Connection
        except AttributeError as e:
            raise ValueError(f"Streamer requires a native 'PY' quarchpy connection over USB. Error: {e}")

        if not hasattr(self.hw_if, 'deviceHandle') or not hasattr(self.hw_if, 'QCmdEP'):
            raise ValueError("Could not find the low-level USB handle in the quarchpy connection.")

    def discover_channels(self) -> StreamMeta:
        log.info("Querying channel layout via quarchpy (config:supports?)...")
        xml = self.dev.sendCommand("config:supports?")
        channels = parse_config_xml(xml) if xml else []

        if channels:
            log.info("  Got %d channels from device XML:", len(channels))
            for ch in channels:
                log.info("    [%d] %-18s group=%-10s units=%s",
                         ch.data_position, ch.name, ch.group, ch.units)
        else:
            log.info("  No channel XML — will derive from first stream packet.")

        self.meta = StreamMeta(device_period_us=4, channels=channels)
        return self.meta

    def stream_stripes(self, n: int = 200, timeout_s: float = 20.0):
        if self.meta is None:
            self.discover_channels()

        chan_by_pos = {ch.data_position: ch for ch in self.meta.channels}

        # 1. Enable stream mode
        self.dev.sendCommand("conf stream enable on")

        # 2. Fire 'rec stream' using standard quarchpy command.
        # This safely reads the "OK\n>" text response so the pipe is clean
        # right before the device starts blasting binary data.
        log.info("Starting hardware stream...")
        resp = self.dev.sendCommand("rec stream")
        if resp and "fail" in resp.lower():
            raise RuntimeError(f"Device refused to stream: {resp}")

        # 3. Hijack the low-level USB handle from quarchpy
        handle = self.hw_if.deviceHandle
        ep     = self.hw_if.QCmdEP

        collected = 0
        deadline  = time.monotonic() + timeout_s
        buf       = bytearray()
        ch_final  = bool(self.meta.channels)

        try:
            while collected < n and time.monotonic() < deadline:
                # Read raw binary chunks natively via libusb
                try:
                    chunk = handle.bulkRead(ep, 4096, 1000)
                    if chunk:
                        buf.extend(bytes(chunk))
                except Exception as exc:
                    if "TIMEOUT" not in str(exc).upper():
                        log.error("USB Read Error: %s", exc)
                    continue

                # Process packets
                while len(buf) >= RAW_STRIPE_HEADER_LEN:
                    hdr = RawStripeDataHeader.from_bytes(buf)
                    if hdr.header_length != RAW_STRIPE_HEADER_LEN:
                        log.warning("Bad header_length=%d — resyncing", hdr.header_length)
                        buf = buf[1:]; continue

                    stripe_sz = hdr.elements_per_stripe * 2
                    packet_sz = RAW_STRIPE_HEADER_LEN + (hdr.number_of_stripes * stripe_sz)
                    if len(buf) < packet_sz:
                        break

                    packet, buf = bytes(buf[:packet_sz]), buf[packet_sz:]

                    if not ch_final:
                        self.meta.channels = generic_channels(hdr.elements_per_stripe)
                        chan_by_pos = {ch.data_position: ch for ch in self.meta.channels}
                        ch_final = True
                        log.info("Derived %d generic channels (elementsPerStripe=%d)",
                                 len(self.meta.channels), hdr.elements_per_stripe)

                    off = RAW_STRIPE_HEADER_LEN
                    for si in range(hdr.number_of_stripes):
                        if collected >= n: break
                        yield self._decode(packet[off:off+stripe_sz],
                                           hdr.data_start_record_number + si,
                                           chan_by_pos)
                        off += stripe_sz; collected += 1

        finally:
            log.info("Stopping hardware stream (%d stripes collected)...", collected)

            # Fire stop command natively using raw SendCommand.
            # This prevents quarchpy from crashing by trying to UTF-8 decode
            # residual binary data currently stuck in the pipe.
            try:
                self.hw_if.SendCommand("rec stop")
            except Exception:
                pass

            # Flush residual binary stripes and the final "OK\n>"
            self._flush(handle, ep, 150)

            # Safely resync quarchpy's prompt expectation
            try:
                self.dev.sendCommand("")
            except Exception:
                pass

    def _flush(self, handle, ep, timeout_ms: int):
        """Drains the physical USB pipe."""
        while True:
            try:
                c = handle.bulkRead(ep, 4096, timeout_ms)
                if not c: break
            except Exception:
                break

    def _decode(self, raw: bytes, rec_no: int, chan_by_pos: dict) -> dict:
        if len(raw) < 2: return {}
        status  = struct.unpack_from("<H", raw, 0)[0]
        trigger = bool(status & 0x4000)
        channels = {}
        for pos in range(1, len(raw) // 2):
            if pos*2+2 > len(raw): break
            val = struct.unpack_from("<h", raw, pos*2)[0]
            ch  = chan_by_pos.get(pos)
            key = f"{ch.name}:{ch.group}" if ch else f"elem_{pos}"
            channels[key] = {
                "value": val,
                "units": ch.units if ch else "raw",
                "name":  ch.name  if ch else f"elem_{pos}",
            }
        return {
            "record_number": rec_no,
            "time_us":       rec_no * self.meta.device_period_us,
            "trigger":       trigger,
            "channels":      channels,
        }

# ── Main Example ─────────────────────────────────────────────────────────────
def main():
    print("=" * 65)
    print("  Quarch PAM – Native quarchpy Binary Streamer")
    print("=" * 65)

    # 1. Open the device using standard quarchpy
    log.info("Connecting to Quarch device...")
    found_device = quarchpy.scanDevices("all")

    if not found_device:
        log.error("No Quarch devices found.")
        sys.exit(1)

    target_id = list(found_device)[0]
    print(f"Found device: {target_id}")

    # Initialize native Python connection (no QIS)
    dev = quarchpy.quarchDevice(ConString=target_id, ConType="PY")

    # USB Recovery: Scrub the pipe of residual data from previous crashes
    log.info("Running USB state recovery...")
    recover_quarch_usb(dev)

    log.info("Device Connected: %s", dev.sendCommand("*idn?").strip())
    log.info("Power State: %s", dev.sendCommand("run:power?").strip())

    # 2. Attach our custom streamer to the quarchpy connection
    streamer = QuarchpyStreamer(dev)

    try:
        hdr_done = False
        # Requesting 50 stripes
        for stripe in streamer.stream_stripes(n=50, timeout_s=10):
            items = list(stripe["channels"].values())

            # Print column headers on first stripe
            if not hdr_done:
                cols = [f"{v['name']}({v['units']})" for v in items]
                print(f"  {'rec#':>6}  {'time µs':>9}  T  " + "  ".join(f"{c:>14}" for c in cols))
                print("  " + "-"*70)
                hdr_done = True

            # Print row values
            vals = [str(v["value"]) for v in items]
            print(f"  {stripe['record_number']:>6}  {stripe['time_us']:>9}  "
                  f"{'T' if stripe['trigger'] else '.'}  " +
                  "  ".join(f"{v:>14}" for v in vals))
    finally:
        log.info("Closing quarchpy connection...")
        dev.closeConnection()

if __name__ == "__main__":
    main()