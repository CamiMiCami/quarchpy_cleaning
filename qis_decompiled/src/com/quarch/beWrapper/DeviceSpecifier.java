/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceType
 */
package src.com.quarch.beWrapper;

import commsInterface.CommsInterfaceType;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DeviceSpecifier {
    public static final String VIRTUALinterfaceNameStr = CommsInterfaceType.VIRTUAL.getText();
    public String interfaceName;
    public String connectionOption;
    private String deviceName;
    public String origDeviceName;
    public String idnName;
    public static final String USBinterfaceNameStr = CommsInterfaceType.USB.getText();
    public static final String TCPinterfaceNameStr = CommsInterfaceType.ETHTCP.getText();
    public static final String RESTinterfaceNameStr = CommsInterfaceType.ETHREST.getText();
    public String deviceAddress;
    public int portNumber;
    public int timeout;
    public boolean specifiedByManualIP;
    public static final String PortSeparator = ":";
    public static final String TimeoutSeparator = "%";
    public static final String CmdSeparator = " ";
    private int portOffset;
    private int timeoutOffset;

    public DeviceSpecifier(String cmdLine) {
        String command = cmdLine.trim().toUpperCase();
        int cmdOffset = 0;
        int devOffset = -1;
        this.specifiedByManualIP = false;
        this.interfaceName = "";
        this.portNumber = -1;
        this.timeout = -1;
        this.deviceName = "";
        this.idnName = "";
        if (command.indexOf(USBinterfaceNameStr + "::") != -1) {
            this.interfaceName = USBinterfaceNameStr;
            devOffset = command.indexOf(USBinterfaceNameStr + "::");
            this.connectionOption = "";
        } else if (command.indexOf(RESTinterfaceNameStr + "::") != -1) {
            this.interfaceName = RESTinterfaceNameStr;
            devOffset = command.indexOf(RESTinterfaceNameStr + "::");
            this.connectionOption = RESTinterfaceNameStr;
        } else if (command.indexOf(TCPinterfaceNameStr + "::") != -1) {
            this.interfaceName = TCPinterfaceNameStr;
            devOffset = command.indexOf(TCPinterfaceNameStr + "::");
            this.connectionOption = TCPinterfaceNameStr;
        } else if (command.indexOf(VIRTUALinterfaceNameStr + "::") != -1) {
            this.interfaceName = VIRTUALinterfaceNameStr;
            devOffset = command.indexOf(VIRTUALinterfaceNameStr + "::");
            this.connectionOption = VIRTUALinterfaceNameStr;
        } else {
            return;
        }
        int minLength = this.interfaceName.length() + "::".length();
        int firstSpace = command.indexOf(CmdSeparator, minLength + devOffset);
        this.portOffset = command.indexOf(PortSeparator, minLength + devOffset);
        if (this.portOffset > 0 && this.portOffset < firstSpace) {
            this.portNumber = this.extractIntValue(command, this.portOffset + 1);
        } else {
            this.portOffset = Integer.MAX_VALUE;
        }
        this.timeoutOffset = command.indexOf(TimeoutSeparator, minLength + devOffset);
        if (this.timeoutOffset > 0 && this.timeoutOffset < firstSpace) {
            this.timeout = this.extractIntValue(command, this.timeoutOffset + 1);
        } else {
            this.timeoutOffset = Integer.MAX_VALUE;
        }
        int devEndOffset = Integer.min(this.portOffset, this.timeoutOffset);
        devEndOffset = Integer.min(devEndOffset, command.length());
        if (devOffset == 0) {
            devOffset = this.interfaceName.length() + "::".length();
            cmdOffset = Integer.min(command.indexOf(CmdSeparator), devEndOffset);
            if (cmdOffset < 0 || cmdOffset > command.length()) {
                cmdOffset = command.length();
            }
            this.setDeviceName(command.substring(devOffset, cmdOffset).trim());
        } else if (devOffset > 0) {
            devOffset = devOffset + this.interfaceName.length() + "::".length();
            this.setDeviceName(command.substring(devOffset, devEndOffset).trim());
        }
        if (!this.getDeviceName().equals("") && this.validateIpAddress(this.getDeviceName())) {
            this.deviceAddress = this.getDeviceName();
            this.specifiedByManualIP = true;
        }
    }

    private boolean validateIpOctet(String octetStr) {
        boolean retVal = false;
        try {
            int octet = Integer.parseInt(octetStr);
            if (octet >= 0 && octet <= 255) {
                retVal = true;
            }
        }
        catch (NumberFormatException e) {
            retVal = false;
        }
        return retVal;
    }

    private boolean validateIpAddress(String addr) {
        boolean retVal = false;
        if (!Character.isDigit(addr.charAt(0))) {
            return false;
        }
        String[] segments = addr.split("\\.");
        if (segments.length == 4) {
            retVal = this.validateIpOctet(segments[0]) && this.validateIpOctet(segments[1]) && this.validateIpOctet(segments[2]) && this.validateIpOctet(segments[3]);
        }
        return retVal;
    }

    private int extractIntValue(String s, int startOffset) {
        int retVal = -1;
        if (startOffset > 0) {
            int endindex = startOffset + 1;
            boolean done = false;
            while (!done) {
                if (endindex >= s.length()) {
                    done = true;
                    continue;
                }
                if (Character.isDigit(s.charAt(endindex))) {
                    ++endindex;
                    continue;
                }
                done = true;
            }
            try {
                retVal = Integer.valueOf(s.substring(startOffset, endindex));
            }
            catch (NumberFormatException e) {
                retVal = -1;
            }
        }
        return retVal;
    }

    public void deviceAddressLookup() {
        if (!this.getDeviceName().equals("")) {
            if (!this.validateIpAddress(this.getDeviceName())) {
                try {
                    String netbiosName = this.getDeviceName();
                    if (netbiosName.length() > 15) {
                        netbiosName = netbiosName.substring(3);
                    }
                    InetAddress address = InetAddress.getByName(netbiosName);
                    this.deviceAddress = address.getHostAddress();
                }
                catch (UnknownHostException e) {
                    this.deviceAddress = "Bad Address";
                }
            } else {
                this.deviceAddress = this.getDeviceName();
            }
            if (this.validateIpAddress(this.deviceAddress)) {
                this.specifiedByManualIP = true;
            }
        }
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.origDeviceName = deviceName;
        this.deviceName = deviceName.toLowerCase();
    }
}

