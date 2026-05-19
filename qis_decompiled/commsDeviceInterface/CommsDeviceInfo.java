/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commandBase.CmdStruct
 *  commsInterface.CommsInterfaceType
 *  device.DeviceRef
 */
package commsDeviceInterface;

import commandBase.CmdStruct;
import commsInterface.CommsInterfaceType;
import device.DeviceRef;
import java.util.List;
import src.com.quarch.deviceAbstractions.DeviceDetails;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.devices.DeviceDataBuffer;
import src.com.quarch.utils.DebugUtil;

public class CommsDeviceInfo
extends QuarchDeviceInfo {
    private boolean isStreaming;
    private String altSerialNo = null;
    protected boolean isHeldOpen;

    public CommsDeviceInfo(DeviceRef item) {
        this.setqDevice(item);
        this.basic_init();
    }

    private void basic_init() {
        this.setIntefaceName(this.getqDevice().getCurrentCommsInterfaceType().getText());
        this.ipAddress = this.getqDevice().getCurrentCommsInterfaceAddress();
        this.serialNumberStr = this.getqDevice().getCommsSerialNumber();
    }

    @Override
    public void freeDevice() {
    }

    @Override
    public void executeCommand(src.com.quarch.beCommandData.CmdStruct cmd) {
        if (!this.getqDevice().getCurrentCommsInterfaceType().getText().equalsIgnoreCase(this.getIntefaceName())) {
            this.getqDevice().setCurrentCommsInterface(CommsInterfaceType.fromString((String)this.getIntefaceName().toUpperCase()));
        }
        this.cmdLock.acquireUninterruptibly();
        this.getqDevice().executeCommand((CmdStruct)cmd);
        this.cmdLock.release();
    }

    @Override
    public void executeBulkDataCommand(src.com.quarch.beCommandData.CmdStruct cmd) {
        this.getqDevice().executeBulkDataCommand((CmdStruct)cmd);
    }

    @Override
    public boolean openForStream() {
        boolean retVal = true;
        if (!this.getqDevice().isOpen()) {
            this.getqDevice().open();
        }
        this.getqDevice().setStreaming(true);
        if (this.getqDevice().isStreaming()) {
            retVal = true;
        }
        return retVal;
    }

    @Override
    public void closeStream() {
        this.getqDevice().setStreaming(false);
        this.getqDevice().close();
    }

    @Override
    public DeviceDataBuffer readStream() {
        DeviceDataBuffer ddBuff = new DeviceDataBuffer();
        if (!this.getqDevice().getCurrentCommsInterfaceType().getText().equalsIgnoreCase(this.getIntefaceName())) {
            this.getqDevice().setCurrentCommsInterface(CommsInterfaceType.fromString((String)this.getIntefaceName().toUpperCase()));
        }
        if (!this.getqDevice().isOpen()) {
            this.getqDevice().open();
        }
        ddBuff.buffer = this.getqDevice().readStream();
        if (ddBuff.buffer != null) {
            ddBuff.len = ddBuff.buffer.length;
            return ddBuff;
        }
        return null;
    }

    @Override
    public boolean isStreaming() {
        return this.isStreaming;
    }

    @Override
    public void sendSyncAck() {
        this.getqDevice().sendSyncAck();
    }

    @Override
    public boolean checkStreamSupport() {
        return DeviceDetails.sNoHasStream(this.getDeviceAbstract(), this.getIntefaceName());
    }

    @Override
    public void sendStreamContinue() {
        DebugUtil.debugMsgln("sendStreamContinue not implimented");
    }

    @Override
    public String getSerialNumberStr() {
        if (this.getAltSerialNo() == null) {
            return this.serialNumberStr;
        }
        return this.getAltSerialNo();
    }

    @Override
    public void setSerialNumberStr(String serialNumberStr) {
        if (serialNumberStr == null) {
            return;
        }
        this.serialNumberStr = serialNumberStr.equals("") ? "Unknown" : (Character.isDigit(serialNumberStr.charAt(0)) ? "QTL" + serialNumberStr.toLowerCase().trim() : serialNumberStr.trim());
        this.setDeviceHasStream(this.checkStreamSupport());
    }

    @Override
    public String getAltSerialNo() {
        return this.altSerialNo;
    }

    @Override
    public void setAltSerialNo(String altSerialNo) {
        this.altSerialNo = altSerialNo;
    }

    @Override
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

    @Override
    public void getExtendedInfo() {
        src.com.quarch.beCommandData.CmdStruct cmd = new src.com.quarch.beCommandData.CmdStruct();
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
                this.setSerialNumberStr(this.idnSerialNo);
                this.setAltSerialNo(this.idnEnclosureNo);
            }
        }
    }

    @Override
    public void holdOpen() {
        this.getqDevice().holdOpen();
    }

    @Override
    public void forceClose() {
        this.getqDevice().forceClose();
    }

    @Override
    public void lateInitialise() {
        this.setSerialNumberStr(this.idnSerialNo);
    }

    @Override
    public void setForceCloseFlag() {
        this.getqDevice().setForceCloseFlag();
        DebugUtil.debugMsgln("setForceCloseFlag not implimented");
    }
}

