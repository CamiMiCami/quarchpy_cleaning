/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.nio.ByteBuffer;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.deviceMOM.FixtureChannel;
import src.com.quarch.utils.DebugUtil;

public class ChannelData
implements ChannelDataSourceIF {
    private final byte[] bitOffsetMaskMap = new byte[]{0, 1, 3, 7, 15, 31, 63, 127, -1};
    final FixtureChannel fixtureChannel;
    static final int byteWidth = 8;
    private final int dataWidth;
    private final int bitOffset;
    private final int msbMask;
    private final int msbShift;
    private final int lsbShift;
    private final int maxUnsignedValue;
    private final int signBitMask;
    private int lastValue;
    private long valueAccumulator = 0L;
    private int valueSampleCount = 0;
    private double carryRemainderValue;
    private int resampledValue;
    private int channelValue;
    private boolean isResamplingActive;

    public ChannelData(FixtureChannel fixtureChannel, int bitOffset) {
        this.fixtureChannel = fixtureChannel;
        this.dataWidth = fixtureChannel.getDataWidthBits();
        this.bitOffset = bitOffset;
        int orphanbits = Math.abs(this.getDataWidth() - bitOffset) % 8;
        this.msbShift = Math.abs(this.getDataWidth() - bitOffset);
        this.msbMask = this.bitOffsetMaskMap[bitOffset];
        this.lsbShift = 8 - orphanbits >= 8 ? 0 : 8 - orphanbits;
        this.maxUnsignedValue = (int)Math.pow(2.0, this.getDataWidth());
        this.signBitMask = this.getDataWidth() == 1 ? 0 : this.maxUnsignedValue >>> 1;
    }

    protected int getData(byte[] srcBuffer, int offset) {
        long retVal = 0L;
        int bitsConsumed = 0;
        retVal = srcBuffer[offset++] & (this.msbMask & 0xFF);
        if (this.getDataWidth() == 1) {
            retVal >>= this.msbShift;
            return (int)(retVal &= (long)this.bitOffsetMaskMap[this.getDataWidth()]);
        }
        if (this.getDataWidth() < 8 && 8 - this.getDataWidth() <= this.bitOffset) {
            return (int)(retVal >>= this.msbShift);
        }
        bitsConsumed = this.bitOffset;
        while (this.getDataWidth() - bitsConsumed > 0) {
            retVal <<= 8;
            retVal |= (long)(srcBuffer[offset++] & 0xFF);
            bitsConsumed += 8;
        }
        if (((retVal >>= this.lsbShift) & (long)this.signBitMask) != 0L) {
            retVal -= (long)this.maxUnsignedValue;
        }
        return (int)retVal;
    }

    public void toStringBuffer(ByteBuffer dataBB, StringBuilder sb) {
        int bytesConsumed = this.getBytesConsumed();
        if (bytesConsumed > 2) {
            sb.append(dataBB.getInt());
        } else if (bytesConsumed > 1) {
            sb.append(dataBB.getInt());
        } else {
            sb.append(dataBB.get());
        }
    }

    private void putToBuffer(int value, ByteBuffer dataBB) {
        int bytesConsumed = this.getBytesConsumed();
        if (bytesConsumed > 2) {
            dataBB.putInt(value);
        } else if (bytesConsumed > 1) {
            dataBB.putInt(value);
        } else {
            dataBB.put((byte)value);
        }
    }

    public int getBitsConsumed() {
        return this.getDataWidth();
    }

    public int getBytesConsumed() {
        return this.getDataWidth() / 8 + 1;
    }

    public void setData(byte[] srcBuffer, int offset) {
        this.setLastValue(this.getData(srcBuffer, offset));
    }

    public void getData(byte[] srcBuffer, int offset, ByteBuffer dataBB) {
        this.setData(srcBuffer, offset);
        this.putToBuffer(this.getLastValue(), dataBB);
    }

    public void getLastValue(ByteBuffer dataBB) {
        this.putToBuffer(this.getLastValue(), dataBB);
    }

    public void getResampledValue(ByteBuffer dataBB) {
        this.putToBuffer(this.getResampledValue(), dataBB);
    }

    protected void getAdjustedLastValue(int adjustment, ByteBuffer dataBB) {
        this.setAdjustedLastValue(adjustment);
        this.putToBuffer(this.getLastValue(), dataBB);
    }

    protected void setAdjustedLastValue(int adjustment) {
        this.setTruncatedLastValue(this.getLastValue() + adjustment);
    }

    public boolean isGroup(String groupStr) {
        return groupStr.equalsIgnoreCase(this.fixtureChannel.getGroupString());
    }

    @Override
    public String getNameString() {
        return this.fixtureChannel.getNameString();
    }

    public int getLastValue() {
        return this.lastValue;
    }

    protected void setLastValue(int lastValue) {
        this.lastValue = lastValue;
        this.accumulate(lastValue);
    }

    private void accumulate(int value) {
        this.valueAccumulator = this.getValueAccumulator() + (long)value;
        this.valueSampleCount = this.getValueSampleCount() + 1;
    }

    public void clearAccumulator() {
        this.valueAccumulator = 0L;
        this.valueSampleCount = 0;
    }

    void setTruncatedLastValue(int value) {
        int bytesConsumed = this.getBytesConsumed();
        if (bytesConsumed > 2) {
            this.setLastValue(value);
        } else if (bytesConsumed > 1) {
            this.setLastValue(value);
        } else {
            this.setLastValue((byte)this.getLastValue());
        }
    }

    long getValueAccumulator() {
        return this.valueAccumulator;
    }

    double getCarryRemainderValue() {
        return this.carryRemainderValue;
    }

    void setCarryRemainderValue(double carryRemainderValue) {
        this.carryRemainderValue = carryRemainderValue;
    }

    public void addCarryRemainderValue(double remainderValueFraction) {
        this.setCarryRemainderValue(this.getCarryRemainderValue() + remainderValueFraction);
    }

    public int getResampledValue() {
        return this.resampledValue;
    }

    void setResampledValue(int resampledValue) {
        this.resampledValue = resampledValue;
    }

    int getValueSampleCount() {
        return this.valueSampleCount;
    }

    public int getDataWidth() {
        return this.dataWidth;
    }

    @Override
    public String getChannelUnits() {
        return this.fixtureChannel.getUnitsString();
    }

    public long getChannelMaxTValue() {
        return this.fixtureChannel.getMaxTValue();
    }

    public void setResamplingActive(boolean resamplingActive) {
        this.isResamplingActive = resamplingActive;
    }

    public void printValue(String prefix) {
        if (DebugUtil.isEnableCostlyDevDebug()) {
            System.out.println(prefix + " " + this.fixtureChannel.getNameString() + " " + this.fixtureChannel.getUnitsString() + " " + this.lastValue);
        }
    }

    @Override
    public String getChannelKey() {
        return this.fixtureChannel.getChannelKey();
    }

    @Override
    public int getChannelValue() {
        if (this.isResamplingActive) {
            return this.getResampledValue();
        }
        return this.getLastValue();
    }

    @Override
    public int calcDefault() {
        return -1;
    }

    @Override
    public int calcResampled() {
        return -1;
    }

    @Override
    public long getMaxTValue() {
        return this.getChannelMaxTValue();
    }

    @Override
    public long getSpecialLong(int specialId) {
        return 0L;
    }

    @Override
    public String getGroupname() {
        return null;
    }
}

