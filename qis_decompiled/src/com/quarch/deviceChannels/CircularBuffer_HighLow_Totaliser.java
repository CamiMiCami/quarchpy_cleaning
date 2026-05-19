/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import src.com.quarch.deviceChannels.CircularBuffer_intRMS;

public class CircularBuffer_HighLow_Totaliser {
    private static final int minimumSize = 2;
    private int hiThreshold = 0;
    private int lowThreshold = 0;
    private int[] data;
    private DataHiLowType[] dataType;
    private int head;
    private int tail;
    private int dataCount;
    private int offerCount;
    private long hiTotal = 0L;
    private int hiTotalCount = 0;
    private long lowTotal = 0L;
    private int lowTotalCount = 0;

    public CircularBuffer_HighLow_Totaliser(int capacity) {
        this.initBufferSpace(capacity);
    }

    public CircularBuffer_HighLow_Totaliser(long dataSamplePeriod_ns, long windowTime_ns) {
        int nSamples = (int)(windowTime_ns / dataSamplePeriod_ns);
        if (nSamples <= 2) {
            nSamples = 2;
        }
        this.initBufferSpace(nSamples);
    }

    private void initBufferSpace(int capacity) {
        this.data = new int[capacity];
        this.dataType = new DataHiLowType[capacity];
        this.head = 0;
        this.tail = 0;
    }

    public boolean offer(int value) {
        if (this.isFull()) {
            this.poll();
        }
        ++this.offerCount;
        if (value >= this.getHiThreshold()) {
            this.hiTotal += (long)value;
            ++this.hiTotalCount;
            ++this.dataCount;
            this.dataType[this.head] = DataHiLowType.isHi;
            this.data[this.head++] = value;
            this.head %= this.data.length;
        } else if (value <= this.getLowThreshold()) {
            this.lowTotal += (long)value;
            ++this.lowTotalCount;
            ++this.dataCount;
            this.dataType[this.head] = DataHiLowType.isLow;
            this.data[this.head++] = value;
            this.head %= this.data.length;
        }
        return true;
    }

    private int poll() {
        if (!this.isEmpty()) {
            --this.offerCount;
            --this.dataCount;
            DataHiLowType type = this.dataType[this.tail];
            int value = this.data[this.tail++];
            this.tail %= this.data.length;
            if (type == DataHiLowType.isHi) {
                this.hiTotal -= (long)value;
                --this.hiTotalCount;
            } else {
                this.lowTotal -= (long)value;
                --this.lowTotalCount;
            }
            return value;
        }
        return Integer.MIN_VALUE;
    }

    public boolean isFull() {
        return this.offerCount >= this.data.length;
    }

    public boolean isEmpty() {
        return this.dataCount == 0;
    }

    public CircularBuffer_intRMS makeTrimmedCopy(int newCapacity) {
        return null;
    }

    public int getCapacity() {
        return this.data.length;
    }

    public int getValue() {
        return Integer.MIN_VALUE;
    }

    public int getHighValue() {
        if (this.hiTotalCount == 0) {
            return Integer.MIN_VALUE;
        }
        return (int)(this.hiTotal / (long)this.hiTotalCount);
    }

    public int getLowValue() {
        if (this.lowTotalCount == 0) {
            return Integer.MIN_VALUE;
        }
        return (int)(this.lowTotal / (long)this.lowTotalCount);
    }

    public int getHiThreshold() {
        return this.hiThreshold;
    }

    public void setHiThreshold(int hiThreshold) {
        this.hiThreshold = hiThreshold;
    }

    public int getLowThreshold() {
        return this.lowThreshold;
    }

    public void setLowThreshold(int lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    static enum DataHiLowType {
        isHi,
        isLow;

    }
}

