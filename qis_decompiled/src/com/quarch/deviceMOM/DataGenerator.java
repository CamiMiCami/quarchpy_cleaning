/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLogger
 */
package src.com.quarch.deviceMOM;

import QuarchLogging.QuarchLogger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.ActiveCustomChannels;
import src.com.quarch.deviceChannels.PowerCalcPair;
import src.com.quarch.deviceChannels.PowerCalcTotal;
import src.com.quarch.deviceMOM.ChannelData;
import src.com.quarch.deviceMOM.FixtureChannel;
import src.com.quarch.deviceMOM.FixtureChannelList;
import src.com.quarch.deviceMOM.Resampler;
import src.com.quarch.devices.BaseStreamDevicePPM;
import src.com.quarch.devices.BufferPage;
import src.com.quarch.utils.DebugUtil;

public class DataGenerator {
    public static final int defaultOutDataByteSize = 4;
    public static final int byteSizeOfGroupId = 2;
    private final short dataGroupId;
    private final int nChannels;
    private final FixtureChannelList fixtureChannelsListRef;
    private final List<PowerCalcPair> powerCalcPairListRef;
    private final ActiveCustomChannels activeCustomChannelsRef;
    private final List<ChannelData> channelDataList = new ArrayList<ChannelData>();
    private int rawDataBufferByteSize;
    private boolean absoluteDataReceived = false;
    private BaseStreamDevicePPM.BasePPM basePPM;
    private Resampler resampler = new Resampler();
    private int customChannelElementCount = 0;
    private int triggerValue;
    private int groupOffset;
    private int resampledTrigger = 0;
    PowerCalcTotal powerCalcTotal;

    public DataGenerator(BaseStreamDevicePPM.BasePPM basePPM, short groupId, int groupOffset, FixtureChannelList fixtureChannelsList) {
        this.basePPM = basePPM;
        this.dataGroupId = groupId;
        this.groupOffset = groupOffset;
        this.fixtureChannelsListRef = fixtureChannelsList;
        this.powerCalcPairListRef = fixtureChannelsList.powerCalcPairList;
        this.activeCustomChannelsRef = fixtureChannelsList.activeCustomChannels;
        this.nChannels = fixtureChannelsList.size();
        int bitOffset = 8;
        int totalBitSize = 0;
        for (FixtureChannel channel : fixtureChannelsList.list) {
            this.channelDataList.add(new ChannelData(channel, bitOffset));
            bitOffset = 8 - (totalBitSize += channel.getDataWidthBits()) % 8;
        }
        this.rawDataBufferByteSize = totalBitSize % 8 > 0 ? totalBitSize / 8 + 1 : totalBitSize / 8;
    }

    public void configureResampling(boolean resamplingActive, long requestedResample_nS) {
        this.resampler.configureResampling(resamplingActive, this.getDeviceSamplePeriod_nS(), requestedResample_nS);
        for (ChannelData cd : this.channelDataList) {
            cd.setResamplingActive(resamplingActive);
        }
    }

    public long getDeviceSamplePeriod_nS() {
        return this.fixtureChannelsListRef.getDeviceSamplePeriod_nS();
    }

    public void toStringBuilder(ByteBuffer data, StringBuilder sb, String textSeparater) {
        for (ChannelData channelData : this.channelDataList) {
            sb.append(textSeparater);
            if (data == null) continue;
            channelData.toStringBuffer(data, sb);
        }
        if (this.powerCalcPairListRef.size() > 0) {
            for (int i = 0; i < this.powerCalcPairListRef.size(); ++i) {
                sb.append(textSeparater);
                if (data == null || !data.hasRemaining()) continue;
                sb.append(data.getInt());
            }
            if (this.basePPM.isEnableTotalPowerCalc()) {
                sb.append(textSeparater);
                if (data != null && data.hasRemaining()) {
                    sb.append(data.getInt());
                }
            }
        }
        for (int i = 0; i < this.activeCustomChannelsRef.getActiveChannelList().size(); ++i) {
            int value;
            sb.append(textSeparater);
            if (data == null || !data.hasRemaining() || (value = data.getInt()) == Integer.MIN_VALUE) continue;
            sb.append(value);
        }
    }

    public int getRawDataBufferByteSize() {
        return this.rawDataBufferByteSize;
    }

    public int getDataElementCount() {
        return this.getNoChannels();
    }

    public FixtureChannelList getFixtureChannelsListRef() {
        return this.fixtureChannelsListRef;
    }

    private void getResampledChannelData(BufferPage dataBuffer, ByteBuffer dataBB) {
        this.resampler.resampleData(this.channelDataList);
        dataBB.putShort(this.getModifiedGroupId());
        for (ChannelData channelData : this.channelDataList) {
            channelData.getResampledValue(dataBB);
        }
        if (this.dataGroupId == 0 && this.basePPM.isEnablePowerCalc()) {
            this.putResampledPowerCalcs(dataBB);
        }
        this.putResampledActiveCustomChannels(dataBB);
        dataBuffer.incEntryCounter(this.getModifiedGroupId());
    }

    private short getModifiedGroupId() {
        return (short)(this.dataGroupId + this.groupOffset);
    }

    public void getData(byte[] srcBuffer, int startOffset, BufferPage dataBuffer) {
        ByteBuffer dataBB = dataBuffer.getData();
        this.absoluteDataReceived = true;
        int bitsConsumed = 0;
        if (this.resampler.isResampleActive()) {
            this.resampler.logNewDataAvailable();
            for (ChannelData channelData : this.channelDataList) {
                channelData.setData(srcBuffer, startOffset + bitsConsumed / 8);
                bitsConsumed += channelData.getBitsConsumed();
            }
            if (this.resampler.isResampleReady()) {
                if (DebugUtil.isEnableDevDebug()) {
                    System.out.println("Resampled at " + System.currentTimeMillis() + " group " + this.dataGroupId);
                }
                this.getResampledChannelData(dataBuffer, dataBB);
                if (this.fixtureChannelsListRef.triggerEnabled) {
                    dataBB.put((byte)(this.resampledTrigger & 0xFF));
                    this.resampledTrigger = 0;
                }
            } else {
                this.resampledTrigger |= this.getTriggerValue();
            }
        } else {
            dataBB.putShort(this.getModifiedGroupId());
            for (ChannelData channelData : this.channelDataList) {
                channelData.getData(srcBuffer, startOffset + bitsConsumed / 8, dataBB);
                bitsConsumed += channelData.getBitsConsumed();
                channelData.printValue("Abs:");
            }
            if (this.dataGroupId == 0) {
                if (this.basePPM.isEnablePowerCalc()) {
                    this.putPowerCalcs(dataBB);
                }
                this.putActiveCustomChannels(dataBB);
            }
            if (this.fixtureChannelsListRef.triggerEnabled) {
                dataBB.put((byte)(this.getTriggerValue() & 0xFF));
            }
            dataBuffer.incEntryCounter(this.getModifiedGroupId());
        }
    }

    public void getRepeatData(BufferPage dataBuffer) {
        ByteBuffer dataBB = dataBuffer.getData();
        if (this.resampler.isResampleActive()) {
            this.resampler.logNewDataAvailable();
            for (ChannelData channelData : this.channelDataList) {
                channelData.setLastValue(channelData.getLastValue());
            }
            if (this.resampler.isResampleReady()) {
                this.getResampledChannelData(dataBuffer, dataBB);
                if (this.fixtureChannelsListRef.triggerEnabled) {
                    dataBB.put((byte)(this.resampledTrigger & 0xFF));
                    this.resampledTrigger = 0;
                }
            } else {
                this.resampledTrigger |= this.getTriggerValue();
            }
        } else {
            dataBB.putShort(this.getModifiedGroupId());
            for (ChannelData channelData : this.channelDataList) {
                channelData.getLastValue(dataBB);
                channelData.printValue("Rep:");
            }
            if (this.dataGroupId == 0) {
                if (this.basePPM.isEnablePowerCalc()) {
                    this.putPowerCalcs(dataBB);
                }
                this.putActiveCustomChannels(dataBB);
            }
            if (this.fixtureChannelsListRef.triggerEnabled) {
                dataBB.put((byte)(this.getTriggerValue() & 0xFF));
            }
            dataBuffer.incEntryCounter(this.getModifiedGroupId());
        }
    }

    public void getAdjustedData(byte[] buffer, int startOffset, int adjustmentWidthFieldSize, int adjustmentBitSize, BufferPage dataBuffer) {
        ByteBuffer dataBB = dataBuffer.getData();
        int bitOffset = adjustmentWidthFieldSize + startOffset * 8;
        if (this.resampler.isResampleActive()) {
            this.resampler.logNewDataAvailable();
            for (ChannelData channelData : this.channelDataList) {
                int adjustment = this.getSingedNBits(buffer, bitOffset, adjustmentBitSize);
                bitOffset += adjustmentBitSize;
                channelData.setAdjustedLastValue(adjustment);
            }
            if (this.resampler.isResampleReady()) {
                this.getResampledChannelData(dataBuffer, dataBB);
                if (this.fixtureChannelsListRef.triggerEnabled) {
                    dataBB.put((byte)(this.resampledTrigger & 0xFF));
                    this.resampledTrigger = 0;
                }
            } else {
                this.resampledTrigger |= this.getTriggerValue();
            }
        } else {
            dataBB.putShort(this.getModifiedGroupId());
            for (ChannelData channelData : this.channelDataList) {
                int adjustment = this.getSingedNBits(buffer, bitOffset, adjustmentBitSize);
                bitOffset += adjustmentBitSize;
                channelData.getAdjustedLastValue(adjustment, dataBB);
                channelData.printValue("Delt: " + adjustment + " ");
            }
            if (this.dataGroupId == 0) {
                if (this.basePPM.isEnablePowerCalc()) {
                    this.putPowerCalcs(dataBB);
                }
                this.putActiveCustomChannels(dataBB);
            }
            if (this.fixtureChannelsListRef.triggerEnabled) {
                dataBB.put((byte)(this.getTriggerValue() & 0xFF));
            }
            dataBuffer.incEntryCounter(this.getModifiedGroupId());
        }
    }

    private int getBit(byte[] data, int pos) {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte valByte = data[posByte];
        int valInt = valByte >> 8 - (posBit + 1) & 1;
        return valInt;
    }

    private int getSingedNBits(byte[] buffer, int bitOffset, int adjustmentBitSize) {
        boolean topBitWasSet;
        int bitsRemaining = adjustmentBitSize - 1;
        int retVal = this.getBit(buffer, bitOffset);
        ++bitOffset;
        boolean bl = topBitWasSet = retVal != 0;
        while (bitsRemaining-- > 0) {
            retVal = retVal << 1 | this.getBit(buffer, bitOffset);
            ++bitOffset;
        }
        if (topBitWasSet && retVal > 0) {
            retVal -= 1 << adjustmentBitSize;
        }
        return retVal;
    }

    private void putActiveCustomChannels(ByteBuffer dataBB) {
        if (!this.activeCustomChannelsRef.isEmpty()) {
            for (ChannelDataSourceIF channelDataSourceIF : this.activeCustomChannelsRef.getActiveChannelList()) {
                channelDataSourceIF.calcDefault();
                dataBB.putInt(channelDataSourceIF.getChannelValue());
            }
        }
    }

    private void putResampledActiveCustomChannels(ByteBuffer dataBB) {
        if (!this.activeCustomChannelsRef.isEmpty()) {
            for (ChannelDataSourceIF channelDataSourceIF : this.activeCustomChannelsRef.getActiveChannelList()) {
                channelDataSourceIF.calcResampled();
                dataBB.putInt(channelDataSourceIF.getChannelValue());
            }
        }
    }

    private void putPowerCalcs(ByteBuffer dataBB) {
        if (this.powerCalcPairListRef.size() > 0) {
            if (this.powerCalcTotal != null) {
                this.powerCalcTotal.reset();
            }
            for (PowerCalcPair powerCalcPair : this.powerCalcPairListRef) {
                powerCalcPair.calcDefault();
                int chanValue = powerCalcPair.getCalculatedValue();
                if (this.powerCalcTotal != null) {
                    this.powerCalcTotal.addValueToTotall(chanValue);
                }
                dataBB.putInt(chanValue);
            }
            if (this.basePPM.isEnableTotalPowerCalc()) {
                dataBB.putInt(this.powerCalcTotal.getChannelValue());
            }
        }
    }

    private void putResampledPowerCalcs(ByteBuffer dataBB) {
        if (this.powerCalcPairListRef.size() > 0) {
            if (this.powerCalcTotal != null) {
                this.powerCalcTotal.reset();
            }
            for (PowerCalcPair powerCalcPair : this.powerCalcPairListRef) {
                powerCalcPair.calcResampled();
                int chanValue = powerCalcPair.getCalculatedValue();
                if (this.powerCalcTotal != null) {
                    this.powerCalcTotal.addValueToTotall(chanValue);
                }
                dataBB.putInt(chanValue);
            }
            if (this.basePPM.isEnableTotalPowerCalc()) {
                dataBB.putInt(this.powerCalcTotal.getChannelValue());
            }
        }
    }

    public boolean isAbsoluteDataReceived() {
        return this.absoluteDataReceived;
    }

    int getNoChannels() {
        return this.nChannels;
    }

    private double getUnitSIDivisor(String unitsStr) {
        if (unitsStr.length() == 1) {
            return 1.0;
        }
        char unitsChar = Character.toLowerCase(unitsStr.charAt(0));
        switch (unitsChar) {
            case 'm': {
                return 1000.0;
            }
            case 'u': {
                return 1000000.0;
            }
            case 'n': {
                return 1.0E9;
            }
        }
        QuarchLogger.logMessage((Level)Level.SEVERE, (String)"Cannot resolve Units for Power Calculation");
        return 1.0E9;
    }

    public int buildCalcPowerChannels() {
        int retVal = 0;
        ArrayList<ChannelData> currentChannels = new ArrayList<ChannelData>();
        ArrayList<ChannelData> voltageChannels = new ArrayList<ChannelData>();
        for (ChannelData channel : this.channelDataList) {
            if (channel.isGroup("voltage")) {
                voltageChannels.add(channel);
            }
            if (!channel.isGroup("current")) continue;
            currentChannels.add(channel);
        }
        for (ChannelData voltageChannel : voltageChannels) {
            for (ChannelData currentChannel : currentChannels) {
                if (!voltageChannel.getNameString().equalsIgnoreCase(currentChannel.getNameString())) continue;
                char voltMultiplier = voltageChannel.getChannelUnits().toLowerCase().charAt(0);
                char currentMultiplier = currentChannel.getChannelUnits().toLowerCase().charAt(0);
                if (voltMultiplier == 'm' && currentMultiplier == 'm') {
                    this.powerCalcPairListRef.add(new PowerCalcPair(voltageChannel.getNameString(), "mW", voltageChannel, currentChannel, 1000.0));
                    continue;
                }
                if (voltMultiplier == 'm' && currentMultiplier == 'u') {
                    this.powerCalcPairListRef.add(new PowerCalcPair(voltageChannel.getNameString(), "uW", voltageChannel, currentChannel, 1000.0));
                    continue;
                }
                QuarchLogger.logMessage((Level)Level.SEVERE, (String)"Cannot resolve Units for Power Calculation. Channel NOT created");
            }
        }
        retVal = this.powerCalcPairListRef.size() * 4;
        this.customChannelElementCount += retVal;
        return retVal;
    }

    public int buildCalcTotalPowerChannels() {
        if (this.powerCalcPairListRef.size() > 0) {
            int retVal = 4;
            this.customChannelElementCount += 4;
            this.powerCalcTotal = new PowerCalcTotal("Tot", "uW", PowerCalcPair.getPMax(this.powerCalcPairListRef));
            return 4;
        }
        return 0;
    }

    public String getGroupIdStr() {
        return Integer.toString(this.dataGroupId);
    }

    public int getBaseOutputByteSize() {
        return this.getDataElementCount() * 4 + 2;
    }

    public int getTotalOutputByteSize() {
        int retVal = this.getBaseOutputByteSize() + this.customChannelElementCount * 4;
        if (this.fixtureChannelsListRef.triggerEnabled) {
            ++retVal;
        }
        return retVal;
    }

    public ChannelFunctionIF activateDefinedChannel(ChannelFunctionIF dc) {
        int i;
        String chanKey;
        DependentChannel[] srcChannels = dc.getDependentChannels();
        int itemCount = srcChannels.length;
        boolean[] found = new boolean[itemCount];
        block0: for (ChannelData cd : this.channelDataList) {
            chanKey = cd.getChannelKey();
            for (i = 0; i < itemCount; ++i) {
                if (!chanKey.equalsIgnoreCase(srcChannels[i].getNameKey())) continue;
                srcChannels[i].setChannelRef(cd);
                found[i] = true;
                continue block0;
            }
        }
        if (this.powerCalcPairListRef.size() > 0) {
            block2: for (PowerCalcPair pcp : this.powerCalcPairListRef) {
                chanKey = pcp.getChannelKey();
                for (i = 0; i < itemCount; ++i) {
                    if (!chanKey.equalsIgnoreCase(srcChannels[i].getNameKey())) continue;
                    srcChannels[i].setChannelRef(pcp);
                    found[i] = true;
                    continue block2;
                }
            }
            String chanKey2 = this.powerCalcTotal.getChannelKey();
            for (int i2 = 0; i2 < itemCount; ++i2) {
                if (!chanKey2.equalsIgnoreCase(srcChannels[i2].getNameKey())) continue;
                srcChannels[i2].setChannelRef(this.powerCalcTotal);
                found[i2] = true;
                break;
            }
        }
        if (!this.testAllTrue(found)) {
            dc.onChannelLookup();
            List<ChannelFunctionIF> activeChannelList = this.activeCustomChannelsRef.getActiveChannelList();
            block5: for (ChannelFunctionIF acc : activeChannelList) {
                String chanKey3 = acc.getChannelKey();
                for (int i3 = 0; i3 < itemCount; ++i3) {
                    if (!chanKey3.equalsIgnoreCase(srcChannels[i3].getNameKey())) continue;
                    srcChannels[i3].setChannelRef(acc);
                    found[i3] = true;
                    continue block5;
                }
            }
        }
        if (!this.testAllTrue(found)) {
            return null;
        }
        if (this.activeCustomChannelsRef.contains(dc)) {
            return null;
        }
        dc.makeActive(this.resampler.getActiveSampleTime_nS());
        this.activeCustomChannelsRef.add(dc);
        ++this.customChannelElementCount;
        return dc;
    }

    public int getActiveCustomChannelsByteSize() {
        return this.activeCustomChannelsRef.getActiveChannelList().size() * 4;
    }

    private boolean testAllTrue(boolean[] boolArray) {
        for (boolean b : boolArray) {
            if (b) continue;
            return false;
        }
        return true;
    }

    public boolean setTriggerEnabled(boolean triggerEnabled) {
        for (FixtureChannel channel : this.fixtureChannelsListRef.list) {
            if (!channel.getGroupString().equals("Digital")) continue;
            this.fixtureChannelsListRef.triggerEnabled = triggerEnabled;
            return true;
        }
        return false;
    }

    private int getTriggerValue() {
        return this.triggerValue;
    }

    public void setTriggerValue(int triggerValue) {
        this.triggerValue = triggerValue;
    }
}

