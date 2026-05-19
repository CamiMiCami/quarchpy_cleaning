/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLoggerInterface
 */
package src.com.quarch.deviceMOM;

import QuarchLogging.QuarchLoggerInterface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beStream.StreamBufferStriped;
import src.com.quarch.beStream.StreamDataReadyListener;
import src.com.quarch.beStream.StreamEventListener;
import src.com.quarch.beStream.StreamFactory;
import src.com.quarch.beStream.StreamStatusCodes;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceMOM.DataGenerator;
import src.com.quarch.deviceMOM.DataGenerators;
import src.com.quarch.deviceMOM.StreamHeader;
import src.com.quarch.deviceMOM.ToTextHelper;
import src.com.quarch.deviceXML.XMLFixture;
import src.com.quarch.devices.BaseStreamDevicePPM;
import src.com.quarch.devices.BufferPage;
import src.com.quarch.devices.DeviceDataBuffer;
import src.com.quarch.devices.PacketCapture;
import src.com.quarch.devices.PagedBuffer;
import src.com.quarch.utils.DebugUtil;

public class MOM
extends BaseStreamDevicePPM
implements StreamEventListener {
    private static final String OK_STR = "OK";
    private static final String FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING = "FAIL: Command not available while streaming";
    private boolean triggerEnabled = false;
    private ToTextHelper toTextHelper;
    GroupDebugTrackers[] groupDebugTrackers;
    PacketCapture packetCapture = null;
    private static final int bihDataHeaderSize = 4;
    private PagedBuffer pagedBuffer = new PagedBuffer();
    private BufferPage binaryStreamDataBuffer = null;
    private BufferPage textStreamDataBuffer = null;
    private boolean toTextBufferDebug = false;
    private final LineInProgress lineInProgress = new LineInProgress();
    private final List<String> groupTextBuffer = new ArrayList<String>();
    private boolean textModePadWithNulls = true;
    private int streamDataIdx;
    private byte[] streamData = null;
    private DataGenerators dataGenerators = new DataGenerators();
    private int largestGroupOutSizeBytes = -1;
    protected BaseStreamDevicePPM.StreamState streamState;
    private boolean isStreamRunning = false;
    private long recordCount = 0L;
    private boolean syncPacketActive = false;
    private boolean waitPacketAfterSync = false;
    private final int syncRetryCountReset = 10;
    private int syncRetryCount = 10;
    private int triggerstate;
    private int debugTonToff;
    int lastGoodIndex = -1;

    public MOM(DeviceListEntry dle) {
        this.myDle = null;
        if (dle != null) {
            this.xmlREF = dle.deviceInfo.getExistingXMLDev();
        }
        this.setNeedFurtherIndetification(false);
        this.setHasREST(true);
        this.setHasSerial(false);
        this.setHasStream(true);
        this.setHasTelnet(false);
        this.setHasUDP(true);
        this.setHasUSB(true);
        this.setVirtualSourceDevice(true);
        this.getBasePPM().setHeaderVersion(BaseStreamDevicePPM.PPMHeaderVersion.V3);
        dle.deviceInfo.cmdBufferSize = 512;
        dle.deviceInfo.streamBufferSize = 512;
        this.streamStatusCode = StreamStatusCodes.STOPPED;
        this.streamBuffer = new StreamBufferStriped();
    }

    @Override
    public void enableStreamReceive(DeviceListEntry dle) {
        boolean startOkay = false;
        this.myDle = dle;
        if (this.myDle.deviceInfo.openForStream()) {
            try {
                if (this.packetCapture != null) {
                    this.packetCapture.reopen();
                }
                this.myDle.deviceInfo.cmdLock.acquireUninterruptibly();
                this.streamBase = StreamFactory.getInstance().createNewStream(this.myDle);
                this.streamBase.streamEventInitiater.addListener(this);
                this.steamThread = new Thread(this.streamBase);
                this.streamState = BaseStreamDevicePPM.StreamState.waitHeader;
                this.initDebugTrackers();
                this.streamBuffer.clear();
                this.textStreamDataBuffer = null;
                this.pagedBuffer.clear();
                this.binaryStreamDataBuffer = null;
                this.recordCount = 0L;
                this.initStreamExceptions();
                if (this.getVirtualOwner() == null) {
                    this.setGroupOffsetValue(0);
                    this.setChannelNamePrefix("");
                }
                this.streamHeaderRef = null;
                this.steamThread.start();
                this.isStreamRunning = true;
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
        if (this.binaryStreamDataBuffer != null) {
            this.pagedBuffer.qFullBufferPage(this.binaryStreamDataBuffer);
            this.binaryStreamDataBuffer = null;
        }
        if (this.streamStatusCode == StreamStatusCodes.RUNNING) {
            this.streamStatusCode = StreamStatusCodes.STOPPED;
        }
        this.streamBase.fireDataReadyEvent(this.streamStatusCode, this.streamBuffer.size());
        this.streamBase.stop();
        this.streamBase = null;
        this.isStreamRunning = false;
        if (this.packetCapture != null) {
            try {
                this.packetCapture.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean modePowerCmd(String sCmd, CmdStruct cmd) {
        boolean retVal = true;
        if (sCmd.toLowerCase().startsWith("stream mode power total")) {
            this.basePPM.setEnableTotalPowerCalc(true);
            this.basePPM.setEnablePowerCalc(true);
            cmd.response.add(OK_STR);
            this.cmdPostAmble(cmd);
        } else if (sCmd.toLowerCase().startsWith("stream mode power enable")) {
            this.basePPM.setEnableTotalPowerCalc(false);
            this.basePPM.setEnablePowerCalc(true);
            cmd.response.add(OK_STR);
            this.cmdPostAmble(cmd);
        } else if (sCmd.toLowerCase().startsWith("stream mode power disable")) {
            this.basePPM.setEnableTotalPowerCalc(false);
            this.basePPM.setEnablePowerCalc(false);
            cmd.response.add(OK_STR);
            this.cmdPostAmble(cmd);
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
            this.cmdPostAmble(cmd);
        } else {
            retVal = false;
        }
        return retVal;
    }

    private boolean streamChannelCmd(String sCmd, CmdStruct cmd) {
        boolean retVal = true;
        if (sCmd.toLowerCase().equals("stream created function definitions?")) {
            this.dataGenerators.listChannelDefinitions(cmd.response);
        } else if (sCmd.toLowerCase().equals("stream created channels?")) {
            this.dataGenerators.listCreatedChannels(cmd.response);
        } else if (sCmd.toLowerCase().equals("stream created channels clear")) {
            if (this.isStreamRunning) {
                cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            } else {
                retVal = this.dataGenerators.deleteAllCreatedChannels(cmd.response);
            }
        } else if (sCmd.toLowerCase().startsWith("stream created channel ")) {
            if (this.isStreamRunning) {
                cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            } else {
                String createCmd = sCmd.substring("stream created channel ".length());
                if (createCmd.toLowerCase().startsWith("delete ")) {
                    String channelDef = createCmd.substring("delete ".length());
                    retVal = this.dataGenerators.deleteCreatedChannel(cmd.response, channelDef);
                }
            }
        } else if (sCmd.toLowerCase().startsWith("stream create channel ")) {
            if (this.isStreamRunning) {
                cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            } else {
                String createCmd = sCmd.substring("stream create channel ".length());
                retVal = this.dataGenerators.createChannel(cmd.response, createCmd);
                if (retVal) {
                    cmd.response.add(OK_STR);
                }
            }
        } else {
            retVal = false;
        }
        return retVal;
    }

    private void queryDeviceEnabledChannels(List<DeviceEnabledChannels> deviceChannelEnables) {
        CmdStruct cmd = new CmdStruct();
        cmd.command = "Fixture:Channels XML?";
        this.myDle.deviceInfo.executeCommand(cmd);
        List rawDeviceResponse = cmd.response;
        if (rawDeviceResponse == null || rawDeviceResponse.isEmpty()) {
            return;
        }
        if (!((String)rawDeviceResponse.get(0)).contains("Invalid or No Longer Valid")) {
            // empty if block
        }
        StringBuilder sb = new StringBuilder();
        if (!((String)rawDeviceResponse.get(0)).contains("<?")) {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        }
        for (int i = 0; i < rawDeviceResponse.size() - 1; ++i) {
            sb.append((String)rawDeviceResponse.get(i));
        }
        XMLFixture xmlFix = XMLFixture.createFromXML(sb.toString());
        List<String> list = xmlFix.getFullEnabledChannelNames();
    }

    @Override
    public boolean internalCommand(CmdStruct cmd, Object caller) {
        int maxReadStripes = 3072;
        String sCmd = cmd.command.replaceAll("(\\r|\\n)", "");
        if (this.getVirtualOwner() != null && !this.getVirtualOwner().equals(caller) && !DebugUtil.isEnableDebug()) {
            cmd.response.add("FAIL: Device is controlled by virtual device: " + this.getVirtualOwner().dle.serialNo);
            this.cmdPostAmble(cmd);
        }
        if (sCmd.toLowerCase().equals("stream?")) {
            this.cmdStreamQuery(cmd);
        } else if (sCmd.toLowerCase().startsWith("stream text")) {
            this.cmdStreamText(cmd, maxReadStripes);
        } else if (sCmd.toLowerCase().startsWith("stream buffer text")) {
            this.cmdStreamBufferText(cmd, maxReadStripes);
        } else if (sCmd.toLowerCase().startsWith("stream bin")) {
            this.cmdStreamBin(cmd, maxReadStripes);
        } else if (!this.modePowerCmd(sCmd, cmd)) {
            if (this.streamChannelCmd(sCmd, cmd)) {
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode header v3")) {
                this.getBasePPM().setHeaderVersion(BaseStreamDevicePPM.PPMHeaderVersion.V3);
                cmd.response.add(OK_STR);
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode header v2")) {
                cmd.response.add("FAIL: Mode Not Supported");
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode header v1")) {
                cmd.response.add("FAIL: Mode Not Supported");
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode header?")) {
                cmd.response.add(this.getBasePPM().getHeaderVersion().getCode());
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode trigger?")) {
                if (this.triggerEnabled) {
                    cmd.response.add("ON");
                } else {
                    cmd.response.add("OFF");
                }
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode trigger on")) {
                if (this.isStreamRunning) {
                    cmd.response.add("FAIL: Cannot Change While Stream Running");
                    this.cmdPostAmble(cmd);
                } else {
                    this.triggerEnabled = true;
                    cmd.response.add(OK_STR);
                    this.cmdPostAmble(cmd);
                }
            } else if (sCmd.toLowerCase().startsWith("stream mode trigger off")) {
                if (this.isStreamRunning) {
                    cmd.response.add("FAIL: Cannot Change While Stream Running");
                    this.cmdPostAmble(cmd);
                } else {
                    this.triggerEnabled = false;
                    cmd.response.add(OK_STR);
                    this.cmdPostAmble(cmd);
                }
            } else if (sCmd.toLowerCase().startsWith("ton")) {
                this.debugTonToff = 1;
                cmd.response.add(OK_STR);
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("toff")) {
                this.debugTonToff = 0;
                cmd.response.add(OK_STR);
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode resample?")) {
                this.getBasePPM().getRequestedResample(cmd.response);
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode resample details?")) {
                this.getBasePPM().getRequestedResampleDetails(cmd.response);
                this.cmdPostAmble(cmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode resample") && !sCmd.toLowerCase().contains("?")) {
                this.cmdStreamResample(cmd, sCmd);
            } else if (sCmd.toLowerCase().startsWith("stream mode capture")) {
                this.cmdCapture(cmd, sCmd);
            }
        }
        return cmd.action == 333;
    }

    public void cmdStreamResample(CmdStruct cmd, String sCmd) {
        if (this.isStreamRunning) {
            cmd.response.add("FAIL: Cannot Change While Stream Running");
        } else {
            boolean cmdError = false;
            String gStr = sCmd.trim();
            int pos = gStr.toLowerCase().indexOf("group ");
            if (pos == -1) {
                gStr = "";
            } else if ((pos = (gStr = gStr.substring(pos + "group ".length()).trim()).lastIndexOf(" ")) == -1) {
                cmd.response.add("FAIL: Invalid Group");
                cmdError = true;
            } else {
                gStr = gStr.substring(0, pos);
            }
            if (!cmdError) {
                String nStr = sCmd.trim();
                pos = nStr.lastIndexOf(" ");
                nStr = pos < "stream mode resample".length() ? "" : nStr.substring(pos + 1);
                if (nStr.toLowerCase().equals("off")) {
                    this.getBasePPM().setRequestedResampleOff(gStr);
                    cmd.response.add(OK_STR);
                } else {
                    String str = this.getBasePPM().setValidatedRequestedResample(gStr, nStr);
                    if (str == null) {
                        cmd.response.add(OK_STR);
                    } else {
                        cmd.response.add("FAIL: " + str);
                    }
                }
            }
        }
        this.cmdPostAmble(cmd);
    }

    private void cmdCapture(CmdStruct cmd, String sCmd) {
        if (sCmd.toLowerCase().startsWith("stream mode capture?")) {
            if (this.packetCapture == null) {
                cmd.response.add("off");
            } else {
                cmd.response.add(this.packetCapture.getPathName());
            }
            this.cmdPostAmble(cmd);
            return;
        }
        if (sCmd.toLowerCase().startsWith("stream mode capture off")) {
            if (this.isStreamRunning) {
                cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
                this.cmdPostAmble(cmd);
                return;
            }
            try {
                if (this.packetCapture != null) {
                    this.packetCapture.close();
                    this.packetCapture = null;
                }
                cmd.response.add(OK_STR);
            }
            catch (Exception e) {
                cmd.response.add("FAIL: Could not remove capture file");
            }
            this.cmdPostAmble(cmd);
            return;
        }
        if (this.isStreamRunning) {
            cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            this.cmdPostAmble(cmd);
            return;
        }
        String pathName = cmd.command.substring("stream mode capture ".length()).trim();
        try {
            if (this.packetCapture != null) {
                this.packetCapture.close();
            }
            this.packetCapture = new PacketCapture(pathName);
            cmd.response.add(OK_STR);
        }
        catch (Exception e) {
            cmd.response.add("FAIL: Could not create capture file");
        }
        this.cmdPostAmble(cmd);
    }

    private void cmdReplay(CmdStruct cmd, String sCmd) {
        if (sCmd.toLowerCase().startsWith("stream mode replay?")) {
            this.cmdPostAmble(cmd);
            return;
        }
        if (sCmd.toLowerCase().startsWith("stream mode replay off")) {
            if (this.isStreamRunning) {
                cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
                this.cmdPostAmble(cmd);
                return;
            }
            this.cmdPostAmble(cmd);
            return;
        }
        if (this.isStreamRunning) {
            cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            this.cmdPostAmble(cmd);
            return;
        }
        this.cmdPostAmble(cmd);
    }

    private void cmdBuffers(CmdStruct cmd, String sCmd) {
        if (sCmd.toLowerCase().startsWith("stream mode buffers?")) {
            this.cmdPostAmble(cmd);
            return;
        }
        if (this.isStreamRunning) {
            cmd.response.add(FAIL_COMMAND_NOT_AVAILABLE_WHILE_STREAMING);
            this.cmdPostAmble(cmd);
            return;
        }
        try {
            int nBuffers = Integer.parseInt(cmd.command.substring("stream mode buffers ".length()).trim());
        }
        catch (NumberFormatException e) {
            int nBuffers = -1;
        }
        catch (StringIndexOutOfBoundsException e) {
            int nBuffers = -1;
        }
    }

    private void cmdPostAmble(CmdStruct cmd) {
        cmd.response.add(">");
        cmd.action = 333;
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
                String exceptionStr = this.getStreamExceptionString();
                if (exceptionStr.isEmpty()) {
                    cmd.response.add("Stopped: User");
                    break;
                }
                cmd.response.add("Stopped: Error " + exceptionStr);
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
            case StopppedFixtureDisconnect: {
                cmd.response.add("Stopped: Fixture Disconnect");
                break;
            }
            case StoppedCommsFailure: {
                cmd.response.add("Stopped: Comms Failure");
                break;
            }
        }
        cmd.response.add("Stripes Buffered: " + Integer.toString(this.pagedBuffer.getTotalBufferedDataGroupEntries(this.binaryStreamDataBuffer)) + " of " + Integer.toString(this.streamBuffer.getMaxRamStripes()));
        this.cmdPostAmble(cmd);
    }

    private void cmdStreamBin(CmdStruct cmd, int maxReadStripes) {
        try {
            BufferPage page = this.pagedBuffer.getFullBufferPage();
            if (page != null) {
                page.getData().rewind();
                if (page.getData().getInt() != 5) {
                    page.getData().rewind();
                }
                page.getData().rewind();
                cmd.setCmBuffer(page);
            } else {
                BufferPage empty = this.pagedBuffer.getEmptyBufferPage();
                empty.getData().putInt(5);
                empty.getData().putInt(8);
                empty.getData().limit(8);
                empty.getData().rewind();
                cmd.setCmBuffer(empty);
            }
            cmd.action = 333;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getStreamHeader(CmdStruct cmd) {
        if (this.getStreamHeaderRef() != null && this.getStreamHeaderRef().isValid() && this.getStreamHeaderRef().isInitialisationComplete()) {
            List<String> sList = this.getStreamHeaderRef().getAsXML();
            for (String s : sList) {
                cmd.response.add(s);
            }
        } else {
            cmd.response.add("Header Not Available");
        }
        return true;
    }

    private void cmdStreamText(CmdStruct cmd, int maxReadStripes) {
        int maxStripes = -1;
        if (cmd.command.substring(12).trim().startsWith("header")) {
            this.getStreamHeader(cmd);
            cmd.response.add(">");
        } else if (this.getStreamHeaderRef() == null) {
            cmd.response.add("eof");
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
            this.textStripeOut(cmd, maxReadStripes, maxStripes);
        }
        cmd.action = 333;
    }

    private boolean bufferDataAvailable() {
        if (this.textStreamDataBuffer == null) {
            this.textStreamDataBuffer = this.pagedBuffer.getFullBufferPage();
            if (this.textStreamDataBuffer != null) {
                this.textStreamDataBuffer.skipOverEntryCounts();
            }
        }
        return this.textStreamDataBuffer != null;
    }

    private boolean bufferHasRemaining(BufferPage bPage) {
        ByteBuffer data = bPage.getData();
        return data.hasRemaining();
    }

    private void textStripeOut(CmdStruct cmd, int maxReadStripes, int maxStripes) {
        if (maxStripes > 0 && maxStripes <= maxReadStripes) {
            cmd.setStringBuilder(null);
            cmd.response.add(">");
            StringBuilder sb = new StringBuilder();
            while (maxStripes > 0) {
                if (!this.bufferDataAvailable()) {
                    sb.append("eof\r\n");
                    break;
                }
                if (this.bufferHasRemaining(this.textStreamDataBuffer)) {
                    boolean isLineComplete = this.bufferStripeToStringBuilder(this.textStreamDataBuffer, sb);
                    if (!isLineComplete) continue;
                    --maxStripes;
                }
                if (this.bufferHasRemaining(this.textStreamDataBuffer)) continue;
                this.textStreamDataBuffer.freeBuffer();
                this.textStreamDataBuffer = null;
            }
            if (this.textStreamDataBuffer != null && !this.bufferHasRemaining(this.textStreamDataBuffer) && this.textStreamDataBuffer != null) {
                this.textStreamDataBuffer.freeBuffer();
                this.textStreamDataBuffer = null;
            }
            sb.append(">");
            cmd.setStringBuilder(sb);
        } else {
            cmd.response.add("Valid Range: 1 to " + Integer.toString(maxReadStripes));
            cmd.response.add(">");
        }
    }

    private void initTextOutput() {
        this.toTextHelper = new ToTextHelper(this.dataGenerators.getDataGeneratorList());
        this.clearGroupTextBuffer();
    }

    private void clearGroupTextBuffer() {
        this.groupTextBuffer.clear();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.dataGenerators.getDataGeneratorList().size(); ++i) {
            sb.setLength(0);
            ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(i), null, sb);
            this.groupTextBuffer.add(sb.toString());
        }
    }

    private boolean bufferStripeToStringBuilder(BufferPage textStreamDataBuffer, StringBuilder sb) {
        ByteBuffer data = textStreamDataBuffer.getData();
        if (this.toTextBufferDebug) {
            byte[] debug = new byte[data.limit()];
            int savePos = data.position();
            data.rewind();
            for (int i = 0; i < debug.length; ++i) {
                if (!data.hasRemaining()) continue;
                debug[i] = data.get();
            }
            data.position(savePos);
        }
        if (this.getStreamHeaderRef().getEnabledGroupIdCount() >= 2 && this.getStreamHeaderRef().isMultiRate()) {
            return this.multiRate_BufferStripeToStringBuilder(textStreamDataBuffer, sb);
        }
        return this.singleRate_BufferStripeToStringBuilder(0, textStreamDataBuffer, sb);
    }

    private boolean multiRate_BufferStripeToStringBuilder(BufferPage textStreamDataBuffer, StringBuilder sb) {
        boolean retVal = false;
        ByteBuffer data = textStreamDataBuffer.getData();
        int debug1 = data.remaining();
        boolean debug2 = data.hasRemaining();
        int groupId = -1;
        try {
            groupId = data.getShort();
        }
        catch (Exception e) {
            groupId = -2;
        }
        int maxGroupId = this.dataGenerators.getDataGeneratorList().size() - 1;
        StringBuilder lipSB = this.lineInProgress.getWorkingSb();
        if (groupId < 0 || groupId > maxGroupId) {
            return retVal;
        }
        int fastestSampleRateGroupId = this.getStreamHeaderRef().getFastestSampleRateGroup();
        if (groupId != fastestSampleRateGroupId) {
            lipSB.setLength(0);
            ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(groupId), data, lipSB);
            this.groupTextBuffer.set(groupId, lipSB.toString());
            retVal = false;
        } else {
            lipSB.setLength(0);
            ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(fastestSampleRateGroupId), data, lipSB);
            this.groupTextBuffer.set(fastestSampleRateGroupId, lipSB.toString());
            sb.append(this.recordCount * (long)this.getBasePPM().getActiveSampleTimeUS());
            sb.append(this.groupTextBuffer.get(0));
            for (int i = 1; i < this.groupTextBuffer.size(); ++i) {
                sb.append(this.groupTextBuffer.get(i));
            }
            sb.append("\r\n");
            ++this.recordCount;
            if (this.textModePadWithNulls) {
                this.clearGroupTextBuffer();
            }
            retVal = true;
        }
        return retVal;
    }

    private boolean singleRate_BufferStripeToStringBuilder(int masterGroup, BufferPage textStreamDataBuffer, StringBuilder sb) {
        boolean retVal = false;
        ByteBuffer data = textStreamDataBuffer.getData();
        int groupId = data.getShort();
        int maxGroupId = this.dataGenerators.getDataGeneratorList().size() - 1;
        LineInProgress lipSB = this.lineInProgress;
        if (groupId < 0 || groupId > maxGroupId) {
            return retVal;
        }
        if (groupId != masterGroup) {
            if (lipSB.isEmpty()) {
                this.bufferStripeToStringBuilderInitLine(maxGroupId);
                for (int i = 0; i < groupId; ++i) {
                    ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(i), null, lipSB.getWorkingSb());
                    lipSB.weHaveStringForGroup(i);
                }
            } else {
                for (int i = groupId; i <= maxGroupId && data.hasRemaining(); ++i) {
                    ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(i), data, lipSB.getWorkingSb());
                    lipSB.weHaveStringForGroup(i);
                    if (!lipSB.isComplete()) {
                        if (!data.hasRemaining()) continue;
                        data.getShort();
                        continue;
                    }
                    break;
                }
            }
        } else {
            this.bufferStripeToStringBuilderInitLine(maxGroupId);
            for (int i = 0; i <= maxGroupId && data.hasRemaining(); ++i) {
                ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(i), data, lipSB.getWorkingSb());
                lipSB.weHaveStringForGroup(i);
                if (!lipSB.isComplete()) {
                    if (!data.hasRemaining()) continue;
                    data.getShort();
                    continue;
                }
                break;
            }
        }
        if (lipSB.isComplete()) {
            sb.append((CharSequence)lipSB.getWorkingSb());
            retVal = true;
            sb.append("\r\n");
            ++this.recordCount;
        }
        return retVal;
    }

    private void bufferStripeToStringBuilderInitLine(int maxGroupId) {
        this.lineInProgress.clear(maxGroupId);
        this.lineInProgress.getWorkingSb().append(this.recordCount * (long)this.getBasePPM().getActiveSampleTimeUS());
    }

    private void cmdStreamBufferText(CmdStruct cmd, int maxReadStripes) {
        int maxStripes = -1;
        cmd.setDebugFilterLongReply(true);
        if (cmd.command.substring(19).trim().startsWith("all")) {
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
        this.simpleTextOut(cmd, maxReadStripes, maxStripes);
        cmd.action = 333;
    }

    private void simpleTextOut(CmdStruct cmd, int maxReadStripes, int maxStripes) {
        if (maxStripes > 0 && maxStripes <= maxReadStripes) {
            cmd.setStringBuilder(null);
            cmd.response.add(">");
            StringBuilder sb = new StringBuilder();
            while (maxStripes > 0) {
                if (!this.bufferDataAvailable()) {
                    sb.append("eof\r\n");
                    break;
                }
                this.simpleBufferStripeToStringBuilder(this.textStreamDataBuffer, sb);
                sb.append("\r\n");
                --maxStripes;
                ++this.recordCount;
                if (this.textStreamDataBuffer.getData().hasRemaining()) continue;
                this.textStreamDataBuffer.freeBuffer();
                this.textStreamDataBuffer = null;
            }
            if (this.textStreamDataBuffer != null && !this.textStreamDataBuffer.getData().hasRemaining() && this.textStreamDataBuffer != null) {
                this.textStreamDataBuffer.freeBuffer();
                this.textStreamDataBuffer = null;
            }
            sb.append(">");
            cmd.setStringBuilder(sb);
        } else {
            cmd.response.add("Valid Range: 1 to " + Integer.toString(maxReadStripes));
            cmd.response.add(">");
        }
    }

    private void simpleBufferStripeToStringBuilder(BufferPage textStreamDataBuffer, StringBuilder sb) {
        ByteBuffer data = textStreamDataBuffer.getData();
        if (this.toTextBufferDebug) {
            byte[] debug = new byte[data.limit()];
            int savePos = data.position();
            data.rewind();
            for (int i = 0; i < debug.length; ++i) {
                if (!data.hasRemaining()) continue;
                debug[i] = data.get();
            }
            data.position(savePos);
        }
        sb.append(this.recordCount);
        sb.append(" ");
        short groupId = data.getShort();
        sb.append(groupId);
        ToTextHelper.toStringBuffer(this.dataGenerators.getDataGeneratorList().get(groupId), data, sb);
    }

    private int processHeader() {
        this.lastGoodIndex = -1;
        this.streamHeaderRef = new StreamHeader(this.getBasePPM(), this.streamData);
        if (!this.getStreamHeaderRef().isValid()) {
            boolean noEnabledGroups = this.getStreamHeaderRef().getNoEnabledGroups();
            this.streamHeaderRef = null;
            if (noEnabledGroups) {
                return -2;
            }
            return -1;
        }
        return 1;
    }

    private boolean isHeaderValid() {
        boolean retVal = false;
        if (this.streamData.length > 1) {
            retVal = true;
        }
        return retVal;
    }

    private void setDecodeParameters() {
        this.dataGenerators.clearDataGeneratorList();
        BaseStreamDevicePPM parent = this.basePPM.getParent();
        int groupOffset = parent.getGroupOffsetValue();
        int totalGroups = parent.totalGroups.get();
        this.dataGenerators.setTotalGroups(totalGroups);
        int enabledGroupIdCount = this.getStreamHeaderRef().getEnabledGroupIdCount();
        for (int i = 0; i < enabledGroupIdCount; ++i) {
            DataGenerator dataGenerator = new DataGenerator(this.getBasePPM(), (short)i, groupOffset, this.getStreamHeaderRef().getEnabledChannelsList(i));
            this.dataGenerators.add(dataGenerator);
            int groupOutSizeBytes = dataGenerator.getBaseOutputByteSize();
            if (this.getBasePPM().isEnablePowerCalc()) {
                groupOutSizeBytes += dataGenerator.buildCalcPowerChannels();
            } else {
                this.largestGroupOutSizeBytes = this.largestGroupOutSizeBytes;
            }
            if (this.getBasePPM().isEnableTotalPowerCalc()) {
                groupOutSizeBytes += dataGenerator.buildCalcTotalPowerChannels();
            } else {
                this.largestGroupOutSizeBytes = this.largestGroupOutSizeBytes;
            }
            this.largestGroupOutSizeBytes = Math.max(groupOutSizeBytes, this.largestGroupOutSizeBytes);
        }
        for (DataGenerator dg : this.dataGenerators.getDataGeneratorList()) {
            this.largestGroupOutSizeBytes = Math.max(dg.getTotalOutputByteSize(), this.largestGroupOutSizeBytes);
        }
        this.groupDebugTrackers = new GroupDebugTrackers[enabledGroupIdCount];
        for (int i = 0; i < this.groupDebugTrackers.length; ++i) {
            this.groupDebugTrackers[i] = new GroupDebugTrackers(groupOffset);
        }
    }

    private void handleOneByteStatus(byte b) {
        switch (b) {
            case 0: {
                this.streamStoppedByStatusCode(StreamStatusCodes.StoppedUser);
                DebugUtil.debugMsgln("User Exit");
                break;
            }
            case 1: {
                this.streamStoppedByStatusCode(StreamStatusCodes.StoppedOverrun);
                DebugUtil.debugMsgln("Overrrun");
                break;
            }
            case 2: {
                this.streamStoppedByStatusCode(StreamStatusCodes.StoppedOverTemp);
                DebugUtil.debugMsgln("Over Temperature");
                break;
            }
            case 3: {
                DebugUtil.debugMsgln("No Valid Data");
                break;
            }
            case 7: {
                this.syncRetryCount = 10;
                this.myDle.deviceInfo.cmdLock.release();
                DebugUtil.debugMsg("7");
                if (this.needStopStreamCmd()) {
                    ++this.stopSentCount;
                    CmdStruct cmd = new CmdStruct();
                    cmd.command = "rec stop";
                    System.out.println("Sending Stop");
                    this.myDle.deviceInfo.executeCommand(cmd);
                    System.out.println("Stop Sent");
                }
                this.syncPacketActive = true;
                break;
            }
            case 8: {
                this.streamStoppedByStatusCode(StreamStatusCodes.StopppedFixtureDisconnect);
                DebugUtil.debugMsgln("Fixture Disconnect");
                break;
            }
            case 99: {
                if (--this.syncRetryCount > 0) {
                    if (this.waitPacketAfterSync) {
                        this.myDle.deviceInfo.cmdLock.release();
                        CmdStruct frigCmd = new CmdStruct();
                        frigCmd.command = "hello";
                        this.myDle.deviceInfo.executeCommand(frigCmd);
                        System.out.println("sync unblock " + (frigCmd.response.size() > 0 ? (String)frigCmd.response.get(0) : "no response"));
                        this.myDle.deviceInfo.cmdLock.acquireUninterruptibly();
                        System.out.println("lock acquired");
                    }
                    this.sendSynk();
                    try {
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                this.syncRetryCount = 10;
                DebugUtil.debugMsg("99");
                this.streamStatusCode = StreamStatusCodes.StoppedCommsFailure;
                this.disableStreamReceive();
                this.myDle.deviceInfo.cmdLock.release();
            }
        }
    }

    private void processData() throws Exception {
        int dataLen = this.streamData.length;
        this.streamDataIdx = 0;
        boolean dataReady = false;
        if (this.waitPacketAfterSync && dataLen > 0) {
            this.waitPacketAfterSync = false;
        }
        if ((this.streamData.length & 1) != 0) {
            if (DebugUtil.isEnableDebug()) {
                String s = "";
                for (byte b : this.streamData) {
                    s = s + Byte.toString(b) + " ";
                }
                DebugUtil.debugMsgln(s);
            }
            this.handleOneByteStatus(this.streamData[this.streamData.length - 1]);
            DebugUtil.devDebugMsgln("One Byte " + this.streamData[this.streamData.length - 1] + " Data Len " + this.streamData.length);
            if (this.streamData.length != 1) {
                dataReady = true;
            }
            --dataLen;
            dataReady = true;
        }
        if (this.terminalException) {
            DebugUtil.devDebugMsgln("Terminal Exception, waiting to stop stream");
            return;
        }
        if (dataLen > 0) {
            this.totalPacketDataLen += (long)dataLen;
            for (int i = 1; i < dataLen; i += 2) {
                byte b = this.streamData[i];
                this.streamData[i] = this.streamData[i - 1];
                this.streamData[i - 1] = b;
            }
            while (this.streamDataIdx < dataLen) {
                byte frameId = this.streamData[this.streamDataIdx];
                int frameStartIndex = this.streamDataIdx;
                switch (frameId) {
                    case 4: {
                        this.doAbsoluteDataPacket(dataLen);
                        break;
                    }
                    case 8: {
                        int blankPayloadSize = this.streamData[this.streamDataIdx + 1] & 0xFF;
                        this.streamDataIdx = this.streamDataIdx + 2 + blankPayloadSize;
                        break;
                    }
                    case 10: {
                        int triggerByte = this.streamData[this.streamDataIdx + 1] & 0xFF;
                        QuarchLoggerInterface.logToDefault((String)"MOM", (Level)Level.INFO, (String)("PAM Trigger packet value: " + triggerByte));
                        if (this.triggerEnabled) {
                            this.triggerstate = triggerByte;
                            this.dataGenerators.setTriggerValue(triggerByte);
                        }
                        this.streamDataIdx += 2;
                        break;
                    }
                    case 12: {
                        this.doDeltaDataPacket(dataLen);
                        break;
                    }
                    case 14: {
                        this.doRepeatDataPacket(dataLen);
                        break;
                    }
                    default: {
                        throw new Exception(this.buildExceptionCorruptDataString(this.streamData, this.lastGoodIndex, this.streamDataIdx));
                    }
                }
                this.lastGoodIndex = frameStartIndex;
            }
            dataReady = true;
        }
        this.binaryStreamDataBuffer = this.pagedBuffer.honourFullPageRequest(this.binaryStreamDataBuffer);
        DebugUtil.devDebugMsgln("");
        DebugUtil.devDebugMsgln("totalPacketDataLen (bytes) " + this.totalPacketDataLen);
        DebugUtil.devDebugMsgln("absolutePacketDatacount " + this.absolutePacketDatacount);
        DebugUtil.devDebugMsgln("repeatPacketDatacount " + this.repeatPacketDatacount);
        DebugUtil.devDebugMsgln("deltaPacketDatacount " + this.deltaPacketDatacount);
        DebugUtil.devDebugMsgln("");
        for (int gdtCount = 0; gdtCount < this.groupDebugTrackers.length; ++gdtCount) {
            GroupDebugTrackers gdt = this.groupDebugTrackers[gdtCount];
            DebugUtil.devDebugMsgln("Group (" + gdtCount + ") Group offset " + gdt.groupOffset);
            DebugUtil.devDebugMsgln("Group (" + gdtCount + ") absolutePacketDatacount " + gdt.absolutePacketDatacount);
            DebugUtil.devDebugMsgln("Group (" + gdtCount + ") repeatPacketDatacount " + gdt.repeatPacketDatacount);
            DebugUtil.devDebugMsgln("Group (" + gdtCount + ") deltaPacketDatacount " + gdt.deltaPacketDatacount);
            DebugUtil.devDebugMsgln("");
        }
        if (this.syncPacketActive) {
            this.myDle.deviceInfo.cmdLock.acquireUninterruptibly();
            this.sendSynk();
            this.waitPacketAfterSync = true;
        } else if (dataReady && this.isStreamRunning) {
            this.sendStreamContinue();
        }
        if (this.streamBase != null) {
            this.streamBase.fireDataReadyEvent(this.streamStatusCode, this.streamBuffer.size());
        }
    }

    private void doRepeatDataPacket(int dataLen) throws Exception {
        byte groupId = this.streamData[this.streamDataIdx + 1];
        int repeatPacketLength = 3;
        ++this.repeatPacketDatacount;
        ++this.groupDebugTrackers[groupId].repeatPacketDatacount;
        if (this.streamDataIdx + 3 <= dataLen) {
            DataGenerator groupDataGenerator = this.dataGenerators.getDataGeneratorList().get(groupId);
            if (groupDataGenerator.isAbsoluteDataReceived()) {
                int repeatCount = this.streamData[this.streamDataIdx + 2] & 0xFF;
                while (repeatCount-- > 0) {
                    if (this.binaryStreamDataBuffer == null) {
                        this.binaryStreamDataBuffer = this.allocateBufferPage();
                    }
                    groupDataGenerator.getRepeatData(this.binaryStreamDataBuffer);
                    int spaceRequired = this.largestGroupOutSizeBytes;
                    this.binaryStreamDataBuffer = this.queueForSendIfFull(this.binaryStreamDataBuffer, spaceRequired);
                }
            }
            this.streamDataIdx += 3;
        } else {
            throw new Exception(this.buildExceptionInternalBufferSizeErrorString());
        }
    }

    private void doDeltaDataPacket(int dataLen) throws Exception {
        byte groupId = this.streamData[this.streamDataIdx + 1];
        DataGenerator groupDataGenerator = this.dataGenerators.getDataGeneratorList().get(groupId);
        int nChannels = groupDataGenerator.getNoChannels();
        int adjustmentWidth = this.streamData[this.streamDataIdx + 2] >> 4 & 0xF;
        ++this.deltaPacketDatacount;
        ++this.groupDebugTrackers[groupId].deltaPacketDatacount;
        int sourceBits = nChannels * adjustmentWidth + 4;
        int sourceBytes = sourceBits % 8 == 0 ? sourceBits / 8 : sourceBits / 8 + 1;
        if (this.streamDataIdx + sourceBytes <= dataLen) {
            if (this.binaryStreamDataBuffer == null) {
                this.binaryStreamDataBuffer = this.allocateBufferPage();
            }
            this.streamDataIdx += 2;
            if (groupDataGenerator.isAbsoluteDataReceived()) {
                groupDataGenerator.getAdjustedData(this.streamData, this.streamDataIdx, 4, adjustmentWidth, this.binaryStreamDataBuffer);
                int spaceRequired = this.largestGroupOutSizeBytes;
                this.binaryStreamDataBuffer = this.queueForSendIfFull(this.binaryStreamDataBuffer, spaceRequired);
            }
            this.streamDataIdx += sourceBytes;
        } else {
            throw new Exception(this.buildExceptionInternalBufferSizeErrorString());
        }
    }

    private void doAbsoluteDataPacket(int dataLen) throws Exception {
        if (this.streamDataIdx + 1 > this.streamData.length) {
            throw new Exception(this.buildExceptionCorruptDataString(this.streamData, this.lastGoodIndex, this.streamDataIdx));
        }
        byte groupId = this.streamData[this.streamDataIdx + 1];
        ++this.absolutePacketDatacount;
        ++this.groupDebugTrackers[groupId].absolutePacketDatacount;
        if (this.streamDataIdx + this.dataGenerators.getDataGeneratorList().get(groupId).getRawDataBufferByteSize() <= dataLen) {
            if (this.binaryStreamDataBuffer == null) {
                this.binaryStreamDataBuffer = this.allocateBufferPage();
            }
            this.streamDataIdx += 2;
            DataGenerator groupDataGenerator = this.dataGenerators.getDataGeneratorList().get(groupId);
            if (groupId >= 0) {
                groupDataGenerator.getData(this.streamData, this.streamDataIdx, this.binaryStreamDataBuffer);
            }
            this.streamDataIdx += groupDataGenerator.getRawDataBufferByteSize();
        } else {
            throw new Exception(this.buildExceptionInternalBufferSizeErrorString());
        }
        int spaceRequired = this.largestGroupOutSizeBytes;
        this.binaryStreamDataBuffer = this.queueForSendIfFull(this.binaryStreamDataBuffer, spaceRequired);
    }

    private BufferPage queueForSendIfFull(BufferPage bufferPage, int spaceRequired) {
        if (!bufferPage.isSpaceFor(spaceRequired)) {
            this.pagedBuffer.qFullBufferPage(bufferPage);
            bufferPage = null;
        }
        return bufferPage;
    }

    public BufferPage allocateBufferPage() throws Exception {
        BufferPage retVal = this.pagedBuffer.getFreeBufferPage();
        if (retVal == null) {
            DebugUtil.devDebugMsgln(System.currentTimeMillis() + " Out Of Space");
            if (DebugUtil.isEnableDevDebug()) {
                this.pagedBuffer.printDebugInfo();
            }
            throw new Exception(this.buildExceptionOutOfBufferSpace());
        }
        retVal.getData().putInt(5);
        retVal.getData().putInt(-1);
        retVal.createEntryCounts(this.dataGenerators.getTotalGroups());
        retVal.positionAfterInitialisation = retVal.getData().position();
        DebugUtil.devDebugMsgln("n");
        return retVal;
    }

    private String buildExceptionInternalBufferSizeErrorString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Internal Buffer Size Error");
        this.setStreamExceptionString(sb.toString());
        sb.append("\nIndex: " + this.streamDataIdx + "\n");
        for (byte b : this.streamData) {
            sb.append(Byte.toUnsignedInt(b));
            sb.append(",");
        }
        return sb.toString();
    }

    public void sanityCheckResample() {
        if (this.getBasePPM().isResamplingActive()) {
            for (DataGenerator dataGenerator : this.dataGenerators.getDataGeneratorList()) {
                String groupIdStr = dataGenerator.getGroupIdStr();
                if (!this.getBasePPM().isResamplingActive(groupIdStr) || this.getBasePPM().getRequestedResampleuS(groupIdStr) * 1000L >= dataGenerator.getDeviceSamplePeriod_nS()) continue;
                this.getBasePPM().setRequestedResample(groupIdStr, (int)(dataGenerator.getDeviceSamplePeriod_nS() / 1000L));
            }
        }
    }

    private boolean isInternalTimeoutPacket(byte[] data) {
        return data.length == 1 && data[0] == 99;
    }

    @Override
    public void streamEvent(DeviceDataBuffer ddBuff) {
        this.streamData = ddBuff.buffer;
        if (this.packetCapture != null && !this.isInternalTimeoutPacket(this.streamData)) {
            this.packetCapture.captureData(this.streamData);
        }
        switch (this.streamState) {
            case waitHeader: {
                if (this.terminalException) {
                    try {
                        this.processData();
                    }
                    catch (Exception exception) {}
                    break;
                }
                if (this.isHeaderValid()) {
                    try {
                        this.checkForHeader();
                    }
                    catch (Exception e) {
                        this.terminalException = true;
                        e.printStackTrace();
                        e.printStackTrace(System.out);
                    }
                }
                if (this.streamState != BaseStreamDevicePPM.StreamState.waitData || !this.getStreamHeaderRef().isValid()) break;
                this.streamData = Arrays.copyOfRange(ddBuff.buffer, this.getStreamHeaderRef().getBytesConsumed(), ddBuff.buffer.length);
            }
            case waitData: {
                try {
                    this.processData();
                }
                catch (Exception e) {
                    this.terminalException = true;
                    e.printStackTrace();
                    e.printStackTrace(System.out);
                }
                break;
            }
        }
    }

    public void checkForHeader() throws Exception {
        int headerState = 0;
        boolean exceptionIsLocal = false;
        try {
            headerState = this.processHeader();
            if (headerState == -2) {
                exceptionIsLocal = true;
                throw new Exception(this.buildExceptionAllGroupsAreEmptyString(this.streamData));
            }
            if (headerState == 1) {
                this.setDecodeParameters();
                this.sanityCheckResample();
                for (DataGenerator dataGenerator : this.dataGenerators.getDataGeneratorList()) {
                    String groupIdStr = dataGenerator.getGroupIdStr();
                    if (this.getBasePPM().isResamplingActive(groupIdStr)) {
                        dataGenerator.configureResampling(true, this.getBasePPM().getRequestedResampleuS(groupIdStr) * 1000L);
                        continue;
                    }
                    dataGenerator.configureResampling(false, 0L);
                }
                this.dataGenerators.buildCustomChannels();
                this.dataGenerators.setTriggerEnabled(this.triggerEnabled);
                for (DataGenerator dg : this.dataGenerators.getDataGeneratorList()) {
                    this.largestGroupOutSizeBytes += dg.getActiveCustomChannelsByteSize();
                }
                this.streamState = BaseStreamDevicePPM.StreamState.waitData;
                this.getStreamHeaderRef().setValid(true);
                this.getStreamHeaderRef().setInitialisationComplete(true);
                this.initTextOutput();
            }
        }
        catch (Exception e) {
            if (exceptionIsLocal) {
                throw e;
            }
            throw new Exception(this.buildExceptionCorruptHeaderString(this.streamData));
        }
    }

    @Override
    public void addDataReadyListener(StreamDataReadyListener toAdd) {
        this.streamBase.addDataReadyListener(toAdd);
    }

    private void sendStreamContinue() {
        this.myDle.deviceInfo.sendStreamContinue();
    }

    private void sendSynk() {
        DebugUtil.debugMsg("Send SyncReset ");
        this.myDle.deviceInfo.sendSyncAck();
        DebugUtil.debugMsgln("Ack");
        this.syncPacketActive = false;
    }

    class LineInProgress {
        private int ourGroupCount = 0;
        private final StringBuilder workingSb = new StringBuilder();
        private boolean[] groupTracker = null;

        public void clear(int maxGroupId) {
            if (this.groupTracker == null) {
                this.groupTracker = new boolean[maxGroupId + 1];
            }
            for (int i = 0; i < this.groupTracker.length; ++i) {
                this.groupTracker[i] = false;
            }
            this.setOurGroupCount(0);
            this.workingSb.setLength(0);
        }

        public StringBuilder getWorkingSb() {
            return this.workingSb;
        }

        public int getOurGroupCount() {
            return this.ourGroupCount;
        }

        public void setOurGroupCount(int ourGroupCount) {
            this.ourGroupCount = ourGroupCount;
        }

        public void incOurGroupCount() {
            ++this.ourGroupCount;
        }

        public boolean isEmpty() {
            return this.ourGroupCount == 0;
        }

        public void weHaveStringForGroup(int groupId) {
            this.groupTracker[groupId] = true;
            this.incOurGroupCount();
        }

        public boolean isComplete() {
            for (int i = 0; i < this.groupTracker.length; ++i) {
                if (this.groupTracker[i]) continue;
                return false;
            }
            return true;
        }
    }

    class DeviceEnabledChannels {
        DeviceEnabledChannels() {
        }
    }

    class GroupDebugTrackers {
        int groupOffset;
        long absolutePacketDatacount = 0L;
        long repeatPacketDatacount = 0L;
        long deltaPacketDatacount = 0L;

        public GroupDebugTrackers(int groupOffset) {
            this.groupOffset = groupOffset;
        }
    }
}

