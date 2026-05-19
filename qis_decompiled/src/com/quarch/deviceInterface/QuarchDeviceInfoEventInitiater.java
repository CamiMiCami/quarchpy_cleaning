/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceInterface.QuarchDeviceInfoListener;

public class QuarchDeviceInfoEventInitiater {
    public List<QuarchDeviceInfoListener> listeners = new ArrayList<QuarchDeviceInfoListener>();

    public void addListener(QuarchDeviceInfoListener toAdd) {
        this.listeners.add(toAdd);
    }

    public synchronized void fireEvent(TreeMap<String, QuarchDeviceInfo> currentDevices, TreeMap<String, QuarchDeviceInfo> prevDevices) {
        for (QuarchDeviceInfoListener hl : this.listeners) {
            hl.QuarchDeviceInfoEvent(currentDevices, prevDevices);
        }
    }
}

