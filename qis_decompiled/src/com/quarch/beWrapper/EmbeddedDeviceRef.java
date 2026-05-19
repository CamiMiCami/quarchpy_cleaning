/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beWrapper;

import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.beWrapper.QbeInterfaceWrapper;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class EmbeddedDeviceRef {
    public DeviceSpecifier devSpec;
    public DeviceListEntry dle;
    public String cmd;

    public EmbeddedDeviceRef(String str) {
        this.devSpec = new DeviceSpecifier(str);
        if (this.devSpec.interfaceName != "") {
            this.dle = QbeInterfaceWrapper.deviceList.findMatchingDevice(this.devSpec);
            if (this.dle != null || this.devSpec.specifiedByManualIP) {
                // empty if block
            }
            if (this.dle != null) {
                this.dle.applyOverrides(this.devSpec);
                int cmdStart = str.indexOf(" ") + 1;
                if (cmdStart >= str.length()) {
                    this.dle = null;
                    return;
                }
                this.cmd = str.substring(cmdStart, str.length());
            }
        }
    }
}

