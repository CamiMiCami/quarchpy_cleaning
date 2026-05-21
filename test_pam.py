import quarchpy

def connect_to_pam():
    print("Connecting to device...")
    found_devices = quarchpy.scanDevices("all")

    if not found_devices:
        return False

    target_id = list(found_devices)[0]
    print(f"Found device: {target_id}")

    try:
        print(f"Connecting to {target_id}...")
        pam = quarchpy.quarchDevice(ConString=target_id, ConType="PY")
        #pam = quarchpy.quarchPPM(pam, skipDefaultSyntheticChannels=True)

        print("--- Communication Established ---")
        print("Send command: hello?")
        print(f"Identity: {pam.sendCommand('hello?')}")
        print("Send command: *IDN?")
        print(f"Identity: {pam.sendCommand('*IDN?')}")
        print("Send command: fixture:channels?")
        print(f"Channels: {pam.sendCommand('fixture:channels?')}")
        print("Send command: Sampling rate?")
        print(f"Identity: {pam.sendCommand('CONF:RATE?')}")
        print("Send command: Connection Type?")
        print(f"Connection Type: {pam.ConCommsType}")
        print(f"Connection Name: {pam.connectionName}")

    except Exception as e:
        print(f"Failed to connect: {e}")

connect_to_pam()