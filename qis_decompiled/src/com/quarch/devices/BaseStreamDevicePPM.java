/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beRecordedData.RecordedDataSet;
import src.com.quarch.beStream.StreamBase;
import src.com.quarch.beStream.StreamBufferStriped;
import src.com.quarch.beStream.StreamStatusCodes;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.devicePPM.EnumPPMAverage;
import src.com.quarch.devicePPM.EnumPPMTrigger;
import src.com.quarch.deviceXML.XMLDeviceDefinition;
import src.com.quarch.deviceXML.XMLFixture;
import src.com.quarch.devices.BaseDevice;

public class BaseStreamDevicePPM
extends BaseDevice {
    public static final String CONF_OUT_MODE = "Conf:Out:Mode";
    protected boolean terminalException;
    protected int stopSentCount;
    protected StreamBase streamBase;
    protected Thread steamThread;
    protected StreamState streamState;
    protected StreamBufferStriped streamBuffer;
    protected StreamStatusCodes streamStatusCode = StreamStatusCodes.STOPPED;
    public boolean powerOn = false;
    public int v5Voltage;
    public int v12Voltage;
    public EnumPPMAverage averaging;
    public long max5vVoltageValue;
    public long max5vCurrentValue;
    public long max12vVoltageValue;
    public long max12vCurrentValue;
    public long max5vPowerValue;
    public long max12vPowerValue;
    public long maxTotalPowerValue;
    protected BasePPM basePPM = new BasePPM(this);
    private String streamExceptionString = "";
    protected boolean isStreamRunning;
    protected long totalPacketDataLen = 0L;
    protected long absolutePacketDatacount = 0L;
    protected long repeatPacketDatacount = 0L;
    protected long deltaPacketDatacount = 0L;

    public void setTheoreticalPPMMaximums(long max5vVoltageValue, long max5vCurrentValue, long max12vVoltageValue, long max12vCurrentValue, long max5vPowerValue, long max12vPowerValue, long maxTotalPowerValue) {
        this.max5vVoltageValue = max5vVoltageValue;
        this.max5vCurrentValue = max5vCurrentValue;
        this.max12vVoltageValue = max12vVoltageValue;
        this.max12vCurrentValue = max12vCurrentValue;
        this.max5vPowerValue = max5vPowerValue;
        this.max12vPowerValue = max12vPowerValue;
        this.maxTotalPowerValue = maxTotalPowerValue;
    }

    @Override
    public boolean holdDeviceOpen() {
        this.myDle.deviceInfo.holdOpen();
        return true;
    }

    @Override
    public boolean forceDeviceClose() {
        this.myDle.deviceInfo.forceClose();
        return true;
    }

    @Override
    public EnumPPMAverage getAveraging() {
        CmdStruct cmd = this.sendCmd("RECord:AVEraging?");
        if (this.responseOkay(cmd)) {
            this.averaging = EnumPPMAverage.getPPMAverageEnum((String)cmd.response.get(0));
            return this.averaging;
        }
        return EnumPPMAverage.AV_32K;
    }

    @Override
    public boolean setAveraging(EnumPPMAverage avg) {
        CmdStruct cmd = this.sendCmd("RECord:AVEraging " + avg.toString());
        return this.responseOkay(cmd);
    }

    @Override
    public boolean setPowerState(boolean b) {
        CmdStruct cmd = b ? this.sendCmd("RUN:POWer up") : this.sendCmd("RUN:POWer down");
        return this.responseOkay(cmd);
    }

    @Override
    public boolean getPowerState() {
        CmdStruct cmd = this.sendCmd("RUN:POWer?");
        if (this.responseOkay(cmd)) {
            this.powerOn = ((String)cmd.response.get(0)).toLowerCase().startsWith("on");
            return this.powerOn;
        }
        return false;
    }

    @Override
    public boolean setV5VoltageSetting(int v) {
        CmdStruct cmd = this.sendCmd("SIGnal:5V:VOLTage " + new Integer(v).toString());
        return this.responseOkay(cmd);
    }

    @Override
    public int getV5VoltageSetting() {
        CmdStruct cmd = this.sendCmd("SIGnal:5V:VOLTage?");
        if (this.responseOkay(cmd)) {
            try {
                this.v5Voltage = Integer.parseInt(((String)cmd.response.get(0)).replaceAll("[^\\d.]", ""));
            }
            catch (NumberFormatException e) {
                return -1;
            }
            return this.v5Voltage;
        }
        return -1;
    }

    @Override
    public boolean setV12VoltageSetting(int v) {
        CmdStruct cmd = this.sendCmd("SIGnal:12V:VOLTage " + new Integer(v).toString());
        return this.responseOkay(cmd);
    }

    @Override
    public int getV12VoltageSetting() {
        CmdStruct cmd = this.sendCmd("SIGnal:12V:VOLTage?");
        if (this.responseOkay(cmd)) {
            try {
                this.v12Voltage = Integer.parseInt(((String)cmd.response.get(0)).replaceAll("[^\\d.]", ""));
            }
            catch (NumberFormatException e) {
                return -1;
            }
            return this.v12Voltage;
        }
        return -1;
    }

    @Override
    public boolean setTrigger(EnumPPMTrigger thisTrigger) {
        CmdStruct cmd;
        switch (thisTrigger) {
            case MANUAL: {
                cmd = this.sendCmd("RECord:TRIGger:MODE MANUAL");
                break;
            }
            case POWER: {
                cmd = this.sendCmd("RECord:TRIGger:MODE POWER");
                break;
            }
            case PATTERN: {
                cmd = this.sendCmd("RECord:TRIGger:MODE PATTERN");
                break;
            }
            case EXTERNAL: {
                cmd = this.sendCmd("RECord:TRIGger:MODE EXTERNAL");
                break;
            }
            case THRESHOLD: {
                cmd = this.sendCmd("RECord:TRIGger:MODE THRESHOLD");
                break;
            }
            default: {
                return false;
            }
        }
        return this.responseOkay(cmd);
    }

    @Override
    public boolean startRecord() {
        CmdStruct cmd = this.sendCmd("RECord RUN");
        return this.responseOkay(cmd);
    }

    @Override
    public boolean stopRecord() {
        CmdStruct cmd = this.sendCmd("RECord STOP");
        return this.responseOkay(cmd);
    }

    @Override
    public RecordedDataSet getRecordedData() {
        return null;
    }

    protected void clearStreamExceptionString() {
        this.streamExceptionString = "";
    }

    protected String setStreamExceptionString(String text) {
        this.streamExceptionString = text;
        return this.streamExceptionString;
    }

    protected String getStreamExceptionString() {
        return this.streamExceptionString;
    }

    protected String buildExceptionOutOfBufferSpace() {
        return this.setStreamExceptionString("Out of Buffer Space");
    }

    protected void streamStoppedByStatusCode(StreamStatusCodes sCode) {
        this.terminalException = false;
        this.streamStatusCode = sCode;
        this.disableStreamReceive();
        this.myDle.deviceInfo.cmdLock.release();
        this.isStreamRunning = false;
    }

    protected boolean modePowerCmd(String sCmd, CmdStruct cmd) {
        boolean retVal = true;
        if (sCmd.toLowerCase().startsWith("stream mode power total")) {
            this.getBasePPM().setEnableTotalPowerCalc(true);
            this.getBasePPM().setEnablePowerCalc(true);
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power enable")) {
            this.getBasePPM().setEnableTotalPowerCalc(false);
            this.getBasePPM().setEnablePowerCalc(true);
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power disable")) {
            this.getBasePPM().setEnableTotalPowerCalc(false);
            this.getBasePPM().setEnablePowerCalc(false);
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power?")) {
            if (this.getBasePPM().isEnablePowerCalc()) {
                if (this.getBasePPM().isEnableTotalPowerCalc()) {
                    cmd.response.add("Total");
                } else {
                    cmd.response.add("Enabled");
                }
            } else {
                cmd.response.add("Disabled");
            }
            cmd.response.add(">");
            cmd.action = 333;
        } else {
            retVal = false;
        }
        return retVal;
    }

    protected void initStreamExceptions() {
        this.terminalException = false;
        this.stopSentCount = 0;
        this.clearStreamExceptionString();
    }

    protected boolean needStopStreamCmd() {
        return this.terminalException && this.stopSentCount == 0;
    }

    protected void initDebugTrackers() {
        this.totalPacketDataLen = 0L;
        this.absolutePacketDatacount = 0L;
        this.repeatPacketDatacount = 0L;
        this.deltaPacketDatacount = 0L;
    }

    protected String buildExceptionCorruptHeaderString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Corrupt Header Received ");
        this.setStreamExceptionString(sb.toString());
        for (byte b : data) {
            sb.append(Byte.toUnsignedInt(b));
            sb.append(",");
        }
        return sb.toString();
    }

    protected String buildExceptionAllGroupsAreEmptyString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("All Groups Are Empty ");
        this.setStreamExceptionString(sb.toString());
        for (byte b : data) {
            sb.append(Byte.toUnsignedInt(b));
            sb.append(",");
        }
        return sb.toString();
    }

    protected String buildExceptionCorruptDataString(byte[] data, int lastGoodIndex, int dataIdx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Corrupt Block Received ");
        this.setStreamExceptionString(sb.toString());
        sb.append("\nLastGoodIndex: " + lastGoodIndex + " Error Index: " + dataIdx + "\n");
        for (byte b : data) {
            sb.append(Byte.toUnsignedInt(b));
            sb.append(",");
        }
        return sb.toString();
    }

    public BasePPM getBasePPM() {
        return this.basePPM;
    }

    public class BasePPM {
        private boolean enablePowerCalc = false;
        private boolean enableTotalPowerCalc = false;
        private PPMHeaderVersion headerVersion = PPMHeaderVersion.V3;
        public TotalPowerData totalPowerData = null;
        public PowerData v5Power = null;
        public PowerData v12Power = null;
        public int have5vV = -1;
        public int have5vA = -1;
        public int have12vV = -1;
        public int have12vA = -1;
        private int devicePerioduS;
        public String voltUnits = "";
        public String currentUnits = "";
        public String powerUnits = "";
        public static final String requestedResampleOffString = "off";
        public static final String requestedResampleOnString = "on";
        public String mainPeriodStr = "";
        final int[] defaultAverageArray = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};
        final long[] defaultSampleTime_ns = new long[]{4000L};
        ResampleTable resampleTable = null;
        private final BaseStreamDevicePPM parent;

        public BasePPM(BaseStreamDevicePPM baseStreamDevicePPM) {
            this.parent = baseStreamDevicePPM;
            this.resampleTable = new ResampleTable(null, null, this.defaultAverageArray, this.defaultSampleTime_ns);
        }

        public String getRequestedResample() {
            return this.resampleTable.table.get((Object)"").requestedResampleStr;
        }

        public void getRequestedResampleDetails(List<String> response) {
            ArrayList<String> groupReplys = new ArrayList<String>();
            String outerReply = "";
            for (String key : this.resampleTable.table.keySet()) {
                if (key.isEmpty()) {
                    outerReply = this.resampleTable.table.get((Object)key).requestedResampleStr;
                    continue;
                }
                String str = this.resampleTable.table.get((Object)key).requestedResampleStr;
                groupReplys.add("Group: " + key + " " + str);
            }
            response.add(outerReply);
            response.addAll(groupReplys);
        }

        /*
         * WARNING - void declaration
         */
        public void getRequestedResample(List<String> response) {
            void var5_10;
            ArrayList<String> groupReplys = new ArrayList<String>();
            String outerReply = "";
            for (String string : this.resampleTable.table.keySet()) {
                if (string.isEmpty()) {
                    outerReply = this.resampleTable.table.get((Object)string).requestedResampleStr;
                    continue;
                }
                String str = this.resampleTable.table.get((Object)string).requestedResampleStr;
                groupReplys.add("Group: " + string + " " + str);
            }
            int offCount = 0;
            for (String gReply : groupReplys) {
                if (!gReply.endsWith(requestedResampleOffString)) continue;
                ++offCount;
            }
            boolean bl = false;
            if (!outerReply.endsWith(requestedResampleOffString) && offCount != 0) {
                void var5_9;
                response.add(outerReply);
                ++var5_9;
            }
            for (String gReply : groupReplys) {
                if (gReply.endsWith(requestedResampleOffString)) continue;
                response.add(gReply);
                ++var5_10;
            }
            if (var5_10 == false) {
                response.add(outerReply);
            }
        }

        public void setRequestedResampleStr(String requestedResample) {
            if (requestedResample.toLowerCase().equals(requestedResampleOffString)) {
                this.setRequestedResampleuS(0);
            }
            this.resampleTable.table.get((Object)"").requestedResampleStr = requestedResample;
        }

        public void setRequestedResampleOff(String gStr) {
            if (gStr == "") {
                this.resampleTable.setAllOff();
            } else {
                ResampleRecord rr = this.resampleTable.table.get(gStr);
                rr.setOff();
            }
        }

        @Deprecated
        public void setRequestedResampleStr(String gStr, String requestedResample) {
            if (gStr == "") {
                this.setRequestedResampleStr(requestedResample);
                if (requestedResample.toLowerCase().equals(requestedResampleOffString)) {
                    ArrayList<String> safeRemove = new ArrayList<String>();
                    for (String key : this.resampleTable.table.keySet()) {
                        if (key == "") continue;
                        safeRemove.add(key);
                    }
                    for (String key : safeRemove) {
                        this.resampleTable.table.remove(key);
                    }
                }
            } else if (requestedResample.toLowerCase().equals(requestedResampleOffString)) {
                this.resampleTable.table.remove(gStr);
                if (this.resampleTable.table.size() == 1) {
                    this.setRequestedResampleStr(requestedResample);
                }
            }
        }

        public void setRequestedResample(int requestedResample) {
            this.setRequestedResampleuS(requestedResample);
            this.resampleTable.table.get((Object)"").requestedResampleStr = Integer.toString(this.getRequestedResampleuS()) + "us";
        }

        public boolean isResamplingActive() {
            return !this.getRequestedResample().equals(requestedResampleOffString);
        }

        public int getActiveSampleTimeUS() {
            if (BaseStreamDevicePPM.this.getBasePPM().isResamplingActive()) {
                return BaseStreamDevicePPM.this.getBasePPM().getRequestedResampleuS();
            }
            return BaseStreamDevicePPM.this.getBasePPM().getDevicePerioduS();
        }

        public void sanityCheckResample() {
            if (this.isResamplingActive() && this.getRequestedResampleuS() < this.getDevicePerioduS()) {
                this.setRequestedResample(this.getDevicePerioduS());
            }
        }

        private int getMultiplier_uS(String nStr) {
            if (nStr.toLowerCase().endsWith("us")) {
                return 1;
            }
            if (nStr.toLowerCase().endsWith("ms")) {
                return 1000;
            }
            return -1;
        }

        public boolean setValidatedRequestedResample(String nStr) {
            String str = this.setValidatedRequestedResample("", nStr);
            return str == null;
        }

        private String badValueString(String value) {
            return "Bad Value (" + value + ")";
        }

        public String setValidatedRequestedResample(String groupStr, String nStr) {
            ResampleRecord rr;
            int value;
            int multiplier;
            BaseStreamDevicePPM parentRef = this.parent;
            XMLDeviceDefinition xml = null;
            int[] averageArray = null;
            long[] groupSampleTimes_ns = null;
            QuarchDeviceInfo deviceInfo = this.getParent().myDle.deviceInfo;
            if (parentRef != null && parentRef.xmlREF != null) {
                xml = parentRef.xmlREF;
                averageArray = xml.getAverageArray();
                if (xml.hasFixtureSupport()) {
                    if ((groupSampleTimes_ns = this.processFixture(groupSampleTimes_ns, deviceInfo)) == null) {
                        return "Fixture Not Connected";
                    }
                } else {
                    return "Unknown Device";
                }
            }
            if (xml == null || averageArray == null) {
                this.resampleTable = new ResampleTable(deviceInfo.getqDevice().getDetailsAsMap(), null, this.defaultAverageArray, this.defaultSampleTime_ns);
                averageArray = this.resampleTable.averageArray;
                groupSampleTimes_ns = this.resampleTable.sampleTimes_ns;
            }
            if ((multiplier = this.getMultiplier_uS(nStr = nStr.trim())) < 1) {
                return this.badValueString(nStr);
            }
            try {
                value = Integer.valueOf(nStr.substring(0, nStr.length() - 2));
            }
            catch (NumberFormatException e) {
                return this.badValueString(nStr);
            }
            if ((double)value * (double)multiplier > 2.147483647E9) {
                return this.badValueString(nStr);
            }
            if (!groupStr.isEmpty()) {
                boolean error = false;
                try {
                    int intGroup = Integer.valueOf(groupStr);
                    if (intGroup >= groupSampleTimes_ns.length) {
                        error = true;
                    }
                }
                catch (Exception e) {
                    error = true;
                }
                if (error) {
                    return this.badValueString("group " + groupStr);
                }
            }
            if ((rr = this.resampleTable.table.get(groupStr)) == null) {
                rr = new ResampleRecord(groupStr, true);
                this.resampleTable.table.put(groupStr, rr);
            }
            if (value == 0) {
                this.setRequestedResampleStr(groupStr, requestedResampleOffString);
            } else {
                if (rr.groupId == -1) {
                    this.resampleTable.setAllOff();
                }
                rr.requestedResampleStr = nStr;
                rr.requestedResampleuS = value * multiplier;
                if (rr.groupId == -1) {
                    rr.createDeviceAverageCommand(averageArray, groupSampleTimes_ns[0]);
                } else {
                    rr.createDeviceAverageCommand(averageArray, groupSampleTimes_ns[rr.groupId]);
                }
            }
            this.sendDeviceAverages(this.resampleTable);
            return null;
        }

        public long[] processFixture(long[] groupSampleTimes_ns, QuarchDeviceInfo deviceInfo) {
            CmdStruct cmd = new CmdStruct();
            cmd.command = "fix:chan:xml?";
            deviceInfo.executeCommand(cmd);
            if (cmd.response.size() > 3) {
                cmd.response.remove(cmd.response.size() - 1);
                XMLFixture xmlFix = XMLFixture.createFromXML(String.join((CharSequence)"/r/n", cmd.response));
                if (xmlFix != null) {
                    groupSampleTimes_ns = xmlFix.getGroupSampleTimes_ns();
                    ResampleTable.access$202(this.resampleTable, groupSampleTimes_ns);
                }
            }
            cmd.command = "fix:idn?";
            deviceInfo.executeCommand(cmd);
            if (cmd.response.size() > 3) {
                if (this.resampleTable != null) {
                    if (this.resampleTable.getFixtureIdnInfo() == null) {
                        this.resampleTable.setFixtureIdnInfo(cmd.response);
                    } else if (this.resampleTable.hasFixtureChanged(cmd.response)) {
                        // empty if block
                    }
                }
            } else {
                return null;
            }
            return groupSampleTimes_ns;
        }

        private boolean sendDeviceAverages(ResampleTable rTable) {
            for (String key : rTable.table.keySet()) {
                ResampleRecord rr = rTable.table.get(key);
                if (rr == null || rr.requestedResampleStr.equals(requestedResampleOffString)) continue;
                CmdStruct cmd = new CmdStruct();
                cmd.command = rr.deviceAverageCommand;
                this.getParent().myDle.deviceInfo.executeCommand(cmd);
            }
            return true;
        }

        public int getRequestedResampleuS() {
            return this.resampleTable.table.get((Object)"").requestedResampleuS;
        }

        public void setRequestedResampleuS(int requestedResampleuS) {
            this.resampleTable.table.get((Object)"").requestedResampleuS = requestedResampleuS;
        }

        public void getLegacyHeaderStrings(ArrayList<String> headerStrs, int version, int format, int average) {
            headerStrs.add("Version: " + Integer.toString(version));
            headerStrs.add("Format: " + Integer.toString(format));
            headerStrs.add("Average: " + Integer.toString(average));
        }

        public void getExtendedHeaderStrings(ArrayList<String> headerStrs, int version, int format, int average) {
            if (this.getHeaderVersion() == PPMHeaderVersion.V2) {
                this.getV2HeaderStrings(headerStrs);
            } else if (this.getHeaderVersion() == PPMHeaderVersion.V3) {
                this.getV3HeaderStrings(headerStrs, version, format, average);
            }
        }

        public void addChannelStrings(ArrayList<String> headerStrs, String name, String group, String units, long maxValue, int dataPosition) {
            headerStrs.add("<channel>");
            headerStrs.add("<name>" + name + "</name>");
            headerStrs.add("<group>" + group + "</group>");
            headerStrs.add("<units>" + units + "</units>");
            headerStrs.add("<maxTValue>" + maxValue + "</maxTValue>");
            headerStrs.add("<dataPosition>" + Integer.toString(dataPosition) + "</dataPosition>");
            headerStrs.add("</channel>");
        }

        private void getV3HeaderStrings(ArrayList<String> headerStrs, int version, int format, int average) {
            headerStrs.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            headerStrs.add("<header>");
            headerStrs.add("<version>V3</version>");
            String mainPeriodStr = this.isResamplingActive() ? Integer.toString(this.getRequestedResampleuS()) + "us" : Integer.toString(this.getDevicePerioduS()) + "us";
            headerStrs.add("<devicePeriod>" + Integer.toString(this.getDevicePerioduS()) + "us</devicePeriod>");
            headerStrs.add("<mainPeriod>" + mainPeriodStr + "</mainPeriod>");
            headerStrs.add("<legacyVersion>" + Integer.toString(version) + "</legacyVersion>");
            headerStrs.add("<legacyFormat>" + Integer.toString(format) + "</legacyFormat>");
            headerStrs.add("<legacyAverage>" + Integer.toString(average) + "</legacyAverage>");
            headerStrs.add("<channels>");
            this.addChannelStrings(headerStrs, "Status", "status", "NA", 0L, 0);
            String auxChanName = this.getAuxChannelName();
            int dataPosition = 1;
            String namePrefix = this.getParent().getExtendedChannelNamePrefix();
            if (this.have5vV >= 0) {
                this.addChannelStrings(headerStrs, namePrefix + auxChanName, "voltage", this.voltUnits, BaseStreamDevicePPM.this.max5vVoltageValue, dataPosition++);
            }
            if (this.have5vA >= 0) {
                this.addChannelStrings(headerStrs, namePrefix + auxChanName, "current", this.currentUnits, BaseStreamDevicePPM.this.max5vCurrentValue, dataPosition++);
            }
            if (this.have12vV >= 0) {
                this.addChannelStrings(headerStrs, namePrefix + "12V", "voltage", this.voltUnits, BaseStreamDevicePPM.this.max12vVoltageValue, dataPosition++);
            }
            if (this.have12vA >= 0) {
                this.addChannelStrings(headerStrs, namePrefix + "12V", "current", this.currentUnits, BaseStreamDevicePPM.this.max12vCurrentValue, dataPosition++);
            }
            if (this.v5Power != null) {
                this.addChannelStrings(headerStrs, namePrefix + auxChanName, "power", this.powerUnits, BaseStreamDevicePPM.this.max5vPowerValue, dataPosition++);
            }
            if (this.v12Power != null) {
                this.addChannelStrings(headerStrs, namePrefix + "12V", "power", this.powerUnits, BaseStreamDevicePPM.this.max12vPowerValue, dataPosition++);
            }
            if (this.totalPowerData != null) {
                this.addChannelStrings(headerStrs, namePrefix + "Tot", "power", this.powerUnits, BaseStreamDevicePPM.this.maxTotalPowerValue, dataPosition++);
            }
            headerStrs.add("</channels>");
            headerStrs.add("</header>");
        }

        private String getAuxChannelName() {
            return BaseStreamDevicePPM.this.getDeviceOptions().containsKey(BaseStreamDevicePPM.CONF_OUT_MODE) && BaseStreamDevicePPM.this.getDeviceOptions().get(BaseStreamDevicePPM.CONF_OUT_MODE) != null ? BaseStreamDevicePPM.this.getDeviceOptions().get(BaseStreamDevicePPM.CONF_OUT_MODE) : "5V";
        }

        private void getV2HeaderStrings(ArrayList<String> headerStrs) {
            headerStrs.add("V2");
            headerStrs.add("@Channels");
            headerStrs.add("Status status NA");
            String auxChanName = this.getAuxChannelName();
            if (this.have5vV >= 0) {
                headerStrs.add(auxChanName + " voltage " + this.voltUnits);
            }
            if (this.have5vA >= 0) {
                headerStrs.add(auxChanName + " current " + this.currentUnits);
            }
            if (this.have12vV >= 0) {
                headerStrs.add("12V voltage " + this.voltUnits);
            }
            if (this.have12vA >= 0) {
                headerStrs.add("12V current " + this.currentUnits);
            }
            if (this.v5Power != null) {
                headerStrs.add(auxChanName + " power " + this.powerUnits);
            }
            if (this.v12Power != null) {
                headerStrs.add("12V power " + this.powerUnits);
            }
            if (this.totalPowerData != null) {
                headerStrs.add("Tot power " + this.powerUnits);
            }
            headerStrs.add("@Channels_End");
        }

        public boolean isEnablePowerCalc() {
            return this.enablePowerCalc;
        }

        public void setEnablePowerCalc(boolean enablePowerCalc) {
            this.enablePowerCalc = enablePowerCalc;
        }

        public boolean isEnableTotalPowerCalc() {
            return this.enableTotalPowerCalc;
        }

        public void setEnableTotalPowerCalc(boolean enableTotalPowerCalc) {
            this.enableTotalPowerCalc = enableTotalPowerCalc;
        }

        public void asXMLSampleRateInfo(List<String> retVal) {
            long reqSampleRate_nS = (long)this.getRequestedResampleuS() * 1000L;
            this.asXMLSampleRate(retVal, reqSampleRate_nS);
        }

        public void asXMLSampleRateInfo(String groupIdStr, List<String> retVal) {
            long reqSampleRate_nS = this.getRequestedResampleuS(groupIdStr) * 1000L;
            this.asXMLSampleRate(retVal, reqSampleRate_nS);
        }

        public void asXMLSampleRate(List<String> retVal, long reqSampleRate_nS) {
            long freq;
            int exponent = 0;
            if (1000000000L > reqSampleRate_nS) {
                freq = 1000000000L / reqSampleRate_nS;
            } else {
                do {
                    --exponent;
                } while (1000000000L < (reqSampleRate_nS /= 10L));
                freq = 1000000000L / reqSampleRate_nS;
            }
            retVal.add("<sampleRateBase>" + freq + "</sampleRateBase>");
            retVal.add("<sampleRateExponent>" + exponent + "</sampleRateExponent>");
            retVal.add("<averagingRate>0</averagingRate>");
        }

        public boolean isResamplingActive(String groupIdStr) {
            String resampleString;
            ResampleRecord resampleGroupRecord = this.resampleTable.table.get(groupIdStr);
            if (resampleGroupRecord != null && !(resampleString = resampleGroupRecord.requestedResampleStr.toLowerCase()).equals(requestedResampleOffString)) {
                return true;
            }
            if (this.resampleTable.table.get((Object)"").requestedResampleStr.toLowerCase().equals(requestedResampleOffString)) {
                return false;
            }
            if (this.resampleTable.table.get((Object)"").requestedResampleStr.toLowerCase().equals(requestedResampleOnString)) {
                return resampleGroupRecord != null;
            }
            return true;
        }

        public long getRequestedResampleuS(String groupIdStr) {
            String resampleString = this.resampleTable.table.get((Object)groupIdStr).requestedResampleStr.toLowerCase();
            if (!resampleString.equals(requestedResampleOffString)) {
                return this.resampleTable.table.get((Object)groupIdStr).requestedResampleuS;
            }
            if (!this.resampleTable.table.get((Object)"").requestedResampleStr.toLowerCase().equals(requestedResampleOnString)) {
                return this.getRequestedResampleuS();
            }
            return -1L;
        }

        public void setRequestedResample(String groupIdStr, int sampleTime_uS) {
            String requestedResampleStr = Integer.toString(sampleTime_uS) + "us";
            this.setValidatedRequestedResample(groupIdStr, requestedResampleStr);
        }

        public BaseStreamDevicePPM getParent() {
            return this.parent;
        }

        public int getDevicePerioduS() {
            return this.devicePerioduS;
        }

        public void setDevicePerioduS(int devicePerioduS) {
            this.devicePerioduS = devicePerioduS;
        }

        public PPMHeaderVersion getHeaderVersion() {
            return this.headerVersion;
        }

        public void setHeaderVersion(PPMHeaderVersion headerVersion) {
            this.headerVersion = headerVersion;
        }

        class ResampleTable {
            protected Map<String, ResampleRecord> table = new LinkedHashMap<String, ResampleRecord>();
            private Map<String, String> deviceIdnInfo = null;
            private List<String> fixtureIdnInfo = null;
            private int[] averageArray;
            private long[] sampleTimes_ns;

            public ResampleTable(Map<String, String> deviceIdnInfo, List<String> fixtureIdnInfo, int[] averageArray, long[] sampleTimes_ns) {
                this.deviceIdnInfo = deviceIdnInfo;
                this.fixtureIdnInfo = fixtureIdnInfo;
                this.averageArray = averageArray;
                this.sampleTimes_ns = sampleTimes_ns;
                this.initResampleTable(sampleTimes_ns);
            }

            public void initResampleTable(long[] sampleTimes_ns) {
                ResampleRecord firstResampleRecord = new ResampleRecord();
                firstResampleRecord.isHidden = false;
                this.table.put("", firstResampleRecord);
                if (sampleTimes_ns != null) {
                    for (int i = 0; i < sampleTimes_ns.length; ++i) {
                        ResampleRecord rr = new ResampleRecord(Integer.toString(i), true);
                        this.table.put(rr.groupStr, rr);
                        rr.baseSampleTime_ns = sampleTimes_ns[i];
                    }
                }
            }

            private boolean idnFieldMatches(String fieldName, Map<String, String> dev1IDN, Map<String, String> dev2IDN) {
                String dev1Str = null;
                String dev2Str = null;
                if (dev1IDN == null || dev2IDN == null) {
                    return false;
                }
                if (dev1IDN.containsKey(fieldName)) {
                    dev1Str = dev1IDN.get(fieldName);
                }
                if (dev2IDN.containsKey(fieldName)) {
                    dev2Str = dev2IDN.get(fieldName);
                }
                if (dev1Str == null || dev2Str == null) {
                    return false;
                }
                return dev1Str.equals(dev2Str);
            }

            public boolean hasDeviceChanged(Map<String, String> dutDeviceIdnInfo) {
                if (this.deviceIdnInfo == null) {
                    return dutDeviceIdnInfo != null;
                }
                return !this.idnFieldMatches("Serial#", this.deviceIdnInfo, dutDeviceIdnInfo);
            }

            public boolean hasFixtureChanged(List<String> dutFixtureIdnInfo) {
                if (this.fixtureIdnInfo == null) {
                    return dutFixtureIdnInfo != null;
                }
                return !this.fixtureIdnInfo.containsAll(dutFixtureIdnInfo);
            }

            public List<String> getFixtureIdnInfo() {
                return this.fixtureIdnInfo;
            }

            public void setFixtureIdnInfo(List<String> response) {
                this.fixtureIdnInfo = response;
                this.initResampleTable(this.sampleTimes_ns);
            }

            public void setAllOff() {
                for (String rrKey : this.table.keySet()) {
                    ResampleRecord rr = this.table.get(rrKey);
                    rr.setOff();
                }
            }

            static /* synthetic */ long[] access$202(ResampleTable x0, long[] x1) {
                x0.sampleTimes_ns = x1;
                return x1;
            }
        }

        class ResampleRecord {
            protected String requestedResampleStr = "off";
            protected int requestedResampleuS = 0;
            protected String deviceAverageCommand = "";
            protected String deviceAverageCommandGroupPrefix = "";
            private String groupStr = "";
            protected int groupId = -1;
            protected long baseSampleTime_ns = -1L;
            protected boolean isHidden = true;

            public ResampleRecord() {
            }

            public ResampleRecord(String _groupStr, boolean hidden) {
                this.setGroupStr(_groupStr);
                this.isHidden = hidden;
            }

            public void createDeviceAverageCommand(int[] averageArray, long groupSampleTimes_ns) {
                int divisor = 1000;
                for (int i = averageArray.length - 1; i >= 0; --i) {
                    int deviceAvg = averageArray[i];
                    long deviceSampleTime = (long)deviceAvg * groupSampleTimes_ns;
                    if ((long)this.requestedResampleuS < (deviceSampleTime /= 1000L)) continue;
                    String avgStr = deviceAvg >= 1024 ? Integer.toString(deviceAvg / 1024) + "k" : Integer.toString(deviceAvg);
                    this.deviceAverageCommand = "rec:ave " + this.deviceAverageCommandGroupPrefix + avgStr;
                    break;
                }
            }

            protected String getGroupStr() {
                return this.groupStr;
            }

            protected void setGroupStr(String groupStr) {
                this.groupStr = groupStr;
                if (groupStr.isEmpty()) {
                    this.groupId = -1;
                    this.deviceAverageCommandGroupPrefix = "";
                } else {
                    this.groupId = Integer.parseInt(groupStr);
                    this.deviceAverageCommandGroupPrefix = "group " + groupStr + " ";
                }
            }

            public void setOff() {
                this.requestedResampleStr = BasePPM.requestedResampleOffString;
                this.deviceAverageCommand = "";
            }
        }
    }

    public class TotalPowerData {
        final int[] srcIdxs;
        final int dstIdx;

        public TotalPowerData(int[] srcIdxs, int dstIdx) {
            this.srcIdxs = srcIdxs;
            this.dstIdx = dstIdx;
        }

        public void calcPower(int[] stripe, double multiplier) {
            double tmp = 0.0;
            for (int x : this.srcIdxs) {
                tmp += (double)stripe[x];
            }
            stripe[this.dstIdx] = (int)(tmp *= multiplier);
        }
    }

    public class PowerData {
        public int mVIdx;
        public int mAIdx;
        public int uWIdx;

        public PowerData(int mVIdx, int mAIdx, int uWIdx) {
            this.mVIdx = mVIdx;
            this.mAIdx = mAIdx;
            this.uWIdx = uWIdx;
        }

        public void calcPower(int[] stripe, double multiplier) {
            double tmp = stripe[this.mVIdx];
            tmp = tmp * (double)stripe[this.mAIdx] * multiplier;
            stripe[this.uWIdx] = (int)tmp;
        }
    }

    public static enum PPMHeaderVersion {
        V1("V1"),
        V2("V2"),
        V3("V3");

        private final String code;

        private PPMHeaderVersion(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }

        public static PPMHeaderVersion getByCode(String str) {
            for (PPMHeaderVersion e : PPMHeaderVersion.values()) {
                if (!e.code.equals(str)) continue;
                return e;
            }
            return null;
        }

        public String toString() {
            return this.code;
        }
    }

    public static enum StreamState {
        waitHeader,
        waitData,
        haveStatus;

    }
}

