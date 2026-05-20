"""
pam_usb_stream.py
=================
Prototype: read raw binary stripes from a Quarch PAM (QTL2098/2312/2582/etc.)
over USB without QIS running.

Java reference chain
--------------------
  qisMain.java              startBackEndInterfaces()
    → USBStream.java        run() tight loop calling readStream()
    → CommsDeviceInfo.java  readStream() → qDevice.readStream()
    → QbeInterfaceWrapper   actionCommandPreamble()
        sends: "conf stream enable on"  then  "rec stream"
  RawStripeDataHeader.java  header layout (4 × LE int32)
  BaseStreamDevicePPM.java  stripe interpretation

USB protocol (PAM over USB)
----------------------------
After "rec stream" the device begins pushing binary packets on the SAME
bulk endpoint used for commands.  Each packet is:

  [ RawStripeDataHeader (16 bytes) ][ stripe data ]

RawStripeDataHeader (RawStripeDataHeader.java, all little-endian int32):
  offset 0  : headerLength        – always 16
  offset 4  : elementsPerStripe   – number of int16 values per stripe
  offset 8  : numberOfStripes     – stripes in this packet
  offset 12 : dataStartRecordNumber

Each stripe = elementsPerStripe × 2 bytes (int16 LE).

Element layout for PAM (from stream text header XML, element 0 = status word):
  element 0  : status  (bit 14 = trigger, rest = flags)
  elements 1+: channel data in the order listed in the XML header

The XML header is fetched via the text command "stream text header" which
returns a V3 XML block.  We parse it to know units and channel names.

How to run
----------
  pip install pyusb libusb (or install libusb system package)
  python pam_usb_stream.py

  On Linux you may need: sudo python or a udev rule for Quarch USB.
"""

import struct
import time
import logging
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from timeit import default_timer as timer
from typing import Optional

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-7s  %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────────────────────
# Constants pulled from Java source
# ─────────────────────────────────────────────────────────────────────────────
QUARCH_VENDOR_ID  = 0x16D0
QUARCH_PRODUCT_ID = 0x0449

RAW_STRIPE_HEADER_LEN = 16   # RawStripeDataHeader.java: 4 × int32
USB_CMD_EP_SIZE       = 64   # QCmdEPSize for non-PIC32 devices
USB_TIMEOUT_MS        = 5000


# ─────────────────────────────────────────────────────────────────────────────
# RawStripeDataHeader  (RawStripeDataHeader.java)
# ─────────────────────────────────────────────────────────────────────────────
@dataclass
class RawStripeDataHeader:
    header_length:          int   # always 16
    elements_per_stripe:    int   # number of int16 values per stripe
    number_of_stripes:      int   # stripes in this USB packet
    data_start_record_number: int

    @classmethod
    def from_bytes(cls, data: bytes) -> "RawStripeDataHeader":
        """
        streamBytesToInt() in Java does LE assembly byte by byte.
        struct '<4i' is identical and faster.
        """
        if len(data) < RAW_STRIPE_HEADER_LEN:
            raise ValueError(
                f"Need {RAW_STRIPE_HEADER_LEN} bytes for header, got {len(data)}"
            )
        h, e, n, r = struct.unpack_from("<4i", data, 0)
        return cls(h, e, n, r)


# ─────────────────────────────────────────────────────────────────────────────
# Stream XML header parser  (BaseStreamDevicePPM.java  getV3HeaderStrings)
# ─────────────────────────────────────────────────────────────────────────────
@dataclass
class ChannelMeta:
    name:          str
    group:         str
    units:         str
    max_t_value:   int
    data_position: int   # index into the stripe (0-based, 0 = status)


@dataclass
class StreamMeta:
    device_period_str: str          # e.g. "4us"
    device_period_us:  int
    channels:          list[ChannelMeta] = field(default_factory=list)


def parse_stream_xml_header(xml_text: str) -> StreamMeta:
    """
    Parse the V3 XML header returned by 'stream text header'.

    The XML looks like (from BaseStreamDevicePPM.java getV3HeaderStrings):

      <?xml version="1.0" ...?>
      <header>
        <version>V3</version>
        <devicePeriod>4us</devicePeriod>
        <mainPeriod>4us</mainPeriod>
        ...
        <channels>
          <channel>
            <name>Status</name><group>status</group>
            <units>NA</units><maxTValue>0</maxTValue>
            <dataPosition>0</dataPosition>
          </channel>
          <channel>
            <name>5V</name><group>voltage</group>
            <units>mV</units><maxTValue>...</maxTValue>
            <dataPosition>1</dataPosition>
          </channel>
          ...
        </channels>
      </header>
    """
    # Strip any leading cursor characters QIS might have added
    xml_text = xml_text.strip().lstrip(">").strip()

    xml_start = xml_text.find("<?xml")
    if xml_start == -1:
        xml_start = xml_text.find("<header")

    if xml_start == -1:
        # Prevent blind crash, show exactly what garbage we received
        raise ValueError(f"Failed to find XML start tag. Raw response was: {xml_text[:200]!r}")

    if xml_start > 0:
        xml_text = xml_text[xml_start:]

    root = ET.fromstring(xml_text)

    # Device period
    dp_el = root.find(".//devicePeriod")
    if dp_el is None:
        dp_el = root.find(".//devicePerioduS")   # older tag name
    device_period_str = dp_el.text.strip() if dp_el is not None else "4us"

    # Convert period string like "4us" / "100us" to integer microseconds
    device_period_us = _parse_time_us(device_period_str)

    # Channels
    channels = []
    for chan_el in root.findall(".//channel"):
        def _txt(tag):
            el = chan_el.find(tag)
            return el.text.strip() if el is not None else ""

        channels.append(ChannelMeta(
            name          = _txt("name"),
            group         = _txt("group"),
            units         = _txt("units"),
            max_t_value   = int(_txt("maxTValue") or 0),
            data_position = int(_txt("dataPosition") or 0),
        ))

    return StreamMeta(
        device_period_str = device_period_str,
        device_period_us  = device_period_us,
        channels          = channels,
    )


def _parse_time_us(s: str) -> int:
    """Convert '4us', '100us', '1ms', '4000ns' → integer microseconds."""
    s = s.strip().lower()
    if s.endswith("ns"):
        return max(1, int(s[:-2]) // 1000)
    if s.endswith("us"):
        return int(s[:-2])
    if s.endswith("ms"):
        return int(s[:-2]) * 1000
    if s.endswith("s"):
        return int(s[:-1]) * 1_000_000
    return int(s)   # fallback: assume µs


# ─────────────────────────────────────────────────────────────────────────────
# Low-level USB handle  (mirrors TQuarchUSB_IF in connection_USB.py)
# ─────────────────────────────────────────────────────────────────────────────
class QuarchUSBHandle:
    """
    Minimal USB handle wrapping usb1 (libusb).

    We open ONE handle for the PAM.  Commands go via bulk-write/read on the
    command endpoint.  After 'rec stream' the device pushes binary data back
    on the SAME endpoint – we just keep bulk-reading until we decide to stop.
    """

    # Lock / unlock magic bytes (from TQuarchUSB_IF)
    _LOCK_CMD   = b'\x02\x00\x00\x01\x04\x01'
    _UNLOCK_CMD = b'\x02\x00\x00\x01\x03\x00'

    def __init__(self, context, device):
        self._ctx    = context
        self._dev    = device
        self._handle = None
        self._ep     = 0
        self._ep_sz  = USB_CMD_EP_SIZE
        self._iface  = 0

        self.last_command_time = timer()

    def open(self) -> bool:
        import platform
        self._handle = self._dev.open()
        if self._handle is None:
            return False

        if platform.system() == "Linux":
            if self._handle.kernelDriverActive(self._iface):
                self._handle.detachKernelDriver(self._iface)

        self._handle.claimInterface(self._iface)

        # Find the highest-numbered OUT endpoint (same logic as TQuarchUSB_IF)
        ep_addr = 0
        for cfg in self._dev.iterConfiguations():
            if cfg.getConfigurationValue() != 1:
                continue
            for intf in cfg.iterInterfaces():
                for setting in intf.iterSettings():
                    if setting.getNumber() != 0:
                        continue
                    for ep in setting.iterEndpoints():
                        addr = ep.getAddress()
                        if ep_addr < addr < 0x80:   # OUT endpoint
                            ep_addr = addr
        if ep_addr:
            self._ep = ep_addr

        self._ep_sz = self._dev.getMaxPacketSize(self._ep)
        log.debug("USB EP=0x%02x  EP_SIZE=%d", self._ep, self._ep_sz)

        # Lock the device into USB mode (PIC32 devices need the lock sequence)
        if self._ep_sz <= 64:
            self._bulk_write(self._UNLOCK_CMD)
            self._bulk_read(self._ep_sz, timeout=500)
            self._bulk_write(self._LOCK_CMD)
            self._bulk_read(self._ep_sz, timeout=500)

        return True

    def close(self):
        if self._handle is None:
            return
        if self._ep_sz <= 64:
            try:
                self._bulk_write(self._UNLOCK_CMD)
                self._bulk_read(self._ep_sz, timeout=500)
            except Exception:
                pass
        try:
            self._handle.releaseInterface(self._iface)
        except Exception:
            pass
        self._handle = None

    def send_command(self, cmd: str, expected_response: bool = True) -> str:
        """
        Send a text command. If expected_response is True, wait for the '>' prompt.
        """
        if (timer() - self.last_command_time < 0.015):
            time.sleep(0.015)

        padded = cmd.ljust(64, "\0").encode("utf-8")
        self._bulk_write(padded)

        response_text = ""
        if expected_response:
            while True:
                chunk = self._bulk_read(self._ep_sz, timeout=USB_TIMEOUT_MS)

                # If we hit a timeout, chunk is b"". Break out to avoid infinite loops.
                if not chunk:
                    break

                # EXACTLY mirror the quarchpy chunk evaluation logic
                decoded = chunk.decode("utf-8", errors="replace")
                processed_chunk = decoded.strip('\0').strip() + '\r\n'

                # If this specific chunk is the standalone prompt, we are done!
                if processed_chunk.startswith('>\r\n'):
                    break
                else:
                    response_text += processed_chunk

        self.last_command_time = timer()

        # Clean off the trailing \r\n we added and return
        return response_text.rstrip("\0> \r\n") if expected_response else ""

    def read_raw(self, n_bytes: int, timeout_ms: int = USB_TIMEOUT_MS) -> bytes:
        """
        Read raw bytes from the USB bulk endpoint.
        Used for stream data after 'rec stream' is sent.
        """
        return self._bulk_read(n_bytes, timeout=timeout_ms)

    # ── private ────────────────────────────────────────────────────────────
    def _bulk_write(self, data: bytes):
        self._handle.bulkWrite(
            endpoint = self._ep,
            data     = data,
            timeout  = USB_TIMEOUT_MS,
        )

    def _bulk_read(self, n: int, timeout: int = USB_TIMEOUT_MS) -> bytes:
        try:
            return bytes(self._handle.bulkRead(self._ep, n, timeout))
        except Exception as exc:
            log.debug("bulk_read timeout/error: %s", exc)
            return b""


# ─────────────────────────────────────────────────────────────────────────────
# Stream session
# ─────────────────────────────────────────────────────────────────────────────
class PAMStreamSession:
    """
    Opens the PAM over USB, configures it, starts streaming,
    reads N stripes, stops, and yields decoded results.

    Mirrors the sequence in QbeInterfaceWrapper.actionCommandPreamble()
    + USBStream.run().
    """

    def __init__(self, usb_handle: QuarchUSBHandle):
        self._h = usb_handle
        self.meta: Optional[StreamMeta] = None

    # ── setup ──────────────────────────────────────────────────────────────
    def configure(self):
        """Send pre-stream configuration commands (from actionCommandPreamble)."""
        log.info("Enabling stream interface on device...")
        r = self._h.send_command("conf stream enable on")
        log.info("  conf stream enable on → %s", r)

    def get_stream_header(self) -> StreamMeta:
        log.info("Fetching stream XML header...")

        self._h.send_command("stream mode header v3")
        xml_raw = self._h.send_command("stream text header")

        if "Not Available" in xml_raw or "<?xml" not in xml_raw:
            log.warning("XML header not available yet – running dummy stream to initialize...")

            # Fire and forget: do NOT wait for text, because binary is about to hit the pipe
            self._h.send_command("RECord RUN", expected_response=False)
            time.sleep(0.1)
            self._h.send_command("RECord STOP", expected_response=False)
            time.sleep(0.1)

            # Flush the USB pipe of all residual binary stripes
            flushed_bytes = 0
            while True:
                junk = self._h.read_raw(4096, timeout_ms=50)
                if not junk:
                    break
                flushed_bytes += len(junk)
            log.debug("Flushed %d bytes of residual binary data from USB pipe", flushed_bytes)

            # Send a blank command to force the module to emit a fresh '>' prompt to sync up
            self._h.send_command("")

            # Now safely request the XML
            xml_raw = self._h.send_command("stream text header")

        self.meta = parse_stream_xml_header(xml_raw)
        log.info("Stream meta: period=%s  channels=%d",
                 self.meta.device_period_str, len(self.meta.channels))
        for ch in self.meta.channels:
            log.info("  [%d] %-20s  group=%-10s  units=%s",
                     ch.data_position, ch.name, ch.group, ch.units)
        return self.meta

    # ── stream ─────────────────────────────────────────────────────────────
    def stream_stripes(self, n_stripes: int = 100, timeout_s: float = 10.0):
        """
        Start streaming, collect n_stripes decoded stripe dicts, then stop.

        Yields dicts like:
          {
            'record_number': 42,
            'time_us':       168,
            'trigger':       False,
            'channels': {
                '5V voltage': {'value': 4987, 'units': 'mV'},
                '5V current': {'value':  312, 'units': 'mA'},
                ...
            }
          }
        """
        if self.meta is None:
            self.get_stream_header()

        # Build lookup: data_position → ChannelMeta (skip status at position 0)
        chan_by_pos = {ch.data_position: ch for ch in self.meta.channels
                       if ch.data_position > 0}

        log.info("Starting stream (target=%d stripes)...", n_stripes)
        r = self._h.send_command("RECord RUN")
        log.info("  RECord RUN → %s", r)

        collected   = 0
        deadline    = time.monotonic() + timeout_s
        raw_buffer  = bytearray()

        try:
            while collected < n_stripes and time.monotonic() < deadline:
                # ── read a chunk from the USB bulk endpoint ──────────────
                # The device sends RawStripeDataHeader(16) + stripe data
                # packets continuously.  We read in large chunks and
                # reassemble packets from the byte stream.
                chunk = self._h.read_raw(4096, timeout_ms=1000)
                if chunk:
                    raw_buffer.extend(chunk)

                # ── consume complete packets from the buffer ─────────────
                while len(raw_buffer) >= RAW_STRIPE_HEADER_LEN:
                    hdr = RawStripeDataHeader.from_bytes(raw_buffer)

                    # Sanity check the header
                    if hdr.header_length != RAW_STRIPE_HEADER_LEN:
                        # Out of sync – discard one byte and try again
                        log.warning("Header length mismatch (%d) – resyncing",
                                    hdr.header_length)
                        raw_buffer = raw_buffer[1:]
                        continue

                    stripe_sz    = hdr.elements_per_stripe * 2  # int16 = 2 bytes
                    packet_sz    = RAW_STRIPE_HEADER_LEN + hdr.number_of_stripes * stripe_sz

                    if len(raw_buffer) < packet_sz:
                        break   # wait for more data

                    # Extract the full packet
                    packet = raw_buffer[:packet_sz]
                    raw_buffer = raw_buffer[packet_sz:]

                    # Decode each stripe in this packet
                    data_offset = RAW_STRIPE_HEADER_LEN
                    for s_idx in range(hdr.number_of_stripes):
                        if collected >= n_stripes:
                            break

                        stripe_raw = packet[data_offset : data_offset + stripe_sz]
                        data_offset += stripe_sz

                        decoded = self._decode_stripe(
                            stripe_raw,
                            hdr.data_start_record_number + s_idx,
                            chan_by_pos,
                        )
                        yield decoded
                        collected += 1

        finally:
            log.info("Stopping stream (collected %d stripes)...", collected)
            self._h.send_command("RECord STOP")
            # Drain any residual bytes so the next command channel is clean
            time.sleep(0.1)
            self._h.read_raw(4096, timeout_ms=200)

    # ── decode ─────────────────────────────────────────────────────────────
    def _decode_stripe(self, raw: bytes, record_number: int,
                       chan_by_pos: dict) -> dict:
        """
        Decode one stripe.

        Element 0 is always the status word:
          bit 15 = valid
          bit 14 = trigger
          bits 0-13 = status flags

        Elements 1+ are channel data values.
        Units and scale come from the XML header – the raw int16 value
        is already in the units stated (mV, mA, etc.) because the PAM
        firmware applies scaling internally before streaming.
        """
        if len(raw) < 2:
            return {}

        # Status word (element 0)
        status_raw = struct.unpack_from("<H", raw, 0)[0]
        trigger    = bool(status_raw & 0x4000)
        valid      = bool(status_raw & 0x8000)

        channels = {}
        n_elements = len(raw) // 2

        for pos in range(1, n_elements):
            if pos * 2 + 2 > len(raw):
                break
            raw_val = struct.unpack_from("<h", raw, pos * 2)[0]  # signed int16

            ch = chan_by_pos.get(pos)
            if ch:
                channels[f"{ch.name} {ch.group}"] = {
                    "value": raw_val,
                    "units": ch.units,
                    "name":  ch.name,
                    "group": ch.group,
                }
            else:
                channels[f"element_{pos}"] = {"value": raw_val, "units": "raw"}

        return {
            "record_number": record_number,
            "time_us":       record_number * (self.meta.device_period_us
                                              if self.meta else 4),
            "trigger":       trigger,
            "valid":         valid,
            "channels":      channels,
        }


# ─────────────────────────────────────────────────────────────────────────────
# Device scanner  (mirrors list_USB in scanDevices.py)
# ─────────────────────────────────────────────────────────────────────────────
def find_pam_device():
    """
    Find the first Quarch PAM on USB.
    Returns (context, device) or raises RuntimeError.
    """
    try:
        from quarchpy.connection_specific.connection_USB import importUSB
        usb1 = importUSB()
    except Exception as exc:
        raise RuntimeError(f"Cannot import usb1/libusb: {exc}") from exc

    ctx  = usb1.USBContext()
    devs = ctx.getDeviceList()

    for dev in devs:
        if (hex(dev.device_descriptor.idVendor)  == hex(QUARCH_VENDOR_ID) and
            hex(dev.device_descriptor.idProduct) == hex(QUARCH_PRODUCT_ID)):

            # Try to read the serial number to confirm it's a PAM
            try:
                h = dev.open()
                sn = h.getASCIIStringDescriptor(3)
                h.close()
                log.info("Found Quarch device: %s", sn)
                # PAM / MOM device numbers: 2098, 2312, 2582, 2602, 2789 etc.
                pam_ids = ["2098", "2312", "2582", "2602", "2789", "2751",
                           "2834", "2843"]
                if any(pid in sn for pid in pam_ids):
                    log.info("Identified as PAM/MOM device.")
                    return ctx, dev
                else:
                    log.warning("Device %s does not look like a PAM – using it anyway",
                                sn)
                    return ctx, dev   # try it anyway for prototyping
            except Exception as exc:
                log.warning("Could not read serial number: %s", exc)
                return ctx, dev

    raise RuntimeError("No Quarch USB device found. "
                       "Check USB connection and permissions.")


# ─────────────────────────────────────────────────────────────────────────────
# Main demo
# ─────────────────────────────────────────────────────────────────────────────
def main():
    print("=" * 65)
    print("  Quarch PAM  –  Pure Python USB Binary Stream (no QIS)")
    print("=" * 65)

    N_STRIPES = 200   # how many stripes to collect

    # 1. Find the device
    log.info("Scanning USB for Quarch PAM...")
    ctx, dev = find_pam_device()

    # 2. Open USB handle
    handle = QuarchUSBHandle(ctx, dev)
    if not handle.open():
        log.error("Failed to open USB handle.")
        sys.exit(1)
    log.info("USB handle opened.")

    session = PAMStreamSession(handle)

    try:
        # 3. Pre-stream configuration (mirrors actionCommandPreamble)
        session.configure()

        # 4. Fetch the XML header so we know channel names/units
        meta = session.get_stream_header()

        # 5. Stream N stripes and print them
        print(f"\nCollecting {N_STRIPES} stripes "
              f"(period={meta.device_period_str})...\n")

        header_printed = False
        for stripe in session.stream_stripes(n_stripes=N_STRIPES, timeout_s=15):
            if not header_printed:
                # Print column headers on first stripe
                ch_names = [f"{v['name']} ({v['units']})"
                            for v in stripe["channels"].values()]
                print(f"  {'rec#':>6}  {'time µs':>9}  {'trig':>4}  " +
                      "  ".join(f"{n:>18}" for n in ch_names))
                print("  " + "-" * (6 + 9 + 4 + 18 * len(ch_names) + 20))
                header_printed = True

            ch_vals = [str(v["value"]) for v in stripe["channels"].values()]
            print(f"  {stripe['record_number']:>6}  "
                  f"{stripe['time_us']:>9}  "
                  f"{'T' if stripe['trigger'] else '.':>4}  " +
                  "  ".join(f"{v:>18}" for v in ch_vals))

    finally:
        handle.close()
        log.info("USB handle closed.")

    print("\nDone.")


if __name__ == "__main__":
    main()