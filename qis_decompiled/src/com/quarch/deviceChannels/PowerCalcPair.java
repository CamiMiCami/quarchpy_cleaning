/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.List;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.deviceMOM.ChannelData;

public class PowerCalcPair
implements ChannelDataSourceIF {
    private static final String groupName = "power";
    private final String nameString;
    private final String unitsString;
    protected final ChannelData voltageChannel;
    protected final ChannelData currentChannel;
    protected final double divisor;
    private int calculatedValue = 0;

    public PowerCalcPair(String nameString, String unitsString, ChannelData voltageChannel, ChannelData currentChannel, double finalDivisor) {
        this.nameString = nameString;
        this.unitsString = unitsString;
        this.voltageChannel = voltageChannel;
        this.currentChannel = currentChannel;
        this.divisor = finalDivisor;
    }

    @Override
    public int calcDefault() {
        double tmp = (double)this.voltageChannel.getLastValue() * (double)this.currentChannel.getLastValue() / this.divisor;
        this.setCalculatedValue((int)tmp);
        return this.getCalculatedValue();
    }

    @Override
    public int calcResampled() {
        double tmp = (double)this.voltageChannel.getResampledValue() * (double)this.currentChannel.getResampledValue() / this.divisor;
        this.setCalculatedValue((int)tmp);
        return this.getCalculatedValue();
    }

    public int getCalculatedValue() {
        return this.calculatedValue;
    }

    private void setCalculatedValue(int calculatedValue) {
        this.calculatedValue = calculatedValue;
    }

    @Override
    public String getNameString() {
        return this.nameString;
    }

    @Override
    public String getGroupname() {
        return groupName;
    }

    public String getUnitsString() {
        return this.unitsString;
    }

    public long getPMax() {
        int bits = this.voltageChannel.getDataWidth() + this.currentChannel.getDataWidth();
        return (long)(Math.pow(2.0, bits) / this.divisor);
    }

    public static long getPMax(List<PowerCalcPair> powerCalcPairList) {
        long pMax = 0L;
        for (PowerCalcPair cp : powerCalcPairList) {
            pMax += cp.getPMax();
        }
        return pMax;
    }

    @Override
    public String getChannelKey() {
        String retVal = this.getNameString() + ":" + groupName;
        return retVal;
    }

    @Override
    public int getChannelValue() {
        return this.getCalculatedValue();
    }

    @Override
    public String getChannelUnits() {
        return this.unitsString;
    }

    @Override
    public long getMaxTValue() {
        return this.getPMax();
    }

    @Override
    public long getSpecialLong(int specialId) {
        return 0L;
    }
}

