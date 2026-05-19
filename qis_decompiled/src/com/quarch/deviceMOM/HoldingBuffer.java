/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

class HoldingBuffer {
    private final byte[] buffer;
    private boolean inUse = false;
    private int holdingLength;

    public HoldingBuffer(int sizeBytes) {
        this.buffer = new byte[sizeBytes];
    }

    public void setBuffer(byte[] data, int startIdx, int len) {
        this.holdingLength = len;
        this.inUse = true;
        System.arraycopy(data, startIdx, this.buffer, this.holdingLength, this.holdingLength);
    }

    public byte[] getBuffer() {
        this.inUse = false;
        return this.buffer;
    }

    public boolean isInUse() {
        return this.inUse;
    }

    public int completeBuffer(byte[] streamData) {
        int retVal = this.buffer.length - this.holdingLength;
        System.arraycopy(streamData, 0, this.buffer, this.holdingLength, retVal);
        return retVal;
    }
}

