import quarchpy
import time


def connect_to_pam():
    print("Connecting to device...")
    found_devices = quarchpy.scanDevices("all")

    if not found_devices:
        print("No devices found.")
        return False

    target_id = list(found_devices)[0]

    try:
        print(f"Connecting to {target_id}...")
        pam = quarchpy.quarchDevice(ConString=target_id, ConType="PY")
        pam.sendCommand('*RST')
        time.sleep(5)
        pam = quarchpy.quarchDevice(ConString=target_id, ConType="PY")

        print("--- Communication Established ---")
        print(f"Identity: {pam.sendCommand('*IDN?').strip()}")
        print(f"Connection Type: {pam.ConCommsType}")
        # print(f"XML descriptor: {pam.sendCommand("FIXture:CHANnels:XML?")}")

        # =========================================================
        # --- BULK READ STREAM INJECTION ---
        # =========================================================
        print("\n--- Starting Bulk Read Stream Test ---")

        # 1. Set trigger to MANUAL (Bypasses waiting; starts instantly on RECord RUN)
        pam.sendCommand("RECord:TRIGger:MODE MANual")
        pam.sendCommand("record:averaging 320k")
        pam.sendCommand("RECord:TRIGger:ARM")

        # 2. Enable stream mode formatting
        pam.sendCommand("conf stream enable on")

        # 3. Extract the low-level USB interface and endpoints
        hw_if = pam.connectionObj.connection.Connection
        handle = hw_if.deviceHandle
        # ep = hw_if.QCmdEP
        # print(f"Show ep: {ep}")

        # 4. Arm the hardware
        print("Sending 'RECord RUN'...")
        # hw_if.RunCommand("RECord RUN", expectedResponse=False)
        hw_if.RunCommand("rec stream", expectedResponse=False)

        print("Reading stream (Running for 20 seconds)...\n")
        start_time = time.time()
        reads = 0

        """
        For now it needs complete reset with the command *RST to get the reading... Needs to find other solutions. And it only shows one bulk when it's working.
        Suspect overflow and mismatched stream and read rate.
        """
        try:
            while time.time() - start_time < 10.0:
                try:
                    print("in the 10s time loop") # for debug
                    chunk = handle.bulkRead(1, 4096, 1000)
                    # chunk = handle.bulkRead(ep, 4096, 1000)
                    if chunk:
                        print("chunk exists loop") # for debug
                        reads += 1
                        raw_bytes = bytes(chunk)
                        print(raw_bytes)

                        # Print the first 16 bytes of the chunk in Hex
                        hex_preview = " ".join(f"{b:02X}" for b in raw_bytes[:16])
                        print(f"Read {reads:03d} | Size: {len(raw_bytes):4d} bytes | Preview: {hex_preview}")

                except Exception as exc:
                    # libusb throws an exception on an empty pipe. Catch and continue.
                    if "TIMEOUT" in str(exc).upper():
                        continue
                    else:
                        print(f"Unexpected USB Error: {exc}")
                        break

        except KeyboardInterrupt:
            print("\nStream interrupted by user.")

        # =========================================================
        # --- CLEANUP & RESET ---
        # =========================================================
        print("\n--- Stop Streaming and Cleaning ---")

        # Stop the recording at the hardware level
        try:
            hw_if.RunCommand("RECord STOP", expectedResponse=False)
        except Exception:
            pass

        print("Flushing USB buffer...")
        while True:
            try:
                c = handle.bulkRead(ep, 4096, 250)
                if not c:
                    break
            except Exception:
                # A timeout here means the buffer is successfully dry
                break

        print("Done Clean!")

    except Exception as e:
        print(f"Failed to connect or execute: {e}")
    finally:
        if 'pam' in locals():
            pam.closeConnection()
            print("Connection closed.")


if __name__ == "__main__":
    connect_to_pam()