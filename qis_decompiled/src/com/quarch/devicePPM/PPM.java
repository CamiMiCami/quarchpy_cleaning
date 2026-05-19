/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devicePPM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beStream.StreamBufferStriped;
import src.com.quarch.beStream.StreamDataReadyListener;
import src.com.quarch.beStream.StreamEventListener;
import src.com.quarch.beStream.StreamFactory;
import src.com.quarch.beStream.StreamStatusCodes;
import src.com.quarch.beStreamData.StreamRawData;
import src.com.quarch.deviceCalibrationData.PPMCalibrationRecordData;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.BaseStreamDevicePPM;
import src.com.quarch.devices.DeviceDataBuffer;
import src.com.quarch.utils.DebugUtil;

public class PPM
extends BaseStreamDevicePPM
implements StreamEventListener {
    private static final int bihDataHeaderSize = 4;
    private int streamDataIdx;
    private byte[] streamData = null;
    StreamHeader streamHeader;
    private int sliceLength;
    private int decodeBufferFunctionsIdx;
    private List<DecodeBufferFunction> decodeBufferFunctions = new ArrayList<DecodeBufferFunction>();
    private int decodedStripeIdx;
    private int[] decodedStripe;
    private PPMCalibrationRecord[] v5Calibration = new PPMCalibrationRecord[3];
    private PPMCalibrationRecord[] v12Calibration = new PPMCalibrationRecord[3];
    private boolean stripeTrigger = false;
    private boolean syncPacketActive = false;
    private boolean waitPacketAfterSync = false;
    private boolean running = false;
    private long cmdWindowTimer;

    public PPM(DeviceListEntry dle) {
        this.myDle = null;
        this.setNeedFurtherIndetification(false);
        this.setHasREST(false);
        this.setHasSerial(true);
        this.setHasStream(true);
        this.setHasTelnet(false);
        this.setHasUDP(false);
        this.setHasUSB(true);
        this.setVirtualSourceDevice(false);
        dle.deviceInfo.cmdBufferSize = 64;
        dle.deviceInfo.streamBufferSize = 64;
        this.streamStatusCode = StreamStatusCodes.STOPPED;
        this.streamBuffer = new StreamBufferStriped();
    }

    @Override
    public void enableStreamReceive(DeviceListEntry dle) {
        boolean startOkay = false;
        this.myDle = dle;
        if (this.myDle.deviceInfo.openForStream()) {
            try {
                this.myDle.deviceInfo.cmdLock.acquireUninterruptibly();
                this.streamBase = StreamFactory.getInstance().createNewStream(this.myDle);
                this.streamBase.streamEventInitiater.addListener(this);
                this.steamThread = new Thread(this.streamBase);
                this.streamState = BaseStreamDevicePPM.StreamState.waitHeader;
                this.streamBuffer.clear();
                if (this.getVirtualOwner() == null) {
                    this.setGroupOffsetValue(0);
                    this.setChannelNamePrefix("");
                }
                this.streamHeader = null;
                this.steamThread.start();
                this.running = true;
                this.cmdWindowTimer = System.currentTimeMillis();
                startOkay = true;
            }
            finally {
                if (!startOkay) {
                    this.myDle.deviceInfo.closeStream();
                    this.myDle.deviceInfo.cmdLock.release();
                } else {
                    this.streamStatusCode = StreamStatusCodes.RUNNING;
                }
            }
        }
    }

    @Override
    public void disableStreamReceive() {
        this.myDle.deviceInfo.closeStream();
        if (this.streamStatusCode == StreamStatusCodes.RUNNING) {
            this.streamStatusCode = StreamStatusCodes.STOPPED;
        }
        this.streamBase.fireDataReadyEvent(this.streamStatusCode, this.streamBuffer.size());
        this.streamBase.stop();
        this.streamBase = null;
        this.running = false;
    }

    @Override
    public boolean internalCommand(CmdStruct cmd, Object caller) {
        int maxReadStripes = 4096;
        String sCmd = cmd.command.replaceAll("(\\r|\\n)", "");
        if (sCmd.toLowerCase().equals("stream?")) {
            this.cmdStreamQuery(cmd);
        } else if (sCmd.toLowerCase().startsWith("stream text")) {
            this.cmdStreamText(cmd, maxReadStripes);
        } else if (sCmd.toLowerCase().startsWith("stream bin")) {
            this.cmdStreamBin(cmd, maxReadStripes);
        }
        return cmd.action == 333;
    }

    private void cmdStreamQuery(CmdStruct cmd) {
        switch (this.streamStatusCode) {
            case STOPPED: {
                cmd.response.add("Stopped:");
                break;
            }
            case RUNNING: {
                cmd.response.add("Running");
                break;
            }
            case StoppedUser: {
                cmd.response.add("Stopped: User");
                break;
            }
            case StoppedOverrun: {
                cmd.response.add("Stopped: Overrun");
                break;
            }
            case StoppedOverTemp: {
                cmd.response.add("Stopped: Over Temperature");
                break;
            }
            case StoppedCommsFailure: {
                cmd.response.add("Stopped: Comms Failure");
                break;
            }
        }
        if (this.streamBuffer != null) {
            cmd.response.add("Stripes Buffered: " + Integer.toString(this.streamBuffer.size()) + " of " + Integer.toString(this.streamBuffer.getMaxRamStripes()));
        } else {
            cmd.response.add("Stripes Buffered: 0");
        }
        cmd.response.add(">");
        cmd.action = 333;
    }

    public static int[] convertIntegers(List<int[]> dataList) {
        int[] ret = new int[dataList.size()];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = dataList.get(i).length;
        }
        return ret;
    }

    private void intAtoByteA(int[] ints, byte[] bytes, int startIdx) {
        for (int i = 0; i < ints.length; ++i) {
            bytes[startIdx++] = (byte)(ints[i] & 0xFF);
            bytes[startIdx++] = (byte)(ints[i] >> 8 & 0xFF);
            bytes[startIdx++] = (byte)(ints[i] >> 16 & 0xFF);
            bytes[startIdx++] = (byte)(ints[i] >> 24 & 0xFF);
        }
    }

    private void cmdStreamBin(CmdStruct cmd, int maxReadStripes) {
        int maxStripes = -1;
        byte[] dataArray = new byte[(4 + maxReadStripes * 8) * 4];
        int dataArrayIdx = 16;
        int[] firstRecord = new int[4];
        firstRecord[0] = 4;
        if (maxStripes > 0 && maxStripes <= maxReadStripes) {
            StreamRawData rawData;
            for (maxStripes = maxReadStripes; maxStripes > 0 && (rawData = this.streamBuffer.getOldestStripe()) != null; --maxStripes) {
                int n = firstRecord[2];
                firstRecord[2] = n + 1;
                if (n == 0) {
                    firstRecord[3] = rawData.getRecNo();
                }
                int[] raw = rawData.asArrayMinusRecNo();
                firstRecord[1] = raw.length;
                this.intAtoByteA(raw, dataArray, dataArrayIdx);
                dataArrayIdx += raw.length * 4;
            }
            cmd.response.clear();
            this.intAtoByteA(firstRecord, dataArray, 0);
            cmd.bArray = Arrays.copyOf(dataArray, dataArrayIdx);
        } else {
            cmd.response.add("Valid Range: 1 to " + Integer.toString(maxReadStripes));
            cmd.response.add(">");
        }
        cmd.action = 333;
    }

    private void cmdStreamText(CmdStruct cmd, int maxReadStripes) {
        int maxStripes = -1;
        if (cmd.command.substring(12).trim().startsWith("header")) {
            this.getStreamHeader(cmd);
            cmd.response.add(">");
        } else {
            cmd.setDebugFilterLongReply(true);
            if (cmd.command.substring(12).trim().startsWith("all")) {
                maxStripes = maxReadStripes;
            } else {
                try {
                    maxStripes = Integer.parseInt(cmd.command.substring(12).trim());
                }
                catch (NumberFormatException e) {
                    maxStripes = -1;
                }
                catch (StringIndexOutOfBoundsException e) {
                    maxStripes = -1;
                }
            }
            if (maxStripes > 0 && maxStripes <= maxReadStripes) {
                while (maxStripes > 0) {
                    StreamRawData rawData = this.streamBuffer.getOldestStripe();
                    if (rawData == null) {
                        cmd.response.add("eof");
                        break;
                    }
                    cmd.response.add(rawData.asText());
                    --maxStripes;
                }
                cmd.response.add(">");
            } else {
                cmd.response.add("Valid Range: 1 to " + Integer.toString(maxReadStripes));
                cmd.response.add(">");
            }
        }
        cmd.action = 333;
    }

    @Override
    public int getStreamData(byte[] outBuffer, int maxLen) {
        return 0;
    }

    @Override
    public boolean getStreamHeader(CmdStruct cmd) {
        if (this.streamHeader != null && this.streamHeader.isValid) {
            ArrayList<String> sList = this.getStreamHeaderStrings();
            for (String s : sList) {
                cmd.response.add(s);
            }
        } else {
            cmd.response.add("Header Not Available");
        }
        return false;
    }

    @Override
    public ArrayList<String> getStreamHeaderStrings() {
        ArrayList<String> headerStrs = new ArrayList<String>();
        this.getBasePPM().getLegacyHeaderStrings(headerStrs, this.streamHeader.version, this.streamHeader.format, this.streamHeader.average);
        this.getBasePPM().getExtendedHeaderStrings(headerStrs, this.streamHeader.version, this.streamHeader.format, this.streamHeader.average);
        return headerStrs;
    }

    private boolean isHeaderValid() {
        boolean retVal = false;
        if (this.streamData.length > 1 && this.streamData[0] == 3) {
            retVal = true;
        }
        return retVal;
    }

    @Override
    public void streamEvent(DeviceDataBuffer ddBuff) {
        this.streamData = ddBuff.buffer;
        switch (this.streamState) {
            case waitHeader: {
                if (this.isHeaderValid() && this.processHeader()) {
                    this.setDecodeParameters();
                    this.streamState = BaseStreamDevicePPM.StreamState.waitData;
                    this.streamHeader.isValid = true;
                }
                if (this.streamState != BaseStreamDevicePPM.StreamState.waitData || this.streamData.length <= 64) break;
                this.streamData = Arrays.copyOfRange(ddBuff.buffer, 64, ddBuff.buffer.length);
            }
            case waitData: {
                this.processData();
                break;
            }
        }
    }

    private boolean processHeader() {
        if (this.streamData.length >= 64) {
            this.streamHeader = new StreamHeader(this.streamData);
            if (this.streamHeader != null) {
                int forFloatConversion;
                int pos;
                this.streamDataIdx = 4;
                boolean bufPos = false;
                for (pos = 0; pos < 3; ++pos) {
                    this.v12Calibration[pos] = new PPMCalibrationRecord();
                    this.v12Calibration[pos].data.upperLimit = this.decodeHeaderWord();
                    forFloatConversion = this.decodeHeaderDWord();
                    this.v12Calibration[pos].data.offset = Float.intBitsToFloat(forFloatConversion);
                    forFloatConversion = this.decodeHeaderDWord();
                    this.v12Calibration[pos].data.multiplier = Float.intBitsToFloat(forFloatConversion);
                }
                while (pos < 6) {
                    this.v5Calibration[pos - 3] = new PPMCalibrationRecord();
                    this.v5Calibration[pos - 3].data.upperLimit = this.decodeHeaderWord();
                    forFloatConversion = this.decodeHeaderDWord();
                    this.v5Calibration[pos - 3].data.offset = Float.intBitsToFloat(forFloatConversion);
                    forFloatConversion = this.decodeHeaderDWord();
                    this.v5Calibration[pos - 3].data.multiplier = Float.intBitsToFloat(forFloatConversion);
                    ++pos;
                }
            }
        } else {
            this.streamHeader = null;
        }
        return this.streamHeader != null;
    }

    private int decodeStreamWord() {
        int retVal = this.streamData[this.streamDataIdx] & 0x3F;
        retVal <<= 8;
        ++this.streamDataIdx;
        int tmp = this.streamData[this.streamDataIdx] & 0xFF;
        ++this.streamDataIdx;
        return retVal += tmp;
    }

    private int decodeHeaderWord() {
        int retVal = this.streamData[this.streamDataIdx] & 0xFF;
        ++this.streamDataIdx;
        int tmp = this.streamData[this.streamDataIdx] & 0xFF;
        ++this.streamDataIdx;
        return retVal += tmp << 8;
    }

    private int decodeHeaderDWord() {
        int w1 = this.decodeHeaderWord();
        int w2 = this.decodeHeaderWord();
        return w1 + (w2 << 16);
    }

    private int calibrateCurrent(int rawValue, PPMCalibrationRecord[] calibTable) {
        float retVal = 0.0f;
        boolean found = false;
        PPMCalibrationRecord foundCalib = null;
        for (PPMCalibrationRecord calib : calibTable) {
            if (rawValue >= calib.data.upperLimit) continue;
            foundCalib = calib;
            found = true;
            break;
        }
        if (found) {
            retVal = foundCalib.data.offset < (float)rawValue ? (float)rawValue - foundCalib.data.offset : 0.0f;
            retVal *= foundCalib.data.multiplier;
        } else {
            retVal = 0.0f;
        }
        return (int)(retVal * 1000.0f);
    }

    private void testAndSetStripeTrigger() {
        this.stripeTrigger = (this.streamData[this.streamDataIdx + 1] & 0x40) != 0;
    }

    private void decodeVoltageWord() {
        this.testAndSetStripeTrigger();
        this.decodedStripe[this.decodedStripeIdx] = (int)((double)this.decodeStreamWord() * 3.538305666);
        ++this.decodedStripeIdx;
    }

    private void decode5VCurrnet() {
        this.testAndSetStripeTrigger();
        this.decodedStripe[this.decodedStripeIdx] = this.calibrateCurrent(this.decodeStreamWord(), this.v5Calibration);
        ++this.decodedStripeIdx;
    }

    private void decode12VCurrnet() {
        this.testAndSetStripeTrigger();
        this.decodedStripe[this.decodedStripeIdx] = this.calibrateCurrent(this.decodeStreamWord(), this.v12Calibration);
        ++this.decodedStripeIdx;
    }

    private void setDecodeParameters() {
        this.getBasePPM().voltUnits = "mV";
        this.getBasePPM().currentUnits = "mA";
        this.getBasePPM().powerUnits = "uW";
        this.getBasePPM().setDevicePerioduS((int)Math.pow(2.0, this.streamHeader.average) * 4);
        this.getBasePPM().have5vV = -1;
        this.getBasePPM().have5vA = -1;
        this.getBasePPM().have12vV = -1;
        this.getBasePPM().have12vA = -1;
        this.getBasePPM().v5Power = null;
        this.getBasePPM().v12Power = null;
        this.streamDataIdx = 0;
        this.decodeBufferFunctionsIdx = 0;
        this.decodedStripeIdx = 0;
        this.sliceLength = 0;
        this.decodeBufferFunctions.clear();
        if ((this.streamHeader.format & 8) != 0) {
            this.getBasePPM().have5vV = this.sliceLength++;
            this.decodeBufferFunctions.add(new DecodeBufferFunction(){

                @Override
                public void decode() {
                    PPM.this.decodeVoltageWord();
                }
            });
        }
        if ((this.streamHeader.format & 4) != 0) {
            this.getBasePPM().have5vA = this.sliceLength++;
            this.decodeBufferFunctions.add(new DecodeBufferFunction(){

                @Override
                public void decode() {
                    PPM.this.decode5VCurrnet();
                }
            });
        }
        if ((this.streamHeader.format & 2) != 0) {
            this.getBasePPM().have12vV = this.sliceLength++;
            this.decodeBufferFunctions.add(new DecodeBufferFunction(){

                @Override
                public void decode() {
                    PPM.this.decodeVoltageWord();
                }
            });
        }
        if ((this.streamHeader.format & 1) != 0) {
            this.getBasePPM().have12vA = this.sliceLength++;
            this.decodeBufferFunctions.add(new DecodeBufferFunction(){

                @Override
                public void decode() {
                    PPM.this.decode12VCurrnet();
                }
            });
        }
        if (this.getBasePPM().isEnablePowerCalc()) {
            if (this.getBasePPM().have5vV >= 0 && this.getBasePPM().have5vA >= 0) {
                this.getBasePPM().v5Power = new BaseStreamDevicePPM.PowerData(this, this.getBasePPM().have5vV, this.getBasePPM().have5vA, this.sliceLength++);
            }
            if (this.getBasePPM().have12vV >= 0 && this.getBasePPM().have12vA >= 0) {
                this.getBasePPM().v12Power = new BaseStreamDevicePPM.PowerData(this, this.getBasePPM().have12vV, this.getBasePPM().have12vA, this.sliceLength++);
            }
        }
        this.decodedStripe = new int[this.sliceLength];
    }

    private void handleOneByteStatus(byte b) {
        switch (b) {
            case 0: {
                this.streamStatusCode = StreamStatusCodes.StoppedUser;
                this.disableStreamReceive();
                this.myDle.deviceInfo.cmdLock.release();
                this.running = false;
                break;
            }
            case 1: {
                this.streamStatusCode = StreamStatusCodes.StoppedOverrun;
                this.disableStreamReceive();
                this.myDle.deviceInfo.cmdLock.release();
                this.running = false;
                break;
            }
            case 2: {
                this.streamStatusCode = StreamStatusCodes.StoppedOverTemp;
                this.disableStreamReceive();
                this.myDle.deviceInfo.cmdLock.release();
                this.running = false;
                break;
            }
            case 3: {
                break;
            }
            case 7: {
                break;
            }
            case 99: {
                DebugUtil.debugMsg("99");
                this.streamStatusCode = StreamStatusCodes.StoppedCommsFailure;
                this.disableStreamReceive();
                this.myDle.deviceInfo.cmdLock.release();
            }
        }
    }

    private void processData() {
        int dataLen = this.streamData.length;
        this.streamDataIdx = 0;
        boolean dataReady = false;
        if ((this.streamData.length & 1) != 0) {
            this.handleOneByteStatus(this.streamData[this.streamData.length - 1]);
            --dataLen;
            dataReady = true;
        }
        if (System.currentTimeMillis() - this.cmdWindowTimer > 200L) {
            this.myDle.deviceInfo.cmdLock.release();
            DebugUtil.debugMsg("7");
            this.syncPacketActive = true;
            this.cmdWindowTimer = System.currentTimeMillis();
        }
        if (dataLen > 0) {
            if (this.waitPacketAfterSync) {
                this.syncPacketActive = false;
                this.waitPacketAfterSync = false;
            }
            while (this.streamDataIdx < dataLen) {
                this.stripeTrigger = false;
                this.decodeBufferFunctions.get(this.decodeBufferFunctionsIdx).decode();
                ++this.decodeBufferFunctionsIdx;
                if (this.decodeBufferFunctionsIdx < this.decodeBufferFunctions.size()) continue;
                this.decodeBufferFunctionsIdx = 0;
                this.decodedStripeIdx = 0;
                if (this.getBasePPM().v5Power != null) {
                    this.getBasePPM().v5Power.calcPower(this.decodedStripe, 1.0);
                }
                if (this.getBasePPM().v12Power != null) {
                    this.getBasePPM().v12Power.calcPower(this.decodedStripe, 1.0);
                }
                if (this.getBasePPM().totalPowerData != null) {
                    this.getBasePPM().totalPowerData.calcPower(this.decodedStripe, 1.0);
                }
                this.streamBuffer.addPPMStripeToBuffer(this.stripeTrigger, this.decodedStripe);
            }
            dataReady = true;
        }
        if (this.syncPacketActive) {
            this.myDle.deviceInfo.cmdLock.acquireUninterruptibly();
            this.sendSynk();
        } else if (dataReady && this.running) {
            this.sendStreamContinue();
        }
        if (this.streamBase != null) {
            this.streamBase.fireDataReadyEvent(this.streamStatusCode, this.streamBuffer.size());
        }
    }

    @Override
    public void addDataReadyListener(StreamDataReadyListener toAdd) {
        this.streamBase.addDataReadyListener(toAdd);
    }

    private void sendStreamContinue() {
    }

    private void sendSynk() {
        DebugUtil.debugMsg("Up");
        DebugUtil.debugMsgln("Ack");
        this.syncPacketActive = false;
    }

    private class PPMCalibrationRecord {
        public PPMCalibrationRecordData data = new PPMCalibrationRecordData();

        private PPMCalibrationRecord() {
        }
    }

    static interface DecodeBufferFunction {
        public void decode();
    }

    private class StreamHeader {
        public static final int expectedBufferSize = 64;
        public static final int expectedHeaderSize = 4;
        int version;
        int average;
        int format;
        boolean isValid = false;

        public StreamHeader(int version, int average, int format) {
            this.version = version;
            this.average = average;
            this.format = format;
        }

        public StreamHeader(byte[] data) {
            this.version = data[0];
            this.format = data[2];
            this.average = data[3];
        }
    }
}

