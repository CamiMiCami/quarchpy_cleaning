/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.deviceChannels.ActiveCustomChannels;
import src.com.quarch.deviceChannels.DefinedCustomChannels;
import src.com.quarch.deviceMOM.DataGenerator;
import src.com.quarch.deviceMOM.FixtureChannelList;

public class DataGenerators {
    private final List<DataGenerator> dataGeneratorList = new ArrayList<DataGenerator>();
    private final DefinedCustomChannels definedChannels = new DefinedCustomChannels();
    private final ActiveCustomChannels activeChannels = new ActiveCustomChannels();
    private int totalGroups = 0;

    public void clearDataGeneratorList() {
        this.getDataGeneratorList().clear();
    }

    public void add(DataGenerator dataGenerator) {
        this.getDataGeneratorList().add(dataGenerator);
    }

    public List<DataGenerator> getDataGeneratorList() {
        return this.dataGeneratorList;
    }

    public void listCreatedChannels(List<String> response) {
        if (this.definedChannels.getDefinedMap().isEmpty()) {
            response.add("No Created Channels");
            return;
        }
        for (String key : this.definedChannels.getDefinedMap().keySet()) {
            ChannelFunctionIF dc = this.definedChannels.getDefinedMap().get(key);
            response.add(dc.getOrigionalCmdStr());
        }
    }

    public boolean deleteCreatedChannel(List<String> response, String channelDef) {
        this.definedChannels.deleteChannel(response, channelDef);
        return true;
    }

    public boolean deleteAllCreatedChannels(List<String> response) {
        this.definedChannels.deleteAll(response);
        return true;
    }

    public boolean createChannel(List<String> response, String createCmd) {
        return this.definedChannels.addChannel(response, createCmd);
    }

    public void buildCustomChannels() {
        boolean anotherLoopNeeded;
        do {
            anotherLoopNeeded = false;
            for (String dcKey : this.definedChannels.getDefinedMap().keySet()) {
                ChannelFunctionIF dc = this.definedChannels.getDefinedMap().get(dcKey);
                for (DataGenerator dg : this.dataGeneratorList) {
                    ChannelFunctionIF cf = dg.activateDefinedChannel(dc);
                    if (cf == null) continue;
                    anotherLoopNeeded = true;
                }
            }
        } while (anotherLoopNeeded);
    }

    public void listChannelDefinitions(List<String> response) {
        this.definedChannels.listChannelDefinitions(response);
    }

    public void setTriggerEnabled(boolean triggerEnabled) {
        for (DataGenerator dg : this.dataGeneratorList) {
            if (!dg.setTriggerEnabled(triggerEnabled)) continue;
            return;
        }
        if (!triggerEnabled) {
            return;
        }
        DataGenerator dg0 = this.dataGeneratorList.get(0);
        FixtureChannelList dg0FixtureChannelList = dg0.getFixtureChannelsListRef();
        dg0FixtureChannelList.triggerEnabled = true;
    }

    public void setTriggerValue(int triggerValue) {
        for (DataGenerator dg : this.dataGeneratorList) {
            dg.setTriggerValue(triggerValue);
        }
    }

    public int getTotalGroups() {
        return this.totalGroups;
    }

    public void setTotalGroups(int totalGroups) {
        this.totalGroups = totalGroups;
    }
}

