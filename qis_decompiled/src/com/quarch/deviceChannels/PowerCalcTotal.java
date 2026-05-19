/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import src.com.quarch.channelFunctions.ChannelDataSourceIF;

public class PowerCalcTotal
implements ChannelDataSourceIF {
    private static final String groupName = "power";
    private final String nameString;
    private final String unitsString;
    private final long maxTValue;
    private int total;

    public PowerCalcTotal(String name, String units, long maxTValue) {
        this.nameString = name;
        this.unitsString = units;
        this.maxTValue = maxTValue;
        this.reset();
    }

    @Override
    public int calcDefault() {
        return 0;
    }

    @Override
    public int calcResampled() {
        return 0;
    }

    @Override
    public String getNameString() {
        return this.nameString;
    }

    private int getCalculatedValue() {
        return this.total;
    }

    public void reset() {
        this.total = 0;
    }

    public int addValueToTotall(int value) {
        this.total += value;
        return this.total;
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
        return this.maxTValue;
    }

    @Override
    public long getSpecialLong(int specialId) {
        return 0L;
    }

    @Override
    public String getGroupname() {
        return null;
    }
}

