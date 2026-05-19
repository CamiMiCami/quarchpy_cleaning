/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import src.com.quarch.deviceInterface.DeviceListChangeListener;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;

public class DeviceListChangeInitiater {
    private List<DeviceListChangeListener> listeners = new ArrayList<DeviceListChangeListener>();

    public void addListener(DeviceListChangeListener toAdd) {
        this.listeners.add(toAdd);
    }

    public void fireEvent(TreeMap<String, QuarchDeviceInfo> currentDevices) {
        for (DeviceListChangeListener hl : this.listeners) {
            hl.deviceListChangeEvent(currentDevices);
        }
    }
}

