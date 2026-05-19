/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commandBase.CmdStruct
 */
package src.com.quarch.beCommandData;

import java.util.ArrayList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.IFCustomManagedBuffer;

public class CmdStruct
extends commandBase.CmdStruct {
    private DeviceListEntry defaultDevice;
    private IFCustomManagedBuffer cmBuffer;
    private boolean debugFilterLongReply = false;
    private StringBuilder stringBuilder;
    private long roundTrip_uS = -1L;

    public CmdStruct() {
        this.command = "";
        this.longCommand = new ArrayList();
        this.response = new ArrayList();
        this.bArray = null;
        this.setStringBuilder(null);
        this.backEndDeviceHandler = null;
        this.action = -1;
    }

    public DeviceListEntry getDefaultDevice() {
        return this.defaultDevice;
    }

    public void setDefaultDevice(DeviceListEntry defaultDevice) {
        this.defaultDevice = defaultDevice;
    }

    public IFCustomManagedBuffer getCmBuffer() {
        return this.cmBuffer;
    }

    public void setCmBuffer(IFCustomManagedBuffer cmBuffer) {
        this.cmBuffer = cmBuffer;
    }

    public boolean isDebugFilterLongReply() {
        return this.debugFilterLongReply;
    }

    public void setDebugFilterLongReply(boolean debugFilterLongReply) {
        this.debugFilterLongReply = debugFilterLongReply;
    }

    public StringBuilder getStringBuilder() {
        return this.stringBuilder;
    }

    public void setStringBuilder(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public long getRoundTrip_uS() {
        return this.roundTrip_uS;
    }

    public void setRoundTrip_uS(long roundTrip_uS) {
        this.roundTrip_uS = roundTrip_uS;
    }
}

