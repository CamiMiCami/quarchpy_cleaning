/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beRecordedData.RecordedDataSet;
import src.com.quarch.beStream.StreamDataReadyListener;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceMOM.StreamHeader;
import src.com.quarch.devicePPM.EnumPPMAverage;
import src.com.quarch.devicePPM.EnumPPMTrigger;
import src.com.quarch.deviceVirtual.VirtualDevice;
import src.com.quarch.deviceXML.XMLDeviceDefinition;

public class BaseDevice {
    private static final String NAME_PREFIX_SEPARATOR = "_";
    private boolean hasUSB = false;
    private boolean hasTelnet = false;
    private boolean hasREST = false;
    private boolean hasUDP = false;
    private boolean hasSerial = false;
    public boolean hasStream = false;
    private Map<String, String> deviceOptions = new HashMap<String, String>();
    public DeviceListEntry myDle;
    private boolean needFurtherIndetification = true;
    public static final int sizeOfInt = 4;
    protected ArrayList<String> idnInfo = new ArrayList();
    private VirtualDevice virtualOwner = null;
    private String channelNamePrefix = "";
    private AtomicInteger groupOffset = new AtomicInteger(0);
    private boolean virtualSourceDevice;
    private boolean isVirtualDeviceMember = false;
    public CountDownLatch allheadersReadSignal;
    private int deviceIndex;
    protected StreamHeader streamHeaderRef;
    public AtomicInteger totalGroups = new AtomicInteger(0);
    public XMLDeviceDefinition xmlREF = null;

    public boolean responseOkay(CmdStruct cmd) {
        boolean retVal = false;
        if (cmd.response.isEmpty()) {
            return false;
        }
        for (String s : cmd.response) {
            if (s.toLowerCase().startsWith("fail")) {
                retVal = false;
                continue;
            }
            if (s.toLowerCase().startsWith(">")) {
                retVal = true;
                continue;
            }
            retVal = false;
        }
        return retVal;
    }

    public CmdStruct sendCmd(String cmdStr) {
        CmdStruct cmd = new CmdStruct();
        cmd.command = cmdStr;
        this.myDle.deviceInfo.executeCommand(cmd);
        return cmd;
    }

    public void sendCmdIDN() {
        if (this.myDle != null) {
            CmdStruct cmd = this.sendCmd("*idn?");
            this.idnInfo.clear();
            this.idnInfo.addAll(cmd.response);
        }
    }

    public void enableStreamReceive(DeviceListEntry dle) {
    }

    public void disableStreamReceive() {
    }

    public boolean internalCommand(CmdStruct cmd, Object caller) {
        return false;
    }

    public int getStreamData(byte[] outBuffer, int maxLen) {
        return 0;
    }

    public boolean getStreamHeader() {
        return false;
    }

    public void addDataReadyListener(StreamDataReadyListener toAdd) {
    }

    public boolean isHasUSB() {
        return this.hasUSB;
    }

    public void setHasUSB(boolean hasUSB) {
        this.hasUSB = hasUSB;
    }

    public boolean isHasTelnet() {
        return this.hasTelnet;
    }

    public void setHasTelnet(boolean hasTelnet) {
        this.hasTelnet = hasTelnet;
    }

    public boolean isHasREST() {
        return this.hasREST;
    }

    public void setHasREST(boolean hasREST) {
        this.hasREST = hasREST;
    }

    public boolean isHasUDP() {
        return this.hasUDP;
    }

    public void setHasUDP(boolean hasUDP) {
        this.hasUDP = hasUDP;
    }

    public boolean isHasSerial() {
        return this.hasSerial;
    }

    public void setHasSerial(boolean hasSerial) {
        this.hasSerial = hasSerial;
    }

    public boolean holdDeviceOpen() {
        return false;
    }

    public boolean forceDeviceClose() {
        return false;
    }

    public StreamHeader getStreamHeaderRef() {
        return this.streamHeaderRef;
    }

    protected void setHasStream(boolean hasStream) {
        this.hasStream = hasStream;
    }

    public boolean getStreamHeader(CmdStruct cmd) {
        return false;
    }

    public ArrayList<String> getStreamHeaderStrings() {
        return null;
    }

    public EnumPPMAverage getAveraging() {
        return EnumPPMAverage.AV_32K;
    }

    public boolean setAveraging(EnumPPMAverage av32k) {
        return false;
    }

    public boolean getPowerState() {
        return false;
    }

    public boolean setV5VoltageSetting(int v) {
        return false;
    }

    public int getV5VoltageSetting() {
        return -1;
    }

    public boolean setV12VoltageSetting(int v) {
        return false;
    }

    public int getV12VoltageSetting() {
        return -1;
    }

    public boolean setPowerState(boolean b) {
        return false;
    }

    public boolean setTrigger(EnumPPMTrigger thisTrigger) {
        return false;
    }

    public boolean startRecord() {
        return false;
    }

    public boolean stopRecord() {
        return false;
    }

    public RecordedDataSet getRecordedData() {
        return null;
    }

    public Map<String, String> getDeviceOptions() {
        return this.deviceOptions;
    }

    public void setDeviceOptions(Map<String, String> deviceOptions) {
        this.deviceOptions = deviceOptions;
    }

    public VirtualDevice getVirtualOwner() {
        return this.virtualOwner;
    }

    public void setVirtualOwner(VirtualDevice virtualOwner, String prefix, AtomicInteger channelOffset, AtomicInteger totalGroups, CountDownLatch allheadersReadSignal, int deviceIndex) {
        this.virtualOwner = virtualOwner;
        if (virtualOwner == null) {
            this.setChannelNamePrefix("");
            this.setGroupOffset(new AtomicInteger(0));
            this.totalGroups = new AtomicInteger(0);
            this.allheadersReadSignal = null;
            this.deviceIndex = -1;
            this.isVirtualDeviceMember = false;
        } else {
            this.setChannelNamePrefix(prefix);
            this.setGroupOffset(channelOffset);
            this.totalGroups = totalGroups;
            this.allheadersReadSignal = allheadersReadSignal;
            this.deviceIndex = deviceIndex;
            this.isVirtualDeviceMember = true;
        }
    }

    public void headerReceived(int groupCount) {
        this.getVirtualOwner().headerReceivedFor(this.deviceIndex, groupCount);
    }

    public void allHeadersWaitDone() {
        this.getVirtualOwner().allHeadersWaitDone(this.deviceIndex);
    }

    public boolean isVirtualSourceDevice() {
        return this.virtualSourceDevice;
    }

    public void setVirtualSourceDevice(boolean value) {
        this.virtualSourceDevice = value;
    }

    public boolean isVirtualDeviceMember() {
        return this.isVirtualDeviceMember;
    }

    public boolean isHasStream() {
        return this.hasStream;
    }

    public boolean isNeedFurtherIndetification() {
        return this.needFurtherIndetification;
    }

    protected void setNeedFurtherIndetification(boolean needFurtherIndetification) {
        this.needFurtherIndetification = needFurtherIndetification;
    }

    public void clearNeedFurtherIndetification() {
        this.setNeedFurtherIndetification(false);
    }

    public AtomicInteger getGroupOffset() {
        return this.groupOffset;
    }

    public void setGroupOffset(AtomicInteger groupOffset) {
        this.groupOffset = groupOffset;
    }

    public int getGroupOffsetValue() {
        return this.getGroupOffset().get();
    }

    public void setGroupOffsetValue(int value) {
        this.getGroupOffset().set(value);
    }

    public String getChannelNamePrefix() {
        return this.channelNamePrefix;
    }

    public String getExtendedChannelNamePrefix() {
        String localChannelNamePrefix = this.getChannelNamePrefix();
        if (localChannelNamePrefix.isEmpty()) {
            return "";
        }
        return localChannelNamePrefix + NAME_PREFIX_SEPARATOR;
    }

    public void setChannelNamePrefix(String channelNamePrefix) {
        this.channelNamePrefix = channelNamePrefix;
    }
}

