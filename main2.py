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

def _parse_time_us(s: str) -> int:
    s = s.strip().lower()
    if s.endswith("ns"): return max(1, int(s[:-2]) // 1000)
    if s.endswith("us"): return int(s[:-2])
    if s.endswith("ms"): return int(s[:-2]) * 1000
    if s.endswith("s"):  return int(s[:-1]) * 1_000_000
    return int(s) if s.isdigit() else 4

# ── XML Parsers ──────────────────────────────────────────────────────────────
def parse_stream_xml(xml_text: str) -> StreamMeta:
    """Parses 'stream text header' XML from Quarch modules."""
    xml_text = xml_text.strip()
    start = xml_text.find("<?xml")
    if start == -1: start = xml_text.find("<header")
    if start == -1: return StreamMeta()

    try:
        root = ET.fromstring(xml_text[start:])
    except ET.ParseError: 
        return StreamMeta()

    dp_str = root.findtext("devicePeriod") or root.findtext("devicePerioduS") or "4us"
    dp_us = _parse_time_us(dp_str)

    channels = []
    for ch in root.iter("channel"):
        name = (ch.findtext("name") or "").strip()
        if name.lower() in ("status", "trigger", ""): continue
        
        # Safely extract the exact integer index for this channel
        pos_str = ch.findtext("dataPosition")
        pos = int(pos_str) if pos_str and pos_str.isdigit() else (len(channels) + 1)

        channels.append(ChannelMeta(
            name=name,
            group=(ch.findtext("group") or "data").strip(),
            units=(ch.findtext("units") or "raw").strip(),
            data_position=pos
        ))
    return StreamMeta(device_period_us=dp_us, channels=channels)

def generic_channels(n_elements: int) -> list:
    return [ChannelMeta(name=f"ch{i}", group="data", units="raw", data_position=i)
            for i in range(1, n_elements)]

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
        log.info("Querying channel layout via quarchpy (stream text header)...")
        self.dev.sendCommand("stream mode header v3")
        xml = self.dev.sendCommand("stream text header")
        
        self.meta = parse_stream_xml(xml) if xml else StreamMeta()

        if self.meta.channels:
            log.info("  Got %d channels from device XML:", len(self.meta.channels))
            for ch in self.meta.channels:
                log.info("    [%d] %-18s group=%-10s units=%s",
                         ch.data_position, ch.name, ch.group, ch.units)
        else:
            log.info("  No channel XML — will derive from first stream packet.")

        return self.meta

    def stream_stripes(self, n: int = 200, timeout_s: float = 20.0):
        if self.meta is None:
            self.discover_channels()

        chan_by_pos = {ch.data_position: ch for ch in self.meta.channels}

        # 1. Enable stream mode
        self.dev.sendCommand("conf stream enable on")

        # 2. Fire the hardware binary start command.
        # MUST use RECord RUN (hardware command), not 'rec stream' (QIS virtual command)!
        log.info("Starting hardware stream...")
        self.hw_if.RunCommand("RECord RUN", expectedResponse=False)

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
                    # Search for the 16-byte header signature to bypass the "OK\r\n>" text response
                    # Since headerLength is always 16, we slide 1 byte forward until we hit it.
                    h_len = struct.unpack_from("<i", buf, 0)[0]
                    if h_len != RAW_STRIPE_HEADER_LEN:
                        buf = buf[1:]
                        continue

                    hdr = RawStripeDataHeader.from_bytes(buf)

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

            # Fire hardware stop command
            try:
                self.hw_if.RunCommand("RECord STOP", expectedResponse=False)
            except Exception:
                pass

            # Flush residual binary stripes and the final text prompt
            self._flush(handle, ep, 250)

            # Safely resync quarchpy's prompt expectation so it can send commands again
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

    log.info("Connecting to Quarch device...")
    found_device = quarchpy.scanDevices("all")
    
    if not found_device:
        log.error("No Quarch devices found.")
        sys.exit(1)
        
    target_id = list(found_device)[0]
    print(f"Found device: {target_id}")
    
    # Initialize native Python connection (no QIS)
    try:
        dev = quarchpy.quarchDevice(ConString=target_id, ConType="PY")
    except Exception as e:
        log.error("quarchDevice failed to initialize. The USB pipe is likely halted.")
        log.error("ACTION REQUIRED: Please UNPLUG the Quarch module from power/USB, wait 5s, and plug it back in.")
        sys.exit(1)

    log.info("Device Connected: %s", dev.sendCommand("*idn?").strip())
    log.info("Power State: %s", dev.sendCommand("run:power?").strip())

    # Attach our custom streamer to the quarchpy connection
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
        dev.close()

if __name__ == "__main__":
    main()