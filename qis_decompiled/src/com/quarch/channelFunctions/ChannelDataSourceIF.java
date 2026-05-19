/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.channelFunctions;

public interface ChannelDataSourceIF {
    public int getChannelValue();

    public int calcDefault();

    public int calcResampled();

    public String getChannelUnits();

    public long getMaxTValue();

    public String getGroupname();

    public String getNameString();

    default public String getChannelKey() {
        return this.getNameString() + ":" + this.getGroupname();
    }

    default public long getSpecialLong(int specialId) {
        return 0L;
    }
}

