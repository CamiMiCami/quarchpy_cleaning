/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.util.ArrayList;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beRecordedData.RecordedDataSet;
import src.com.quarch.beStream.StreamBase;
import src.com.quarch.beStream.StreamBuffer;
import src.com.quarch.beStream.StreamStatusCodes;
import src.com.quarch.devicePPM.EnumPPMAverage;
import src.com.quarch.devicePPM.EnumPPMTrigger;
import src.com.quarch.devices.BaseDevice;

public class BaseStreamDevice
extends BaseDevice {
    public static final String CONF_OUT_MODE = "Conf:Out:Mode";
    protected StreamBase streamBase;
    protected Thread steamThread;
    protected StreamState streamState;
    protected StreamBuffer streamBuffer;
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
    protected BasePPM basePPM = new BasePPM();

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

    protected boolean modePowerCmd(String sCmd, CmdStruct cmd) {
        boolean retVal = true;
        if (sCmd.toLowerCase().startsWith("stream mode power total")) {
            this.basePPM.enableTotalPowerCalc = true;
            this.basePPM.enablePowerCalc = true;
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power enable")) {
            this.basePPM.enableTotalPowerCalc = false;
            this.basePPM.enablePowerCalc = true;
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power disable")) {
            this.basePPM.enableTotalPowerCalc = false;
            this.basePPM.enablePowerCalc = false;
            cmd.response.add("OK");
            cmd.response.add(">");
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream mode power?")) {
            if (this.basePPM.enablePowerCalc) {
                if (this.basePPM.enableTotalPowerCalc) {
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

    public class BasePPM {
        public boolean enablePowerCalc = false;
        public boolean enableTotalPowerCalc = false;
        public PPMHeaderVersion headerVersion = PPMHeaderVersion.V1;
        public TotalPowerData totalPowerData = null;
        public PowerData v5Power = null;
        public PowerData v12Power = null;
        public int have5vV = -1;
        public int have5vA = -1;
        public int have12vV = -1;
        public int have12vA = -1;
        public int devicePerioduS;
        public String voltUnits = "";
        public String currentUnits = "";
        public String powerUnits = "";
        public static final String requestedResampleOffString = "off";
        private String requestedResample = "off";
        public int requestedResampleuS = 0;
        public String requestedTimebase = "";
        public String mainPeriodStr = "";

        public String getRequestedResample() {
            return this.requestedResample;
        }

        public void setRequestedResample(String requestedResample) {
            if (requestedResample.toLowerCase().equals(requestedResampleOffString)) {
                this.requestedResampleuS = 0;
            }
            this.requestedResample = requestedResample;
        }

        public void setRequestedResample(int requestedResample) {
            this.requestedResampleuS = requestedResample;
            this.requestedResample = Integer.toString(this.requestedResampleuS) + "us";
        }

        public boolean isResamplingActive() {
            return !this.getRequestedResample().equals(requestedResampleOffString);
        }

        public void sanityCheckResample() {
            if (this.isResamplingActive() && this.requestedResampleuS < this.devicePerioduS) {
                this.setRequestedResample(this.devicePerioduS);
            }
        }

        public boolean setValidatedRequestedResample(String nStr) {
            int value;
            int multiplier = -1;
            if ((nStr = nStr.trim()).toLowerCase().endsWith("us")) {
                multiplier = 1;
            } else if (nStr.toLowerCase().endsWith("ms")) {
                multiplier = 1000;
            }
            if (multiplier < 1) {
                return false;
            }
            try {
                value = Integer.valueOf(nStr.substring(0, nStr.length() - 2));
            }
            catch (NumberFormatException e) {
                return false;
            }
            if ((double)value * (double)multiplier > 2.147483647E9) {
                return false;
            }
            if (value == 0) {
                this.setRequestedResample(requestedResampleOffString);
            } else {
                this.setRequestedResample(nStr);
            }
            this.requestedResampleuS = value * multiplier;
            return true;
        }

        public void getLegacyHeaderStrings(ArrayList<String> headerStrs, int version, int format, int average) {
            headerStrs.add("Version: " + Integer.toString(version));
            headerStrs.add("Format: " + Integer.toString(format));
            headerStrs.add("Average: " + Integer.toString(average));
        }

        public void getExtendedHeaderStrings(ArrayList<String> headerStrs, int version, int format, int average) {
            if (this.headerVersion == PPMHeaderVersion.V2) {
                this.getV2HeaderStrings(headerStrs);
            } else if (this.headerVersion == PPMHeaderVersion.V3) {
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
            String mainPeriodStr = this.isResamplingActive() ? Integer.toString(this.requestedResampleuS) + "us" : Integer.toString(this.devicePerioduS) + "us";
            headerStrs.add("<devicePerioduS>" + Integer.toString(this.devicePerioduS) + "us</devicePerioduS>");
            headerStrs.add("<mainPeriod>" + mainPeriodStr + "</mainPeriod>");
            headerStrs.add("<legacyVersion>" + Integer.toString(version) + "</legacyVersion>");
            headerStrs.add("<legacyFormat>" + Integer.toString(format) + "</legacyFormat>");
            headerStrs.add("<legacyAverage>" + Integer.toString(average) + "</legacyAverage>");
            headerStrs.add("<channels>");
            this.addChannelStrings(headerStrs, "Status", "status", "NA", 0L, 0);
            String auxChanName = this.getAuxChannelName();
            int dataPosition = 1;
            if (this.have5vV >= 0) {
                this.addChannelStrings(headerStrs, auxChanName, "voltage", this.voltUnits, BaseStreamDevice.this.max5vVoltageValue, dataPosition++);
            }
            if (this.have5vA >= 0) {
                this.addChannelStrings(headerStrs, auxChanName, "current", this.currentUnits, BaseStreamDevice.this.max5vCurrentValue, dataPosition++);
            }
            if (this.have12vV >= 0) {
                this.addChannelStrings(headerStrs, "12V", "voltage", this.voltUnits, BaseStreamDevice.this.max12vVoltageValue, dataPosition++);
            }
            if (this.have12vA >= 0) {
                this.addChannelStrings(headerStrs, "12V", "current", this.currentUnits, BaseStreamDevice.this.max12vCurrentValue, dataPosition++);
            }
            if (this.v5Power != null) {
                this.addChannelStrings(headerStrs, auxChanName, "power", this.powerUnits, BaseStreamDevice.this.max5vPowerValue, dataPosition++);
            }
            if (this.v12Power != null) {
                this.addChannelStrings(headerStrs, "12V", "power", this.powerUnits, BaseStreamDevice.this.max12vPowerValue, dataPosition++);
            }
            if (this.totalPowerData != null) {
                this.addChannelStrings(headerStrs, "Tot", "power", this.powerUnits, BaseStreamDevice.this.maxTotalPowerValue, dataPosition++);
            }
            headerStrs.add("</channels>");
            headerStrs.add("</header>");
        }

        private String getAuxChannelName() {
            return BaseStreamDevice.this.getDeviceOptions().containsKey(BaseStreamDevice.CONF_OUT_MODE) && BaseStreamDevice.this.getDeviceOptions().get(BaseStreamDevice.CONF_OUT_MODE) != null ? BaseStreamDevice.this.getDeviceOptions().get(BaseStreamDevice.CONF_OUT_MODE) : "5V";
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

