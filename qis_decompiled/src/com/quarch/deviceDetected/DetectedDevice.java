/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceDetected;

import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.BaseDevice;

public class DetectedDevice
extends BaseDevice {
    public DetectedDevice(DeviceListEntry dle) {
        this.myDle = dle;
        this.setNeedFurtherIndetification(true);
    }

    public static boolean isValid(DeviceListEntry dle) {
        return true;
    }
}

