"""
pam_usb_stream.py  –  Pure Python PAM USB binary stream (no QIS)

Key fixes vs previous version
------------------------------
1. Command: 'rec stream' not 'RECord RUN'
   QbeInterfaceWrapper.isActionCmd() triggers on startsWith("rec") && endsWith("stream").
   'RECord RUN' is the PPM internal-buffer command and does nothing on PAM.

2. Channel layout: 'config:supports?' (hardware command, QuarchDeviceInfo.getConfigurationXML())
   not 'stream text header' which is a QIS-layer command unavailable without QIS.
   Fallback: derive generic channel names from elementsPerStripe in the first packet.

3. Command pacing: 15 ms minimum gap (RunCommand in connection_USB.py).

4. Cursor detection: check if the CURRENT chunk starts with >\r\n (standalone prompt),
   not whether the accumulated buffer ends with >.  Prevents premature termination
   when XML responses contain embedded > characters.
   Mirrors Java FetchCmdReplyTOut exactly.

5. Endpoint direction: libusb Python wrapper handles direction internally;
   the same address is used for bulkRead and bulkWrite (matches connection_USB.py).
"""

import struct, time, logging, sys, xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from timeit import default_timer as timer
from typing import Optional

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s  %(levelname)-7s  %(message)s",
                    datefmt="%H:%M:%S")
log = logging.getLogger(__name__)

# ── constants ────────────────────────────────────────────────────────────────
QUARCH_VENDOR_ID      = 0x16D0
QUARCH_PRODUCT_ID     = 0x0449
RAW_STRIPE_HEADER_LEN = 16       # 4 × LE int32  (RawStripeDataHeader.java)
USB_CMD_EP_SIZE       = 64
USB_TIMEOUT_MS        = 5000
CMD_PACE_S            = 0.015    # 15 ms  (RunCommand in connection_USB.py)

PAM_IDS = ["2098","2312","2582","2602","2789","2751","2834","2843"]


# ── RawStripeDataHeader  (RawStripeDataHeader.java) ──────────────────────────
@dataclass
class RawStripeDataHeader:
    header_length:            int
    elements_per_stripe:      int
    number_of_stripes:        int
    data_start_record_number: int

    @classmethod
    def from_bytes(cls, data: bytes) -> "RawStripeDataHeader":
        if len(data) < RAW_STRIPE_HEADER_LEN:
            raise ValueError(f"Need 16 bytes for header, got {len(data)}")
        h, e, n, r = struct.unpack_from("<4i", data, 0)
        return cls(h, e, n, r)


# ── channel metadata ─────────────────────────────────────────────────────────
@dataclass
class ChannelMeta:
    name:          str
    group:         str
    units:         str
    data_position: int   # 0=status, 1+ = data

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
    return int(s)


# ── parse config:supports? XML ───────────────────────────────────────────────
def parse_config_xml(xml_text: str) -> list:
    """
    Extract channel info from 'config:supports?' response.
    Returns list[ChannelMeta] or [] if parsing fails.
    QuarchDeviceInfo.getConfigurationXML() sends this command.
    """
    xml_text = xml_text.strip()
    start = xml_text.find("<?xml")
    if start == -1: start = xml_text.find("<")
    if start == -1: return []
    try:
        root = ET.fromstring(xml_text[start:])
    except ET.ParseError as exc:
        log.warning("config:supports? XML parse error: %s", exc)
        return []

    channels, pos = [], 1
    for ch in root.iter("channel"):
        name_el = ch.find("name")
        if name_el is None: continue
        name = (name_el.text or "").strip()
        if name.lower() in ("status", "trigger", ""): continue
        group = (ch.findtext("group") or "data").strip()
        units = (ch.findtext("units") or "raw").strip()
        channels.append(ChannelMeta(name=name, group=group,
                                    units=units, data_position=pos))
        pos += 1
    return channels


def generic_channels(n_elements: int) -> list:
    """Fallback: label channels ch1, ch2, … from elementsPerStripe."""
    return [ChannelMeta(name=f"ch{i}", group="data", units="raw", data_position=i)
            for i in range(1, n_elements)]


# ── USB handle ───────────────────────────────────────────────────────────────
class QuarchUSBHandle:
    _LOCK   = b'\x02\x00\x00\x01\x04\x01'
    _UNLOCK = b'\x02\x00\x00\x01\x03\x00'

    def __init__(self, context, device):
        self._ctx, self._dev = context, device
        self._handle = None
        self._ep = 0
        self._ep_sz = USB_CMD_EP_SIZE
        self._iface = 0
        self._last_cmd = timer()          # Fix 3: pacing

    def open(self) -> bool:
        import platform
        self._handle = self._dev.open()
        if self._handle is None:
            return False
        if platform.system() == "Linux":
            try:
                if self._handle.kernelDriverActive(self._iface):
                    self._handle.detachKernelDriver(self._iface)
            except Exception: pass
        self._handle.claimInterface(self._iface)

        # Find highest OUT endpoint — same logic as TQuarchUSB_IF.
        # libusb wrapper uses same address for read & write; no 0x80 OR needed.
        ep = 0
        for cfg in self._dev.iterConfiguations():
            if cfg.getConfigurationValue() != 1: continue
            for intf in cfg.iterInterfaces():
                for setting in intf.iterSettings():
                    if setting.getNumber() != 0: continue
                    for endpoint in setting.iterEndpoints():
                        addr = endpoint.getAddress()
                        if ep < addr < 0x80:
                            ep = addr
        if ep: self._ep = ep
        self._ep_sz = self._dev.getMaxPacketSize(self._ep)
        log.debug("USB  EP=0x%02x  EP_SZ=%d", self._ep, self._ep_sz)

        if self._ep_sz <= 64:           # PIC32 lock sequence
            self._write(self._UNLOCK); self._read(self._ep_sz, 500)
            self._write(self._LOCK);   self._read(self._ep_sz, 500)
        return True

    def close(self):
        if not self._handle: return
        if self._ep_sz <= 64:
            try: self._write(self._UNLOCK); self._read(self._ep_sz, 300)
            except Exception: pass
        try: self._handle.releaseInterface(self._iface)
        except Exception: pass
        self._handle = None

    def send_command(self, cmd: str, wait: bool = True) -> str:
        """
        Send cmd padded to 64 bytes.  Read until standalone '>' prompt.

        Fix 3: 15 ms pacing.
        Fix 4: cursor = chunk that starts with >\r\n after null-stripping.
               Avoids false break on XML containing > inside tags.
               Mirrors Java FetchCmdReplyTOut.
        """
        gap = CMD_PACE_S - (timer() - self._last_cmd)
        if gap > 0: time.sleep(gap)

        self._write(cmd.ljust(64, "\0").encode("utf-8"))
        self._last_cmd = timer()

        if not wait:
            return ""

        response = ""
        while True:
            chunk = self._read(self._ep_sz, USB_TIMEOUT_MS)
            if not chunk:
                log.debug("send_command: timeout waiting for cursor")
                break
            # Mirror Java: strip nulls, strip whitespace, add \r\n, check prefix
            processed = chunk.decode("utf-8", errors="replace").strip("\0").strip() + "\r\n"
            if processed.startswith(">\r\n"):
                break                   # standalone prompt — done
            response += processed

        return response.strip("\0> \r\n")

    def flush(self, timeout_ms: int = 100) -> int:
        n = 0
        while True:
            c = self._read(4096, timeout_ms)
            if not c: break
            n += len(c)
        if n: log.debug("flush: discarded %d bytes", n)
        return n

    def read_raw(self, n: int = 4096, timeout_ms: int = 1000) -> bytes:
        return self._read(n, timeout_ms)

    def _write(self, data: bytes):
        self._handle.bulkWrite(endpoint=self._ep, data=data, timeout=USB_TIMEOUT_MS)

    def _read(self, n: int, timeout: int = USB_TIMEOUT_MS) -> bytes:
        try:   return bytes(self._handle.bulkRead(self._ep, n, timeout))
        except Exception as exc:
            log.debug("bulk_read: %s", exc); return b""


# ── stream session ───────────────────────────────────────────────────────────
class PAMStreamSession:

    def __init__(self, h: QuarchUSBHandle):
        self._h = h
        self.meta: Optional[StreamMeta] = None

    def configure(self):
        log.info("Configuring device...")
        r = self._h.send_command("conf stream enable on")
        log.info("  conf stream enable on → %r", r)

    def discover_channels(self) -> StreamMeta:
        """
        Query hardware for channel layout via 'config:supports?'.
        Falls back to generic names if XML is absent or unparseable.
        """
        log.info("Querying channel layout (config:supports?)...")
        xml = self._h.send_command("config:supports?")
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
        """
        Send 'rec stream', yield decoded stripe dicts, then stop.
        Uses 'rec stop' to end (matches QbeInterfaceWrapper action command pair).
        """
        if self.meta is None:
            self.discover_channels()

        chan_by_pos = {ch.data_position: ch for ch in self.meta.channels}

        # Fix 1: correct streaming command for PAM
        log.info("Starting stream with 'rec stream'...")
        r = self._h.send_command("rec stream")
        log.info("  rec stream → %r", r)
        if "fail" in r.lower():
            raise RuntimeError(
                f"rec stream failed: {r!r}\n"
                "Possible causes: DUT power off (try 'run:power up'), "
                "or device already streaming.")

        collected = 0
        deadline  = time.monotonic() + timeout_s
        buf       = bytearray()
        ch_final  = bool(self.meta.channels)

        try:
            while collected < n and time.monotonic() < deadline:
                chunk = self._h.read_raw(4096, timeout_ms=1000)
                if chunk:
                    buf.extend(chunk)

                while len(buf) >= RAW_STRIPE_HEADER_LEN:
                    hdr = RawStripeDataHeader.from_bytes(buf)

                    if hdr.header_length != RAW_STRIPE_HEADER_LEN:
                        log.warning("Bad header_length=%d — resyncing", hdr.header_length)
                        buf = buf[1:]; continue

                    stripe_sz  = hdr.elements_per_stripe * 2
                    packet_sz  = RAW_STRIPE_HEADER_LEN + hdr.number_of_stripes * stripe_sz
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
            log.info("Stopping (%d stripes collected)...", collected)
            self._h.flush(50)
            r = self._h.send_command("rec stop")
            log.info("  rec stop → %r", r)
            self._h.flush(100)

    def _decode(self, raw: bytes, rec_no: int, chan_by_pos: dict) -> dict:
        if len(raw) < 2: return {}
        status  = struct.unpack_from("<H", raw, 0)[0]
        trigger = bool(status & 0x4000)
        valid   = bool(status & 0x8000)
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
                "group": ch.group if ch else "unknown",
            }
        return {
            "record_number": rec_no,
            "time_us":       rec_no * self.meta.device_period_us,
            "trigger":       trigger,
            "valid":         valid,
            "channels":      channels,
        }


# ── device finder ─────────────────────────────────────────────────────────────
def find_pam():
    try:
        from quarchpy.connection_specific.connection_USB import importUSB
        usb1 = importUSB()
    except Exception as exc:
        raise RuntimeError(f"Cannot load libusb via quarchpy: {exc}") from exc
    ctx = usb1.USBContext()
    for dev in ctx.getDeviceList():
        if (hex(dev.device_descriptor.idVendor)  != hex(QUARCH_VENDOR_ID) or
            hex(dev.device_descriptor.idProduct) != hex(QUARCH_PRODUCT_ID)):
            continue
        try:
            h = dev.open(); sn = h.getASCIIStringDescriptor(3); h.close()
        except Exception: sn = "unknown"
        log.info("Found Quarch USB: %s", sn)
        return ctx, dev
    raise RuntimeError("No Quarch USB device found.")


# ── main ──────────────────────────────────────────────────────────────────────
def main():
    print("=" * 65)
    print("  Quarch PAM  –  Pure Python USB Binary Stream  (no QIS)")
    print("=" * 65)

    ctx, dev = find_pam()
    h = QuarchUSBHandle(ctx, dev)
    if not h.open():
        log.error("Failed to open USB handle."); sys.exit(1)
    log.info("USB handle opened.")

    session = PAMStreamSession(h)
    try:
        idn = h.send_command("*idn?")
        log.info("*idn? → %s", idn[:120])

        pwr = h.send_command("run:power?")
        log.info("run:power? → %r", pwr)
        if pwr and ("off" in pwr.lower() or "pulled" in pwr.lower()):
            log.info("Powering up DUT rails...")
            h.send_command("run:power up"); time.sleep(1.0)

        session.configure()
        meta = session.discover_channels()

        print(f"\nCollecting 50 stripes (device period ~{meta.device_period_us} µs)...\n")
        hdr_done = False
        for stripe in session.stream_stripes(n=50, timeout_s=20):
            items = list(stripe["channels"].values())
            if not hdr_done:
                cols = [f"{v['name']}({v['units']})" for v in items]
                print(f"  {'rec#':>6}  {'time µs':>9}  T  " +
                      "  ".join(f"{c:>14}" for c in cols))
                print("  " + "-"*70)
                hdr_done = True
            vals = [str(v["value"]) for v in items]
            print(f"  {stripe['record_number']:>6}  {stripe['time_us']:>9}  "
                  f"{'T' if stripe['trigger'] else '.'}  " +
                  "  ".join(f"{v:>14}" for v in vals))
    finally:
        h.close()
        log.info("USB handle closed.")
    print("\nDone.")


if __name__ == "__main__":
    main()