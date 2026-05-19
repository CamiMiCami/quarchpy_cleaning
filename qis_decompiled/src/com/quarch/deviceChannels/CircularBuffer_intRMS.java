/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.math.BigInteger;

class CircularBuffer_intRMS {
    private int[] data;
    private int head;
    private int tail;
    private int count;
    private BigInteger totalSqd = BigInteger.valueOf(0L);

    public CircularBuffer_intRMS(int capacity) {
        this.data = new int[capacity];
        this.head = 0;
        this.tail = 0;
    }

    public CircularBuffer_intRMS(long dataSamplePeriod_ns, long windowTime_ns) {
        int nSamples = (int)(windowTime_ns / dataSamplePeriod_ns);
        if (nSamples <= 0) {
            nSamples = 1;
        }
        this.data = new int[nSamples];
        this.head = 0;
        this.tail = 0;
    }

    public boolean offer(int value) {
        if (this.isFull()) {
            this.poll();
        }
        ++this.count;
        this.data[this.head++] = value;
        this.head %= this.data.length;
        long lValue = value;
        this.totalSqd = this.totalSqd.add(BigInteger.valueOf(lValue * lValue));
        return true;
    }

    private int poll() {
        if (!this.isEmpty()) {
            --this.count;
            long lValue = this.data[this.tail++];
            this.tail %= this.data.length;
            this.totalSqd = this.totalSqd.subtract(BigInteger.valueOf(lValue * lValue));
            return (int)lValue;
        }
        return Integer.MIN_VALUE;
    }

    public boolean isFull() {
        return this.count == this.data.length;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public CircularBuffer_intRMS makeTrimmedCopy(int newCapacity) {
        return null;
    }

    public int getCapacity() {
        return this.data.length;
    }

    public int getValue() {
        long meanL = this.totalSqd.divide(BigInteger.valueOf(this.data.length)).longValue();
        return (int)Math.sqrt(meanL);
    }
}

