/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

public class RawStripeDataHeader {
    public int headerLength;
    public int elementsPerStripe;
    public int numberOfStripes;
    public int dataStartRecordNumber;

    public static int streamBytesToInt(int startIdx, byte[] data) {
        int retVal = data[startIdx];
        byte tmp = data[++startIdx];
        retVal += tmp << 8;
        tmp = data[++startIdx];
        retVal += tmp << 16;
        tmp = data[++startIdx];
        return retVal += tmp << 24;
    }

    public RawStripeDataHeader(byte[] data) {
        this.headerLength = RawStripeDataHeader.streamBytesToInt(0, data);
        this.elementsPerStripe = RawStripeDataHeader.streamBytesToInt(4, data);
        this.numberOfStripes = RawStripeDataHeader.streamBytesToInt(8, data);
        this.dataStartRecordNumber = RawStripeDataHeader.streamBytesToInt(12, data);
    }
}

