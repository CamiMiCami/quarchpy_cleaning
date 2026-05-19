/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.util.List;
import src.com.quarch.deviceMOM.ChannelData;

public class Resampler {
    private boolean isResampleActive = false;
    private long requestedResample_nS = 0L;
    private long devicePeriod_nS = 0L;
    private double carryRemainderSampleTimeFraction;
    private long sampleTimeSum_nS;

    public void configureResampling(boolean resamplingActive, long devicePeriod_nS, long requestedResample_nS) {
        this.isResampleActive = resamplingActive;
        this.devicePeriod_nS = devicePeriod_nS;
        this.requestedResample_nS = requestedResample_nS;
        this.carryRemainderSampleTimeFraction = 0.0;
        this.sampleTimeSum_nS = -devicePeriod_nS;
    }

    protected void logNewDataAvailable() {
        this.sampleTimeSum_nS += this.devicePeriod_nS;
    }

    protected boolean isResampleReady() {
        return this.sampleTimeSum_nS >= this.requestedResample_nS;
    }

    protected void resampleData(List<ChannelData> channelDataList) {
        double remainderTimeFraction = (double)this.requestedResample_nS / (double)this.sampleTimeSum_nS;
        for (ChannelData channelData : channelDataList) {
            int lastValue = channelData.getLastValue();
            long workingTotal = channelData.getValueAccumulator() - (long)lastValue;
            int workingCount = channelData.getValueSampleCount() - 1;
            double dblSampleCount = (double)workingCount + this.carryRemainderSampleTimeFraction + remainderTimeFraction;
            double remainderValueFraction = (double)lastValue * remainderTimeFraction;
            channelData.addCarryRemainderValue(remainderValueFraction);
            double correctedTotal = (double)workingTotal + channelData.getCarryRemainderValue();
            double valueToSet = correctedTotal / dblSampleCount;
            if (channelData.getBitsConsumed() == 1) {
                channelData.setResampledValue(valueToSet > 0.0 ? 1 : 0);
            } else {
                channelData.setResampledValue((int)(valueToSet + 0.5));
            }
            channelData.setCarryRemainderValue((double)lastValue - remainderValueFraction);
            channelData.clearAccumulator();
        }
        this.carryRemainderSampleTimeFraction = 1.0 - remainderTimeFraction;
        this.sampleTimeSum_nS -= this.requestedResample_nS;
    }

    boolean isResampleActive() {
        return this.isResampleActive;
    }

    public long getActiveSampleTime_nS() {
        if (this.isResampleActive()) {
            return this.requestedResample_nS;
        }
        return this.devicePeriod_nS;
    }
}

