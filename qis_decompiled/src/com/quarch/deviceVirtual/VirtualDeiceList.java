/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceVirtual;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceVirtual.VirtualDevice;
import src.com.quarch.deviceVirtual.VirtualDeviceLink;

public class VirtualDeiceList {
    private final List<VirtualDevice> virtualDevices = new ArrayList<VirtualDevice>();

    public Object createDevice(DeviceList deviceList, List<String> paramList, String origCmdText) {
        String name = paramList.get(0);
        String description = paramList.get(1);
        ArrayList<VirtualDeviceLink> sourceDevices = new ArrayList<VirtualDeviceLink>();
        for (VirtualDevice vd : this.getVirtualDevices()) {
            if (!vd.qdi.idnName.equalsIgnoreCase(name)) continue;
            return "FAIL: Virtual device already exists [" + name + "]";
        }
        int i = 0;
        try {
            VirtualDevice vd;
            i = 2;
            while (i < paramList.size() && paramList.get(i) != null) {
                String deviceCmdc = paramList.get(i++);
                String devSpec = paramList.get(i++);
                String preFix = paramList.get(i++);
                int idx = deviceList.findDevice(devSpec);
                if (idx < 0) {
                    return "FAIL: Device not found [" + devSpec + "]";
                }
                DeviceListEntry dle = deviceList.getDeviceListEntryFor(idx);
                if (dle.deviceInfo.getDeviceAbstract().isVirtualDeviceMember()) {
                    return "FAIL: Device is already a member of a virtual device [" + devSpec + "]";
                }
                if (!dle.deviceInfo.getDeviceAbstract().isVirtualSourceDevice()) {
                    return "FAIL: Device cannot be used in a virtual device [" + devSpec + "]";
                }
                sourceDevices.add(new VirtualDeviceLink(dle, preFix));
            }
            vd = new VirtualDevice(name, description, sourceDevices, origCmdText);
            this.getVirtualDevices().add(vd);
            return vd;
        }
        catch (Exception e) {
            if (i == 1) {
                return "FAIL: Error creating " + name;
            }
            return "FAIL: Format error decoding " + paramList.get(i);
        }
    }

    public List<VirtualDevice> getVirtualDevices() {
        return this.virtualDevices;
    }

    public String deleteDevice(DeviceList deviceList, VirtualDevice vDev) {
        if (vDev.qdi.isStreaming()) {
            return "FAIL: Cannot delete while the device is streaming";
        }
        deviceList.remove(vDev.dle);
        this.virtualDevices.remove(vDev);
        vDev.clearSharedVirtualData();
        return "";
    }

    public String deleteAll(DeviceList deviceList) {
        String streamingdevices = "";
        for (VirtualDevice vDev : this.virtualDevices) {
            String retStr = this.deleteDevice(deviceList, vDev);
            if (retStr.isEmpty()) continue;
            streamingdevices = streamingdevices + "<" + vDev.qdi.idnName + ">";
        }
        if (!streamingdevices.isEmpty()) {
            return "FAIL: These devices were streaming and could not be deleted " + streamingdevices;
        }
        return "";
    }
}

