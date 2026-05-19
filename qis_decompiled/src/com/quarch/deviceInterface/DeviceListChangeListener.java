/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceInterface;

import java.util.TreeMap;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;

public interface DeviceListChangeListener {
    public void deviceListChangeEvent(TreeMap<String, QuarchDeviceInfo> var1);
}

