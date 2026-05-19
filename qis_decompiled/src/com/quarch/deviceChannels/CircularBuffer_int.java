/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import src.com.quarch.deviceChannels.CircularBuffer_intRMS;

public class CircularBuffer_int {
    private static final int minimumSize = 2;
    private int[] data;
    private int head;
    private int tail;
    private int count;
    private long total;

    public CircularBuffer_int(int capacity) {
        this.data = new int[capacity];
        this.head = 0;
        this.tail = 0;
    }

    public CircularBuffer_int(long dataSamplePeriod_ns, long windowTime_ns) {
        int nSamples = (int)(windowTime_ns / dataSamplePeriod_ns);
        if (nSamples <= 2) {
            nSamples = 2;
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
        this.getData()[this.head++] = value;
        this.head %= this.getData().length;
        this.total += (long)value;
        return true;
    }

    private int poll() {
        if (!this.isEmpty()) {
            --this.count;
            int value = this.getData()[this.tail++];
            this.tail %= this.getData().length;
            this.total -= (long)value;
            return value;
        }
        return Integer.MIN_VALUE;
    }

    public boolean isFull() {
        return this.count == this.getData().length;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public CircularBuffer_intRMS makeTrimmedCopy(int newCapacity) {
        return null;
    }

    public int getCapacity() {
        return this.getData().length;
    }

    public int getValue() {
        return (int)(this.total / (long)this.count);
    }

    public double getDoubleValue() {
        return (double)this.total / (double)this.count;
    }

    public int[] getData() {
        return this.data;
    }
}

