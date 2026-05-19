/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStreamData;

import java.util.Arrays;

public class StreamRawData {
    public boolean stripeTrigger;
    public int[] stripe;
    public long seqNum;

    public StreamRawData(long seqNum, boolean trigger, int[] stripe) {
        this.seqNum = seqNum;
        this.stripeTrigger = trigger;
        this.stripe = (int[])(stripe != null ? Arrays.copyOf(stripe, stripe.length) : null);
    }

    public int sizeOfToArray() {
        return this.stripe.length + 2;
    }

    public int sizeOfToArrayMinusRecNo() {
        return this.stripe.length + 1;
    }

    public void toArray(int[] data) {
        data[0] = (int)(this.seqNum & Integer.MAX_VALUE);
        data[1] = this.stripeTrigger ? 0x40000000 : 0;
        for (int i = 0; i < this.stripe.length; ++i) {
            data[i + 2] = this.stripe[i];
        }
    }

    public void toArrayMinusRecNo(int[] data) {
        data[1] = this.stripeTrigger ? 0x40000000 : 0;
        for (int i = 0; i < this.stripe.length; ++i) {
            data[i + 1] = this.stripe[i];
        }
    }

    public int[] asArray() {
        int[] data = new int[this.sizeOfToArray()];
        this.toArray(data);
        return data;
    }

    public int[] asArrayMinusRecNo() {
        int[] data = new int[this.sizeOfToArrayMinusRecNo()];
        this.toArrayMinusRecNo(data);
        return data;
    }

    public int getRecNo() {
        return (int)(this.seqNum & Integer.MAX_VALUE);
    }

    public int getDataArraySize() {
        return this.sizeOfToArrayMinusRecNo();
    }

    public String asText() {
        String s = Long.toString(this.seqNum);
        s = this.stripeTrigger ? s + " 1" : s + " 0";
        for (int i : this.stripe) {
            s = s + " " + Integer.toString(i);
        }
        return s;
    }

    public void toSB(StringBuilder sb) {
        sb.append(this.seqNum);
        if (this.stripeTrigger) {
            sb.append(" 1");
        } else {
            sb.append(" 0");
        }
        for (int i : this.stripe) {
            sb.append(" ");
            sb.append(i);
        }
    }
}

