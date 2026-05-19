/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  Quarch.QChangeListener
 *  device.DeviceChangeEventData
 *  device.DeviceChangeEventTypes
 */
package commsDeviceInterface;

import Quarch.QChangeListener;
import commsDeviceInterface.CommsDeviceInfo;
import device.DeviceChangeEventData;
import device.DeviceChangeEventTypes;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class commsDeviceListListener
implements QChangeListener<DeviceChangeEventData> {
    static DeviceList deviceList;
    AtomicInteger threadCount = new AtomicInteger(0);

    public commsDeviceListListener(DeviceList device_list) {
        deviceList = device_list;
    }

    public void changedSimple(DeviceChangeEventData device) {
        DeviceChangeEventTypes deviceChangeEventType = device.getDeviceChangeEventType();
        if (deviceChangeEventType.equals((Object)DeviceChangeEventTypes.DeviceWasAdded)) {
            this.add_device(device);
        }
        if (deviceChangeEventType.equals((Object)DeviceChangeEventTypes.DeviceWasRemoved)) {
            this.remove_device(device);
        }
        if (deviceChangeEventType.equals((Object)DeviceChangeEventTypes.InterfaceWasAdded)) {
            this.add_interface(device);
        }
        if (deviceChangeEventType.equals((Object)DeviceChangeEventTypes.InterfaceWasRemoved)) {
            this.remove_device_interface(device);
        }
    }

    private synchronized void add_interface(DeviceChangeEventData device) {
        CommsDeviceInfo device_info = new CommsDeviceInfo(device.getDeviceRef());
        device_info.setIntefaceName(device.getCommsInterface().getCommsInterfaceType().getText());
        device_info.setAltSerialNo(device.getDeviceRef().getEnclosureNumber());
        this.addDeviceListEntry(device_info);
    }

    private synchronized void add_device(DeviceChangeEventData device) {
        this.threadedAddDevice(device);
    }

    private void sequentialAddDevice(DeviceChangeEventData device) {
        CommsDeviceInfo device_info = new CommsDeviceInfo(device.getDeviceRef());
        if (device.getDeviceRef().getIdnName() == null) {
            device.getDeviceRef().enquire();
            device_info.setAltSerialNo(device.getDeviceRef().getEnclosureNumber());
        }
        this.addDeviceListEntry(device_info);
    }

    private void threadedAddDevice(DeviceChangeEventData device) {
        new Thread(() -> {
            CommsDeviceInfo device_info = new CommsDeviceInfo(device.getDeviceRef());
            if (device.getDeviceRef().getIdnName() == null) {
                device.getDeviceRef().enquire();
                device_info.setAltSerialNo(device.getDeviceRef().getEnclosureNumber());
            }
            this.addDeviceListEntry(device_info);
        }, "add_device_" + this.threadCount.getAndIncrement()).start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addDeviceListEntry(CommsDeviceInfo device_info) {
        DeviceListEntry dle = new DeviceListEntry(device_info.getSerialNumberStr(), device_info);
        DeviceList deviceList = commsDeviceListListener.deviceList;
        synchronized (deviceList) {
            commsDeviceListListener.deviceList.addDLE(dle);
        }
    }

    private void remove_device(DeviceChangeEventData device) {
        ArrayList<DeviceListEntry> devList = new ArrayList<DeviceListEntry>();
        devList.addAll(deviceList.getDeviceList());
        for (DeviceListEntry dle : devList) {
            if (dle.serialNo.equals(device.getCommsInterface().getCommsSerialNo())) {
                dle.deviceInfo.freeDevice();
                deviceList.getDeviceList().remove(dle);
                continue;
            }
            if (!dle.deviceInfo.idnEnclosureNo.equals(device.getCommsInterface().getCommsSerialNo())) continue;
            dle.deviceInfo.freeDevice();
            deviceList.getDeviceList().remove(dle);
        }
    }

    private void remove_device_interface(DeviceChangeEventData device) {
        ArrayList<DeviceListEntry> devList = new ArrayList<DeviceListEntry>();
        devList.addAll(deviceList.getDeviceList());
        for (DeviceListEntry dle : devList) {
            if (!dle.serialNo.equals(device.getCommsInterface().getCommsSerialNo()) && !dle.deviceInfo.idnEnclosureNo.equals(device.getCommsInterface().getCommsSerialNo()) && !dle.deviceInfo.serialNumberStr.equals(device.getCommsInterface().getCommsSerialNo())) continue;
            CommsDeviceInfo device_info = new CommsDeviceInfo(device.getDeviceRef());
            device_info.setIntefaceName(device.getCommsInterface().getInterfaceAsString());
            deviceList.removeDevice(dle.serialNo, device_info);
            break;
        }
    }
}

