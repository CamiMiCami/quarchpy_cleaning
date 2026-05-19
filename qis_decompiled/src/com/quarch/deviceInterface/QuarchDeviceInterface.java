/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.usb4java.DeviceList
 */
package src.com.quarch.deviceInterface;

import java.util.TreeMap;
import org.usb4java.DeviceList;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceInterface.QuarchDeviceInfoEventInitiater;
import src.com.quarch.deviceInterface.QuarchDeviceInfoListener;

public class QuarchDeviceInterface {
    public DeviceList deviceList;
    public TreeMap<String, QuarchDeviceInfo> currentDevices;
    public TreeMap<String, QuarchDeviceInfo> previousDevices;
    public QuarchDeviceInfoEventInitiater infoEvent = new QuarchDeviceInfoEventInitiater();

    public void addInfoListener(QuarchDeviceInfoListener listener) {
        this.infoEvent.addListener(listener);
    }

    protected void updateListeners() {
        boolean needUpdate = false;
        if (this.previousDevices == null) {
            needUpdate = true;
        } else if (!this.currentDevices.keySet().equals(this.previousDevices.keySet())) {
            needUpdate = true;
        }
        if (needUpdate) {
            this.infoEvent.fireEvent(this.currentDevices, this.previousDevices);
            this.previousDevices = this.currentDevices;
        }
    }

    protected void updateListeners(TreeMap<String, QuarchDeviceInfo> currentDevices, TreeMap<String, QuarchDeviceInfo> previousDevices) {
        this.infoEvent.fireEvent(currentDevices, previousDevices);
    }
}

