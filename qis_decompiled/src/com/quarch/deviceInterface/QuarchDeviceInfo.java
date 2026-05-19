/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceBase
 *  commsInterface.CommsInterfaceType
 *  device.DeviceRef
 */
package src.com.quarch.deviceInterface;

import commsInterface.CommsInterfaceBase;
import commsInterface.CommsInterfaceType;
import device.DeviceRef;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.deviceAbstractions.DeviceDetails;
import src.com.quarch.deviceAbstractions.DeviceTypes;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceXML.XMLDeviceDefinition;
import src.com.quarch.devices.BaseDevice;
import src.com.quarch.devices.DeviceDataBuffer;
import src.com.quarch.utils.DebugUtil;

public class QuarchDeviceInfo {
    private static final String NA_ = "na";
    private String intefaceName;
    public int vendorID;
    public int productID;
    public String manufacturerStr;
    public String productStr;
    public String serialNumberStr;
    public String MACAddress = "";
    public String ipAddress;
    public String enclosureNumber;
    public String positionNumber;
    public int port = -1;
    public int timeout = -1;
    public int overridePort = -1;
    public int overrideTimeout = -1;
    public boolean isValid;
    public int cmdBufferSize;
    public int streamBufferSize;
    private boolean deviceHasStream;
    private DeviceRef qDevice;
    private BaseDevice deviceAbstract;
    protected XMLDeviceDefinition xmlDev;
    public final Semaphore cmdLock = new Semaphore(1, true);
    public String idnFamily;
    public String idnName;
    public String idnPartNo;
    public String idnProcessor;
    public String idnBootloader;
    public String idnFPGA1;
    public String idnSerialNo;
    public String idnEnclosureNo = "na";
    public String positionNo = "na";
    public String nbiosName = "na";
    private String altSerialNo = null;
    protected boolean isHeldOpen;

    public QuarchDeviceInfo() {
        this.initialise();
    }

    private void initialise() {
        this.setIntefaceName("");
        this.vendorID = -1;
        this.productID = -1;
        this.manufacturerStr = "";
        this.productStr = "";
        this.serialNumberStr = "";
        this.isValid = false;
    }

    public void freeDevice() {
    }

    public void executeCommand(CmdStruct cmd) {
        DebugUtil.debugMsgln("executeCommand not implimented");
    }

    public void executeBulkDataCommand(CmdStruct cmd) {
        DebugUtil.debugMsgln("executeCommand not implimented");
    }

    public boolean openForStream() {
        DebugUtil.debugMsgln("openForStream not implimented");
        return false;
    }

    public void closeStream() {
        DebugUtil.debugMsgln("closeStream not implimented");
    }

    public DeviceDataBuffer readStream() {
        DebugUtil.debugMsgln("DeviceDataBuffer not implimented");
        return null;
    }

    public boolean isStreaming() {
        DebugUtil.debugMsgln("isStreaming not implimented");
        return false;
    }

    public void sendSyncAck() {
        DebugUtil.debugMsgln("sendSyncAck not implimented");
    }

    public boolean checkStreamSupport() {
        DeviceTypes deviceType = this.getDeviceType();
        boolean hasStreamSupport = deviceType == null ? DeviceDetails.sNoHasStream(this.getSerialNumberStr(), this.getIntefaceName()) : DeviceDetails.sNoHasStream(deviceType, this.getIntefaceName());
        return hasStreamSupport;
    }

    public void sendStreamContinue() {
        DebugUtil.debugMsgln("sendStreamContinue not implimented");
    }

    public boolean isDeviceHasStream() {
        BaseDevice devAb = this.getDeviceAbstract();
        if (devAb == null) {
            return false;
        }
        return devAb.hasStream;
    }

    public String getSerialNumberStr() {
        if (this.getAltSerialNo() == null) {
            return this.serialNumberStr;
        }
        return this.getAltSerialNo();
    }

    public void setSerialNumberStr(String serialNumberStr) {
        if (serialNumberStr == null) {
            return;
        }
        this.serialNumberStr = serialNumberStr.equals("") ? "Unknown" : (Character.isDigit(serialNumberStr.charAt(0)) ? "QTL" + serialNumberStr.toLowerCase().trim() : serialNumberStr.trim());
        this.setDeviceHasStream(this.checkStreamSupport());
    }

    public String getAltSerialNo() {
        return this.altSerialNo;
    }

    public void setAltSerialNo(String altSerialNo) {
        this.altSerialNo = altSerialNo;
    }

    public boolean processBasicIDNResponse(List<String> response) {
        int responseCount = 0;
        boolean haveSerialNo = false;
        if (response.size() < 2) {
            return false;
        }
        for (String s : response) {
            String[] splitStr = s.split(":");
            if (splitStr[0].trim().toLowerCase().equals("family")) {
                this.idnFamily = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("name")) {
                this.idnName = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("part#")) {
                this.idnPartNo = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("processor")) {
                this.idnProcessor = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("bootloader")) {
                this.idnBootloader = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("fpga 1")) {
                this.idnFPGA1 = splitStr[1].trim();
                ++responseCount;
                continue;
            }
            if (splitStr[0].trim().toLowerCase().equals("serial#")) {
                this.idnSerialNo = splitStr[1].trim();
                haveSerialNo = true;
                ++responseCount;
                continue;
            }
            if (!splitStr[0].trim().toLowerCase().equals("enclosure#")) continue;
            this.idnEnclosureNo = splitStr[1].trim();
            haveSerialNo = true;
            ++responseCount;
        }
        return haveSerialNo;
    }

    public void getExtendedInfo() {
        CmdStruct cmd = new CmdStruct();
        cmd.command = "*idn?";
        DebugUtil.debugMsgln("Interface " + this.getIntefaceName());
        this.executeCommand(cmd);
        if (this.processBasicIDNResponse(cmd.response)) {
            if (this.idnSerialNo.equals("NO_SERIAL")) {
                String sNo = this.idnPartNo;
                if (!this.idnPartNo.endsWith("???")) {
                    sNo = sNo + "-???";
                }
                this.setSerialNumberStr(sNo);
            }
            if (this.idnEnclosureNo.toLowerCase().startsWith("qtl")) {
                this.setAltSerialNo(this.idnEnclosureNo);
            }
        }
    }

    public void setDeviceHasStream(boolean hasStream) {
        this.deviceHasStream = hasStream;
    }

    public void holdOpen() {
    }

    public void forceClose() {
    }

    public void lateInitialise() {
        this.setSerialNumberStr(this.idnSerialNo);
    }

    public void setForceCloseFlag() {
        DebugUtil.debugMsgln("setForceCloseFlag not implimented");
    }

    public boolean isIdnEnclosureValid() {
        return this.idnEnclosureNo != null && !this.idnEnclosureNo.equals(NA_);
    }

    public void enableStreamReceive(DeviceListEntry dle) {
        this.getDeviceAbstract().enableStreamReceive(dle);
    }

    public BaseDevice getDeviceAbstract() {
        return this.deviceAbstract;
    }

    public void setDeviceAbstract(BaseDevice deviceAbstract) {
        this.deviceAbstract = deviceAbstract;
    }

    public void setPrefix(String prefix) {
        this.getDeviceAbstract().setChannelNamePrefix(prefix);
    }

    public void setChannelOffset(AtomicInteger channelOffset) {
        this.getDeviceAbstract().setGroupOffset(channelOffset);
    }

    public DeviceRef getqDevice() {
        return this.qDevice;
    }

    public void setqDevice(DeviceRef qDevice) {
        this.qDevice = qDevice;
    }

    private String getConfigurationXML() {
        String retVal = null;
        CmdStruct cmd = new CmdStruct();
        cmd.command = "config:supports?";
        long timer = System.currentTimeMillis();
        this.executeCommand(cmd);
        CommsInterfaceBase.logString((String)("\tQuarchDevice->getConfigurationXML() time " + this.serialNumberStr + " " + this.getIntefaceName() + " " + (System.currentTimeMillis() - timer) + "ms (" + System.currentTimeMillis() + ")"));
        if (cmd.response.size() > 2) {
            StringBuilder sb = new StringBuilder();
            if (!((String)cmd.response.get(0)).contains("<?")) {
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            }
            for (int i = 0; i < cmd.response.size() - 1; ++i) {
                sb.append((String)cmd.response.get(i));
            }
            retVal = sb.toString();
        }
        return retVal;
    }

    public XMLDeviceDefinition loadXMLDev() {
        String xmlData;
        if (this.xmlDev == null && (xmlData = this.getConfigurationXML()) != null) {
            this.xmlDev = XMLDeviceDefinition.createFromXML(xmlData);
        }
        return this.xmlDev;
    }

    public XMLDeviceDefinition getExistingXMLDev() {
        return this.xmlDev;
    }

    public DeviceTypes getDeviceType() {
        if (this.getIntefaceName().equalsIgnoreCase(CommsInterfaceType.ETHTCP.getAlt1Text())) {
            return null;
        }
        XMLDeviceDefinition xmlDev = this.loadXMLDev();
        if (xmlDev == null) {
            return null;
        }
        DeviceTypes deviceType = xmlDev.Identity.isPAM() ? DeviceTypes.mom : (xmlDev.Identity.isPPM() ? DeviceTypes.hd : (xmlDev.Identity.isPPM_PAM() ? DeviceTypes.mom : DeviceTypes.unknown));
        return deviceType;
    }

    public String getIntefaceName() {
        return this.intefaceName;
    }

    public void setIntefaceName(String intefaceName) {
        this.intefaceName = intefaceName;
    }
}

