/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceType
 *  device.DeviceRef
 */
package src.com.quarch.deviceInterface;

import commsInterface.CommsInterfaceType;
import device.DeviceRef;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.deviceAbstractions.DeviceFactory;
import src.com.quarch.deviceDetected.DetectedDevice;
import src.com.quarch.deviceInterface.DeviceListChangeInitiater;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceInterface.QuarchDeviceInfoListener;
import src.com.quarch.devices.BaseDevice;
import src.com.quarch.utils.DebugUtil;

public class DeviceList
extends DeviceListChangeInitiater
implements QuarchDeviceInfoListener {
    private List<DeviceListEntry> deviceList;
    public static final String DeviceNameSeparator = "::";
    private final List<RootDevice> rootDeviceList = new ArrayList<RootDevice>();

    private void addDeviceConnection(DeviceListEntry dle) {
        boolean found = false;
        for (RootDevice rd : this.rootDeviceList) {
            if (!rd.hasSerialNumber(dle.serialNo)) continue;
            rd.addDeviceConnection(dle);
            found = true;
        }
        if (!found) {
            this.rootDeviceList.add(new RootDevice(dle));
        }
    }

    private void removeDeviceConnection(DeviceListEntry dle) {
        RootDevice foundRootDevice = null;
        for (RootDevice rd : this.rootDeviceList) {
            if (!rd.hasSerialNumber(dle.serialNo)) continue;
            foundRootDevice = rd;
        }
        if (foundRootDevice != null && foundRootDevice.removeDeviceConnection(dle) == 0) {
            this.rootDeviceList.remove(foundRootDevice);
        }
    }

    public DeviceList() {
        this.setDeviceList(new ArrayList<DeviceListEntry>());
    }

    @Override
    public synchronized void QuarchDeviceInfoEvent(TreeMap<String, QuarchDeviceInfo> currentDevices, TreeMap<String, QuarchDeviceInfo> previousDevices) {
        for (String sNo : currentDevices.keySet()) {
            if (previousDevices != null && previousDevices.keySet().contains(sNo) || this.findDevice(sNo, currentDevices.get(sNo).getIntefaceName()) != -1) continue;
            this.addDevice(sNo, currentDevices.get(sNo));
        }
        if (previousDevices != null) {
            for (String sNo : previousDevices.keySet()) {
                if (currentDevices.keySet().contains(sNo)) continue;
                this.removeDevice(sNo, previousDevices.get(sNo));
            }
        }
        this.fireEvent(currentDevices);
    }

    public synchronized int findDevice(String serialNo, String intface) {
        int i = 0;
        for (DeviceListEntry dle : this.getDeviceList()) {
            if (dle.serialNo.equalsIgnoreCase(serialNo) && dle.deviceInfo.getIntefaceName().equalsIgnoreCase(intface)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public int findDevice(String deviceSpecifier) {
        String[] parts = deviceSpecifier.split(DeviceNameSeparator);
        if (parts.length == 2) {
            return this.findDevice(parts[1].toUpperCase(), parts[0].toLowerCase());
        }
        return -1;
    }

    public DeviceListEntry getDeviceListEntryFor(int idx) {
        return this.getDeviceList().get(idx);
    }

    private synchronized void addDevice(String serialNo, QuarchDeviceInfo deviceInfo) {
        DeviceListEntry deviceListEntry = new DeviceListEntry(serialNo, deviceInfo);
        this.addDevice(deviceListEntry);
    }

    private synchronized void addDevice(DeviceListEntry dle) {
        this.addDeviceConnection(dle);
        this.getDeviceList().add(dle);
    }

    private void removeDLE(DeviceListEntry dle) {
        if (dle == null) {
            return;
        }
        this.getDeviceList().remove(dle);
        this.removeDeviceConnection(dle);
    }

    public synchronized void removeDevice(String serialNo, QuarchDeviceInfo deviceInfo) {
        DeviceListEntry toRemove = null;
        for (DeviceListEntry dle : this.getDeviceList()) {
            if (!dle.serialNo.equals(serialNo) || !dle.deviceInfo.getIntefaceName().equals(deviceInfo.getIntefaceName())) continue;
            dle.deviceInfo.freeDevice();
            toRemove = dle;
            break;
        }
        this.removeDLE(toRemove);
    }

    public static String getFullDeviceName(String key, QuarchDeviceInfo qdi) {
        String s = null;
        if (qdi != null) {
            s = qdi.getIntefaceName() + DeviceNameSeparator + key;
        }
        return s;
    }

    public synchronized void getDeviceSerialNumbers(List<String> sl) {
        for (DeviceListEntry dle : this.getDeviceList()) {
            sl.add(dle.deviceInfo.getIntefaceName() + DeviceNameSeparator + dle.serialNo);
        }
    }

    public synchronized void getNumberedtDeviceSerialNumbers(List<String> sl) {
        int i = 1;
        for (DeviceListEntry dle : this.getDeviceList()) {
            sl.add(Integer.toString(i++) + ") " + dle.deviceInfo.getIntefaceName() + DeviceNameSeparator + dle.serialNo);
        }
    }

    public synchronized void getNumberedtDeviceDetails(List<String> sl) {
        int i = 1;
        for (DeviceListEntry dle : this.getDeviceList()) {
            String deviceSerialNo = dle.serialNo;
            if (deviceSerialNo.charAt(0) == '\u00ff') {
                deviceSerialNo = "NO_SERIAL";
            }
            String str = Integer.toString(i++) + ") " + dle.deviceInfo.getIntefaceName() + DeviceNameSeparator + deviceSerialNo;
            DeviceRef getqDevice = dle.deviceInfo.getqDevice();
            if (dle.deviceInfo.getIntefaceName().equalsIgnoreCase(DeviceSpecifier.VIRTUALinterfaceNameStr)) {
                str = str + " Stream:";
                str = str + this.getStreamString(dle.deviceInfo);
            } else if (dle.deviceInfo.getIntefaceName().equalsIgnoreCase(DeviceSpecifier.USBinterfaceNameStr)) {
                str = str + " Port:" + getqDevice.getPort();
                str = str + " Stream:";
                str = str + this.getStreamString(dle.deviceInfo);
            } else {
                CommsInterfaceType preferred;
                CommsInterfaceType commsInterfaceType = preferred = getqDevice == null ? null : getqDevice.getCurrentCommsInterfaceType();
                if (getqDevice != null) {
                    if (dle.deviceInfo.getIntefaceName() != getqDevice.getCurrentCommsInterfaceType().getText()) {
                        getqDevice.setCurrentCommsInterface(CommsInterfaceType.fromString((String)dle.deviceInfo.getIntefaceName()));
                    }
                    str = str + " IP:" + getqDevice.getUniqueID().split(":")[0];
                    str = str + " Port:" + getqDevice.getPort();
                    str = str + " NBName:" + getqDevice.getHostName();
                }
                str = str + " Stream:";
                str = str + this.getStreamString(dle.deviceInfo);
                if (getqDevice != null) {
                    getqDevice.setCurrentCommsInterface(preferred);
                }
            }
            BaseDevice deviceAbstract = dle.deviceInfo.getDeviceAbstract();
            str = deviceAbstract != null && deviceAbstract.isVirtualSourceDevice() ? str + " VirtualSource:Yes" : str + " VirtualSource:No";
            if (deviceAbstract != null && deviceAbstract.getVirtualOwner() != null) {
                str = str + " Owner:" + deviceAbstract.getVirtualOwner().dle.deviceInfo.serialNumberStr;
            }
            str = str + " Name:" + getqDevice.getIdnName();
            if (DebugUtil.isEnableDevDebug()) {
                str = str + " {Ref: " + deviceAbstract + "}";
                str = deviceAbstract != null ? (getqDevice.getCurrentCommsInterfaceType() == CommsInterfaceType.VIRTUAL ? str + " Identified? VIRTUAL" : str + " Identified? " + !deviceAbstract.isNeedFurtherIndetification()) : str + " No Abstract";
            }
            sl.add(str);
        }
    }

    private String getStreamString(QuarchDeviceInfo deviceInfo) {
        String str = "";
        str = deviceInfo.getDeviceAbstract() == null ? this.specialNoStreamString() : (deviceInfo.checkStreamSupport() ? "Yes" : "No");
        return str;
    }

    public String specialNoStreamString() {
        if (DebugUtil.isEnableDevDebug()) {
            return "NoAbstract";
        }
        return "No";
    }

    public synchronized String geNumberedtDeviceSerialNumber(int num) {
        String s = "";
        int i = 1;
        for (DeviceListEntry dle : this.getDeviceList()) {
            if (i++ != num) continue;
            s = dle.deviceInfo.getIntefaceName() + DeviceNameSeparator + dle.serialNo;
            break;
        }
        return s;
    }

    public List<String> getDeviceSerialNumbers() {
        ArrayList<String> sl = new ArrayList<String>();
        this.getDeviceSerialNumbers(sl);
        return sl;
    }

    public synchronized List<String> getStreamDeviceSerialNumbers() {
        ArrayList<String> sl = new ArrayList<String>();
        for (DeviceListEntry dle : this.getDeviceList()) {
            if (!dle.deviceInfo.isDeviceHasStream()) continue;
            sl.add(dle.deviceInfo.getIntefaceName() + DeviceNameSeparator + dle.serialNo);
        }
        return sl;
    }

    public synchronized DeviceListEntry findMatchingDevice(DeviceSpecifier devSpec) {
        DeviceListEntry retVal = null;
        if (devSpec.specifiedByManualIP) {
            for (DeviceListEntry dle : this.getDeviceList()) {
                if (dle.deviceInfo.ipAddress == null || !dle.deviceInfo.ipAddress.toLowerCase().equals(devSpec.deviceAddress) || !dle.deviceInfo.getIntefaceName().toLowerCase().equals(devSpec.interfaceName)) continue;
                retVal = dle;
                break;
            }
        } else {
            for (DeviceListEntry dle : this.getDeviceList()) {
                if (!dle.serialNo.equalsIgnoreCase(devSpec.getDeviceName()) || !dle.deviceInfo.getIntefaceName().equalsIgnoreCase(devSpec.interfaceName)) continue;
                retVal = dle;
                break;
            }
        }
        return retVal;
    }

    public synchronized void remove(DeviceListEntry dle) {
        this.removeDLE(dle);
    }

    public List<DeviceListEntry> getDeviceList() {
        return this.deviceList;
    }

    public void setDeviceList(List<DeviceListEntry> deviceList) {
        this.deviceList = deviceList;
    }

    public void addDLE(DeviceListEntry deviceListEntry) {
        this.addDevice(deviceListEntry);
    }

    public void mergeReplaceIdentifiedDevice(DeviceListEntry dle, BaseDevice device) {
        String sNo = dle.serialNo;
        for (DeviceListEntry _dle : this.deviceList) {
            if (!_dle.serialNo.equalsIgnoreCase(sNo)) continue;
            _dle.deviceInfo.setDeviceAbstract(device);
        }
    }

    class RootDevice {
        final List<DeviceListEntry> deviceConnectionsList = new ArrayList<DeviceListEntry>();

        protected RootDevice(DeviceListEntry dle) {
            this.addDeviceConnection(dle);
        }

        public boolean hasSerialNumber(String serialNo) {
            return serialNo.equals(this.deviceConnectionsList.get((int)0).serialNo);
        }

        public void addDeviceConnection(DeviceListEntry dle) {
            if (this.deviceConnectionsList.size() == 0) {
                this.deviceConnectionsList.add(dle);
                String intefaceName = dle.deviceInfo.getIntefaceName();
                if (!DeviceFactory.isExcludedInterface(dle)) {
                    dle.deviceInfo.getDeviceAbstract().clearNeedFurtherIndetification();
                }
            } else {
                for (DeviceListEntry cle : this.deviceConnectionsList) {
                    if (cle.deviceInfo.getDeviceAbstract() instanceof DetectedDevice) {
                        cle.deviceInfo.setDeviceAbstract(dle.deviceInfo.getDeviceAbstract());
                        continue;
                    }
                    dle.deviceInfo.setDeviceAbstract(cle.deviceInfo.getDeviceAbstract());
                }
            }
        }

        public int removeDeviceConnection(DeviceListEntry dle) {
            this.deviceConnectionsList.remove(dle);
            return this.deviceConnectionsList.size();
        }
    }
}

