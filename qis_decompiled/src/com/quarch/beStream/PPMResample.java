/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

public class PPMResample {
    private boolean isResampleActive = false;
    private int devicePerioduS = 0;
    private int requestedResampleuS = 0;
    private int dataFields = 0;
    private long[] totals;
    private double[] carryRemainderValues;
    private double carryRemainderSampleTimeFraction;
    private int sampleTimeSum;
    private int sampleCount = 0;
    private boolean resampleTrigger = false;

    public PPMResample(boolean resamplingActive, int devicePerioduS, int requestedResampleuS, int dataFields) {
        this.isResampleActive = resamplingActive;
        this.devicePerioduS = devicePerioduS;
        this.requestedResampleuS = requestedResampleuS;
        this.dataFields = dataFields;
        this.totals = new long[dataFields];
        this.carryRemainderValues = new double[dataFields];
        this.carryRemainderSampleTimeFraction = 0.0;
        this.sampleTimeSum = -devicePerioduS;
        this.sampleCount = 0;
    }

    public int[] resampleData(boolean trigger, int[] stripe) {
        int[] resampledStripe = null;
        this.resampleTrigger = this.isResampleTrigger() | trigger;
        this.sampleTimeSum += this.devicePerioduS;
        if (this.sampleTimeSum >= this.requestedResampleuS) {
            double remainderTimeFraction = (double)this.requestedResampleuS / (double)this.sampleTimeSum;
            resampledStripe = new int[this.dataFields];
            double dblSampleCount = (double)this.sampleCount + this.carryRemainderSampleTimeFraction + remainderTimeFraction;
            for (int i = 0; i < this.dataFields; ++i) {
                double remainderValueFraction = (double)stripe[i] * remainderTimeFraction;
                int n = i;
                this.carryRemainderValues[n] = this.carryRemainderValues[n] + remainderValueFraction;
                double correctedTotal = (double)this.totals[i] + this.carryRemainderValues[i];
                resampledStripe[i] = (int)(correctedTotal / dblSampleCount);
                this.carryRemainderValues[i] = (double)stripe[i] - remainderValueFraction;
                this.totals[i] = 0L;
            }
            this.sampleCount = 0;
            this.resampleTrigger = false;
            this.carryRemainderSampleTimeFraction = 1.0 - remainderTimeFraction;
            this.sampleTimeSum -= this.requestedResampleuS;
        } else {
            for (int i = 0; i < this.dataFields; ++i) {
                int n = i;
                this.totals[n] = this.totals[n] + (long)stripe[i];
            }
            ++this.sampleCount;
        }
        return resampledStripe;
    }

    public boolean isResampleActive() {
        return this.isResampleActive;
    }

    public boolean isResampleTrigger() {
        return this.resampleTrigger;
    }
}

