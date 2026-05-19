/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceBase
 *  commsInterface.CommsInterfaceType
 */
package src.com.quarch.deviceAbstractions;

import commsInterface.CommsInterfaceBase;
import commsInterface.CommsInterfaceType;
import src.com.quarch.deviceAbstractions.DecodedSerialNumber;
import src.com.quarch.deviceDetected.DetectedDevice;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceMOM.MOM;
import src.com.quarch.devicePPM.HDPPM;
import src.com.quarch.devicePPM.PPM;
import src.com.quarch.devicePPM.XLC;
import src.com.quarch.deviceXML.XMLDeviceDefinition;
import src.com.quarch.devices.BaseDevice;
import src.com.quarch.devices.BaseStreamDevicePPM;

public final class DeviceFactory {
    private static volatile DeviceFactory instance;

    private DeviceFactory() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static DeviceFactory getInstance() {
        if (instance != null) return instance;
        Class<DeviceFactory> clazz = DeviceFactory.class;
        synchronized (DeviceFactory.class) {
            if (instance != null) return instance;
            instance = new DeviceFactory();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    public BaseDevice createDevice(DeviceListEntry dle, boolean forceIdentify) {
        BaseDevice baseDev = null;
        if ((forceIdentify || !DeviceFactory.isExcludedInterface(dle)) && (baseDev = this.createDevice_XML(dle)) != null) {
            CommsInterfaceBase.logString((String)("DeviceFactory->createDevice forced identify " + baseDev));
            baseDev.clearNeedFurtherIndetification();
            return baseDev;
        }
        if (!dle.deviceInfo.getIntefaceName().equalsIgnoreCase(CommsInterfaceType.ETHTELNET.getText())) {
            baseDev = this.createDevice_SerialNumber(dle);
        }
        if (baseDev != null && !DeviceFactory.isExcludedInterface(dle)) {
            baseDev.clearNeedFurtherIndetification();
        }
        CommsInterfaceBase.logString((String)("DeviceFactory->createDevice S# identify " + baseDev));
        return baseDev;
    }

    public static boolean isExcludedInterface(DeviceListEntry dle) {
        if (dle.deviceInfo.getIntefaceName().equalsIgnoreCase(CommsInterfaceType.VIRTUAL.getText())) {
            return true;
        }
        if (dle.deviceInfo.getIntefaceName().equalsIgnoreCase(CommsInterfaceType.ETHTCP.getText())) {
            return true;
        }
        return dle.deviceInfo.getIntefaceName().equalsIgnoreCase(CommsInterfaceType.ETHTELNET.getText());
    }

    private BaseDevice createDevice_SerialNumber(DeviceListEntry dle) {
        BaseDevice baseDev = null;
        DecodedSerialNumber dsn = new DecodedSerialNumber(dle);
        if (dsn.isValid()) {
            switch (dsn.getDeviceType()) {
                case hd: {
                    baseDev = new HDPPM(dle);
                    break;
                }
                case xlc: {
                    baseDev = new XLC(dle);
                    break;
                }
                case ppm: {
                    baseDev = new PPM(dle);
                    break;
                }
                default: {
                    if (!DetectedDevice.isValid(dle)) break;
                    baseDev = new DetectedDevice(dle);
                }
            }
        }
        CommsInterfaceBase.logString((String)("DeviceFactory->createDevice_SerialNumber " + baseDev));
        return baseDev;
    }

    private BaseDevice createDevice_XML(DeviceListEntry dle) {
        BaseStreamDevicePPM baseDev = null;
        QuarchDeviceInfo deviceInfo = dle.deviceInfo;
        XMLDeviceDefinition xmlDev = deviceInfo.loadXMLDev();
        if (xmlDev == null) {
            return null;
        }
        baseDev = xmlDev.Identity.isPAM() ? new MOM(dle) : (xmlDev.Identity.isPPM() ? new HDPPM(dle) : (xmlDev.Identity.isPPM_PAM() ? new MOM(dle) : null));
        CommsInterfaceBase.logString((String)("DeviceFactory->createDevice_XML " + baseDev));
        return baseDev;
    }
}

