/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.channelFunctions.ChannelFunctionIF;

public class ActiveCustomChannels {
    private final List<ChannelFunctionIF> activeChannelList = new ArrayList<ChannelFunctionIF>();

    public void add(ChannelFunctionIF dc) {
        this.getActiveChannelList().add(dc);
    }

    public void clear() {
        this.getActiveChannelList().clear();
    }

    public List<ChannelFunctionIF> getActiveChannelList() {
        return this.activeChannelList;
    }

    public boolean isEmpty() {
        return this.activeChannelList.isEmpty();
    }

    public boolean contains(ChannelFunctionIF dc) {
        return this.activeChannelList.contains(dc);
    }
}

