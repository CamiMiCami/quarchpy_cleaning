/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.deviceChannels.ActiveCustomChannels;
import src.com.quarch.deviceChannels.PowerCalcPair;
import src.com.quarch.deviceMOM.FixtureChannel;
import src.com.quarch.deviceMOM.StreamHeader;

public class FixtureChannelList {
    List<FixtureChannel> list = new ArrayList<FixtureChannel>();
    List<PowerCalcPair> powerCalcPairList = new ArrayList<PowerCalcPair>();
    ActiveCustomChannels activeCustomChannels = new ActiveCustomChannels();
    private int deviceSamplePerioduS;
    private int channelSamplePerioduS;
    private final long deviceSamplePeriod_nS;
    public boolean triggerEnabled = false;

    public FixtureChannelList(StreamHeader.FixtureGroupV2 fixtureGroupV2, int devicePerioduS) {
        this.setDeviceSamplePerioduS(devicePerioduS);
        this.setChannelSamplePerioduS(devicePerioduS);
        if (fixtureGroupV2 == null) {
            this.deviceSamplePeriod_nS = (long)devicePerioduS * 1000L;
        } else {
            long freq = (long)((double)fixtureGroupV2.getSampleRateBase() * Math.pow(10.0, fixtureGroupV2.getSampleRateExponent()));
            long period_nS = 1000000000L / freq;
            long average = (long)Math.pow(2.0, fixtureGroupV2.getAveragingRate());
            this.deviceSamplePeriod_nS = period_nS * average;
        }
    }

    public int size() {
        return this.list.size();
    }

    public void add(FixtureChannel fc) {
        this.list.add(fc);
    }

    public FixtureChannel get(int i) {
        return this.list.get(i);
    }

    public int getDeviceSamplePerioduS() {
        return this.deviceSamplePerioduS;
    }

    private void setDeviceSamplePerioduS(int devicePerioduS) {
        this.deviceSamplePerioduS = devicePerioduS;
    }

    public int getChannelSamplePerioduS() {
        return this.channelSamplePerioduS;
    }

    private void setChannelSamplePerioduS(int channelSamplePerioduS) {
        this.channelSamplePerioduS = channelSamplePerioduS;
    }

    public long getDeviceSamplePeriod_nS() {
        return this.deviceSamplePeriod_nS;
    }

    public boolean isAllDisabled() {
        return this.list.size() == 0;
    }
}

