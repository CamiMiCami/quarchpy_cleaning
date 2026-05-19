/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceInterface;

import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.deviceAbstractions.DeviceFactory;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;

public class DeviceListEntry {
    public final String serialNo;
    public final QuarchDeviceInfo deviceInfo;
    public String option;

    public DeviceListEntry(String serialNo, QuarchDeviceInfo deviceInfo) {
        this.serialNo = serialNo.trim();
        this.deviceInfo = deviceInfo;
        this.option = "";
        boolean forceIenfity = !DeviceFactory.isExcludedInterface(this);
        deviceInfo.setDeviceAbstract(DeviceFactory.getInstance().createDevice(this, forceIenfity));
    }

    public void applyOverrides(DeviceSpecifier devSpec) {
        if (devSpec.portNumber != -1) {
            this.deviceInfo.overridePort = devSpec.portNumber;
        }
        if (devSpec.timeout != -1) {
            this.deviceInfo.overrideTimeout = devSpec.timeout;
        }
    }

    public String getDeviceIdStr() {
        if (this.deviceInfo != null) {
            return this.deviceInfo.getIntefaceName() + "::" + this.serialNo;
        }
        return "Invalid Device ID";
    }
}

