/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  Quarch.QChangeListener
 *  QuarchLogging.QuarchLogger
 *  commsInterface.InterfaceChangeEventData
 *  commsInterface.InterfaceChangeEventTypes
 *  device.DeviceRef
 */
package commsDeviceInterface;

import Quarch.QChangeListener;
import QuarchLogging.QuarchLogger;
import commsDeviceInterface.CommsDeviceInfo;
import commsInterface.InterfaceChangeEventData;
import commsInterface.InterfaceChangeEventTypes;
import device.DeviceRef;
import java.util.logging.Level;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class CommsScanListListener
implements QChangeListener<InterfaceChangeEventData> {
    static DeviceList deviceList;

    public CommsScanListListener(DeviceList device_list) {
        deviceList = device_list;
    }

    public void changedSimple(InterfaceChangeEventData dev_interface) {
        if (dev_interface.getInterfaceChangeEventType().equals((Object)InterfaceChangeEventTypes.InterfaceWasAdded)) {
            this.add_device(dev_interface);
        }
    }

    private void add_device(InterfaceChangeEventData dev_interface) {
        DeviceRef referenceDev = null;
        for (DeviceListEntry item : deviceList.getDeviceList()) {
            if (!item.serialNo.equalsIgnoreCase(dev_interface.getDeviceInterface().getCommsSerialNo())) continue;
            referenceDev = item.deviceInfo.getqDevice();
            break;
        }
        if (referenceDev != null) {
            if (!referenceDev.setCurrentCommsInterface(dev_interface.getDeviceInterface().getCommsInterfaceType())) {
                QuarchLogger.logMessage((Level)Level.WARNING, (String)"Fail: Cannot add deviceListEntry as deviceRef does not have  commsInterfaceType");
                return;
            }
            CommsDeviceInfo device_info = new CommsDeviceInfo(referenceDev);
            device_info.getExtendedInfo();
            deviceList.addDLE(new DeviceListEntry(device_info.getSerialNumberStr(), device_info));
        } else {
            QuarchLogger.logMessage((Level)Level.WARNING, (String)"Couldn't find a deviceRef with the same serialNo as commsInterface");
        }
    }
}

