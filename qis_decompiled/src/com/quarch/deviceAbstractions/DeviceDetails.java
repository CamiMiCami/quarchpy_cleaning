/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceAbstractions;

import java.util.Arrays;
import src.com.quarch.deviceAbstractions.DeviceTypes;
import src.com.quarch.devices.BaseDevice;

public class DeviceDetails {
    public static final String USBInterfaceNameStr = "usb";
    public static final String UDPInterfaceNameStr = "udp";
    public static final String TCPInterfaceNameStr = "tcp";
    public static final String RESTInterfaceNameStr = "rest";
    private static String[] XLCPPMs = new String[]{"qtl1824", "qtl1847"};
    private static String[] CalPPMs = new String[]{"qtl1455", "qtl1658", "qtl1727", "qtl1730"};
    private static String[] HDPPMs = new String[]{"qtl1944", "qtl1995", "qtl1999"};
    private static String[] MOMs = new String[]{"qtl2098", "qtl2312", "qtl2582", "qtl2602", "qtl2789", "qtl2751", "qtl2834", "qtl2843"};

    public static DeviceTypes sNoToDeviceType(String sNo) {
        String devName = sNo.substring(0, 7).toLowerCase();
        if (Arrays.asList(XLCPPMs).contains(devName)) {
            return DeviceTypes.xlc;
        }
        if (Arrays.asList(CalPPMs).contains(devName)) {
            return DeviceTypes.ppm;
        }
        if (Arrays.asList(HDPPMs).contains(devName)) {
            return DeviceTypes.hd;
        }
        return DeviceTypes.unknown;
    }

    public static boolean sNoHasStream(String sNo, String intFName) {
        if (sNo.length() < 7) {
            return false;
        }
        String devName = sNo.substring(0, 7).toLowerCase();
        for (String s : XLCPPMs) {
            if (!s.equalsIgnoreCase(devName)) continue;
            return true;
        }
        for (String s : CalPPMs) {
            if (!s.equalsIgnoreCase(devName)) continue;
            return true;
        }
        for (String s : HDPPMs) {
            if (!s.equalsIgnoreCase(devName) || !intFName.equalsIgnoreCase(USBInterfaceNameStr) && !intFName.equalsIgnoreCase(TCPInterfaceNameStr)) continue;
            return true;
        }
        return false;
    }

    public static boolean sNoHasStream(DeviceTypes deviceType, String intFName) {
        boolean hasStream = deviceType == DeviceTypes.xlc ? true : (deviceType == DeviceTypes.ppm ? true : (deviceType == DeviceTypes.hd && (intFName.equalsIgnoreCase(USBInterfaceNameStr) || intFName.equalsIgnoreCase(TCPInterfaceNameStr)) ? true : deviceType == DeviceTypes.mom && (intFName.equalsIgnoreCase(USBInterfaceNameStr) || intFName.equalsIgnoreCase(TCPInterfaceNameStr))));
        return hasStream;
    }

    public static boolean sNoHasStream(BaseDevice deviceAbstract, String intefaceName) {
        return deviceAbstract.isHasStream() && (intefaceName.equalsIgnoreCase(USBInterfaceNameStr) || intefaceName.equalsIgnoreCase(TCPInterfaceNameStr));
    }
}

