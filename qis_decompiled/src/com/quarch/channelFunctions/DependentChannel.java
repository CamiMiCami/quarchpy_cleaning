/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.channelFunctions;

import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.deviceMOM.ChannelData;

public class DependentChannel {
    private String nameKey;
    private ChannelDataSourceIF channelRef;

    public DependentChannel(String nameKey, ChannelData channelRef) {
        this.setNameKey(nameKey);
        this.setChannelRef(channelRef);
    }

    public DependentChannel() {
    }

    public String getNameKey() {
        return this.nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public ChannelDataSourceIF getChannelRef() {
        return this.channelRef;
    }

    public void setChannelRef(ChannelDataSourceIF channelRef) {
        this.channelRef = channelRef;
    }
}

