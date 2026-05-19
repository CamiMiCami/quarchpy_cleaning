/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceType
 *  device.DeviceRef
 */
package src.com.quarch.beWrapper;

import commsInterface.CommsInterfaceType;
import device.DeviceRef;
import java.io.IOException;
import java.util.List;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beEthernet.EthernetMChip30303Locate;
import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.beWrapper.EmbeddedDeviceRef;
import src.com.quarch.deviceAbstractions.DecodedSerialNumber;
import src.com.quarch.deviceAbstractions.DeviceFactory;
import src.com.quarch.deviceAbstractions.DeviceTypes;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.devices.BaseDevice;

public final class QbeInterfaceWrapper {
    private static volatile QbeInterfaceWrapper instance;
    public static DeviceList deviceList;
    private boolean debugSyncOnDevice = true;

    private QbeInterfaceWrapper() {
    }

    public DeviceList getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(DeviceList deviceList) {
        QbeInterfaceWrapper.deviceList = deviceList;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static QbeInterfaceWrapper getInstance() {
        if (instance != null) return instance;
        Class<QbeInterfaceWrapper> clazz = QbeInterfaceWrapper.class;
        synchronized (QbeInterfaceWrapper.class) {
            if (instance != null) return instance;
            instance = new QbeInterfaceWrapper();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    public void closeAllDevices() {
        List<String> devices = deviceList.getDeviceSerialNumbers();
        for (String devStr : devices) {
            DeviceListEntry dle;
            DeviceSpecifier devSpec = this.getDevice(devStr);
            if (devSpec == null || (dle = deviceList.findMatchingDevice(devSpec)) == null) continue;
            dle.deviceInfo.forceClose();
        }
    }

    private DeviceSpecifier getDevice(String s) {
        int numStartPos = s.indexOf(" ") + 1;
        DeviceSpecifier devSpec = null;
        if (numStartPos > 0 && numStartPos < s.length()) {
            try {
                int num = Integer.valueOf(s.substring(numStartPos));
                devSpec = new DeviceSpecifier(deviceList.geNumberedtDeviceSerialNumber(num));
            }
            catch (NumberFormatException e) {
                devSpec = null;
            }
        }
        if (devSpec == null) {
            devSpec = Character.isAlphabetic(s.charAt(numStartPos)) ? new DeviceSpecifier(s) : new DeviceSpecifier("");
        }
        return devSpec;
    }

    public boolean isDefaultCommand(CmdStruct cmd) {
        boolean retVal = false;
        String command = cmd.command.trim().toLowerCase();
        cmd.action = 0;
        if (command.startsWith("$default ") || command.startsWith("$def ")) {
            DeviceSpecifier devSpec = this.getDevice(command);
            if (devSpec != null && devSpec.interfaceName != "") {
                DeviceListEntry dle = deviceList.findMatchingDevice(devSpec);
                if (dle != null) {
                    dle.applyOverrides(devSpec);
                    cmd.setDefaultDevice(dle);
                    cmd.getDefaultDevice().option = devSpec.connectionOption;
                    cmd.response.add("Default Device " + cmd.getDefaultDevice().getDeviceIdStr());
                    cmd.response.add(">");
                } else {
                    cmd.response.add("Device Specifier Not Recognised");
                    cmd.response.add(">");
                }
                retVal = true;
            } else {
                cmd.response.add("Device Specifier Not Recognised");
                cmd.response.add(">");
                retVal = true;
            }
        } else if (command.startsWith("$default?") || command.startsWith("$def?")) {
            if (cmd.getDefaultDevice() == null) {
                cmd.response.add("Default Device:None");
                cmd.response.add(">");
            } else {
                cmd.response.add("Default Device " + cmd.getDefaultDevice().getDeviceIdStr());
                cmd.response.add(">");
            }
            retVal = true;
        }
        return retVal;
    }

    private boolean isActionCmd(CmdStruct cmd) {
        String sCmd = cmd.command.replaceAll("(\\r|\\n)", "");
        if (sCmd.toLowerCase().startsWith("rec") && sCmd.toLowerCase().endsWith("stream?")) {
            return false;
        }
        return sCmd.toLowerCase().startsWith("rec") && sCmd.toLowerCase().endsWith("stream");
    }

    private boolean testHoldOpenForActionCmd(CmdStruct cmd) {
        return this.isActionCmd(cmd);
    }

    private void testCmdIsActionCmd(DeviceListEntry dle, CmdStruct cmd) {
        if (this.isActionCmd(cmd) && dle.deviceInfo.isDeviceHasStream() && !cmd.response.isEmpty() && ((String)cmd.response.get(0)).toLowerCase().equals("ok") && dle.deviceInfo.isDeviceHasStream()) {
            dle.deviceInfo.enableStreamReceive(dle);
        }
    }

    private boolean cmdForInternalDevice(DeviceListEntry dle, CmdStruct cmd) {
        BaseDevice deviceAbstract = dle.deviceInfo.getDeviceAbstract();
        if (deviceAbstract != null) {
            if (deviceAbstract.myDle == null) {
                deviceAbstract.myDle = dle;
            }
            return deviceAbstract.internalCommand(cmd, null);
        }
        return false;
    }

    private void executeCommand(DeviceListEntry dle, CmdStruct cmd) {
        dle.deviceInfo.executeCommand(cmd);
        if (cmd.response.isEmpty()) {
            cmd.addReplyStr("Connection Timeout");
            cmd.addReplyStr(">");
        }
    }

    private void delayedDeviceIdentify(DeviceListEntry dle) {
        if (dle.deviceInfo.getDeviceAbstract().isNeedFurtherIndetification()) {
            dle.deviceInfo.getDeviceAbstract().clearNeedFurtherIndetification();
            DeviceFactory df = DeviceFactory.getInstance();
            BaseDevice device = df.createDevice(dle, true);
            deviceList.mergeReplaceIdentifiedDevice(dle, device);
        }
    }

    public void forwardCommand(CmdStruct cmd) {
        EmbeddedDeviceRef edr = new EmbeddedDeviceRef(cmd.command);
        cmd.action = 666;
        if (!edr.devSpec.getDeviceName().equals("")) {
            if (edr.dle != null) {
                DeviceListEntry device = edr.dle;
                cmd.replaceCommand(edr.cmd);
                DeviceRef getqDevice = device.deviceInfo.getqDevice();
                if (getqDevice != null && !getqDevice.getCurrentCommsInterfaceType().toString().contains(edr.devSpec.interfaceName)) {
                    getqDevice.setCurrentCommsInterface(CommsInterfaceType.fromString((String)edr.devSpec.interfaceName));
                }
                this.processCommand(cmd, device);
            } else {
                cmd.addReplyStr("Device " + edr.devSpec.interfaceName + "::" + edr.devSpec.getDeviceName() + " Invalid or No Longer Valid");
                cmd.addReplyStr(">");
            }
        } else if (cmd.getDefaultDevice() != null) {
            if (deviceList.findDevice(cmd.getDefaultDevice().serialNo, cmd.getDefaultDevice().deviceInfo.getIntefaceName()) >= 0) {
                DeviceListEntry device = cmd.getDefaultDevice();
                DeviceRef getqDevice = device.deviceInfo.getqDevice();
                if (getqDevice != null && !getqDevice.getCurrentCommsInterfaceType().toString().contains(device.deviceInfo.getIntefaceName())) {
                    getqDevice.setCurrentCommsInterface(CommsInterfaceType.fromString((String)device.deviceInfo.getIntefaceName()));
                }
                this.processCommand(cmd, device);
            } else {
                cmd.addReplyStr("Default Device " + cmd.getDefaultDevice().getDeviceIdStr() + " No Longer Valid");
                cmd.addReplyStr(">");
                cmd.setDefaultDevice(null);
                cmd.defaultDeviceIsValid = false;
            }
        } else {
            cmd.addReplyStr("No Target Device Specified");
            cmd.addReplyStr(">");
            cmd.action = 333;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processCommand(CmdStruct cmd, DeviceListEntry device) {
        this.delayedDeviceIdentify(device);
        if (!(this.cmdForInternalDevice(device, cmd) || this.forceIPAddress(cmd, device) || this.forceReset(cmd, device) || this.forceCloseConnection(cmd, device))) {
            if (this.debugSyncOnDevice) {
                DeviceListEntry deviceListEntry = device;
                synchronized (deviceListEntry) {
                    this._processCommand(cmd, device);
                }
            } else {
                this._processCommand(cmd, device);
            }
        }
    }

    private void _processCommand(CmdStruct cmd, DeviceListEntry device) {
        if (this.isActionCmd(cmd) && device.deviceInfo.isDeviceHasStream() && !this.actionCommandPreamble(cmd, device)) {
            cmd.response.add("FAIL: Device already streaming");
            return;
        }
        this.executeCommand(device, cmd);
        this.testCmdIsActionCmd(device, cmd);
    }

    private boolean forceCloseConnection(CmdStruct cmd, DeviceListEntry device) {
        if (cmd.command.trim().equalsIgnoreCase("close")) {
            QuarchDeviceInfo dInfo = device.deviceInfo;
            if (dInfo.isStreaming()) {
                dInfo.setForceCloseFlag();
                CmdStruct stopCmd = new CmdStruct();
                stopCmd.command = "rec stop";
                this.executeCommand(device, stopCmd);
                cmd.addReplyStr("OK");
                cmd.addReplyStr(">");
                cmd.action = 333;
                return true;
            }
            dInfo.setForceCloseFlag();
            dInfo.forceClose();
            cmd.action = 333;
            cmd.addReplyStr("OK");
            cmd.addReplyStr(">");
            return true;
        }
        return false;
    }

    public static void forceIPAddress(DeviceListEntry dle, String ip4Address) throws IOException {
        if (dle.deviceInfo.getDeviceAbstract().isHasREST() || dle.deviceInfo.getDeviceAbstract().isHasTelnet() || dle.deviceInfo.getDeviceAbstract().isHasUSB()) {
            String mac = dle.deviceInfo.MACAddress;
            EthernetMChip30303Locate.sendString("ForceIP: " + mac + " " + ip4Address);
        }
    }

    public static void forceReset(DeviceListEntry dle) throws IOException {
        if (dle.deviceInfo.getDeviceAbstract().isHasREST() || dle.deviceInfo.getDeviceAbstract().isHasTelnet() || dle.deviceInfo.getDeviceAbstract().isHasUSB()) {
            String mac = dle.deviceInfo.MACAddress;
            EthernetMChip30303Locate.sendString("ForceReset: " + mac);
        }
    }

    private boolean forceIPAddress(CmdStruct cmd, DeviceListEntry device) {
        if (cmd.command.toLowerCase().startsWith("forceIPAddress".toLowerCase())) {
            String[] parts = cmd.command.split(" ");
            if (parts.length == 2) {
                if (device.deviceInfo.MACAddress.isEmpty()) {
                    cmd.addReplyStr("Device MAC Address is Unknown or Invalid");
                    cmd.addReplyStr(">");
                } else {
                    try {
                        QbeInterfaceWrapper.forceIPAddress(device, parts[1].trim());
                        cmd.addReplyStr("Request Sent");
                        cmd.addReplyStr(">");
                    }
                    catch (IOException e) {
                        cmd.addReplyStr("IO Exception During Send " + e.getMessage());
                        cmd.addReplyStr(">");
                    }
                }
                cmd.action = 333;
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean forceReset(CmdStruct cmd, DeviceListEntry device) {
        if (cmd.command.toLowerCase().startsWith("forceReset".toLowerCase())) {
            String[] parts = cmd.command.split(" ");
            if (parts.length == 1) {
                if (device.deviceInfo.MACAddress.isEmpty()) {
                    cmd.addReplyStr("Device MAC Address is Unknown or Invalid");
                    cmd.addReplyStr(">");
                } else {
                    try {
                        QbeInterfaceWrapper.forceReset(device);
                        cmd.addReplyStr("Request Sent");
                        cmd.addReplyStr(">");
                    }
                    catch (IOException e) {
                        cmd.addReplyStr("IO Exception During Send " + e.getMessage());
                        cmd.addReplyStr(">");
                    }
                }
                cmd.action = 333;
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean actionCommandPreamble(CmdStruct cmd, DeviceListEntry device) {
        if (this.isActionCmd(cmd)) {
            if (device.deviceInfo.checkStreamSupport()) {
                DeviceRef getqDevice = device.deviceInfo.getqDevice();
                if (getqDevice != null && !getqDevice.getCurrentCommsInterfaceType().getText().equalsIgnoreCase(device.deviceInfo.getIntefaceName())) {
                    getqDevice.setCurrentCommsInterface(CommsInterfaceType.fromString((String)device.deviceInfo.getIntefaceName().toUpperCase()));
                }
                device.deviceInfo.openForStream();
                CmdStruct enableStream = new CmdStruct();
                enableStream.command = "conf stream enable on";
                this.executeCommand(device, enableStream);
                DecodedSerialNumber dsn = new DecodedSerialNumber(device);
                if (dsn.getDeviceType() == DeviceTypes.hd) {
                    CmdStruct getMode = new CmdStruct();
                    getMode.command = "conf out mode?";
                    this.executeCommand(device, getMode);
                    if (Character.isDigit(((String)getMode.response.get(0)).charAt(0))) {
                        BaseDevice dev = device.deviceInfo.getDeviceAbstract();
                        dev.getDeviceOptions().put("Conf:Out:Mode", ((String)getMode.response.get(0)).trim());
                    } else {
                        BaseDevice dev = device.deviceInfo.getDeviceAbstract();
                        dev.getDeviceOptions().put("Conf:Out:Mode", null);
                    }
                }
                device.deviceInfo.openForStream();
            }
            return device.deviceInfo.openForStream();
        }
        return false;
    }
}

