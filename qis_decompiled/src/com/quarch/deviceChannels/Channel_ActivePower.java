/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLogger
 */
package src.com.quarch.deviceChannels;

import QuarchLogging.QuarchLogger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.CircularBuffer_int;

public class Channel_ActivePower
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "pActive";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "pActive"), ChannelFunctionIF.asXMLField("returnType", "ActivePower"), ChannelFunctionIF.asXMLField("description", "The actual power which is transferred to the load / dissipated in the circuit."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("window"), ChannelFunctionIF.typeAsXMLField("timeWindow"), ChannelFunctionIF.timeWindowAverageDescriptionAsXMLField(), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source"), ChannelFunctionIF.typeAsXMLField("chanInstantaneousPower"), ChannelFunctionIF.asXMLField("description", "A PInstantaneous channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String baseUnits;
    private String unitsString;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[1];
    private double divisor = 1.0;
    private long thisWindow_nS;
    private AtomicLong window_nS = new AtomicLong();
    private long activeSampleTime_nS;
    private CircularBuffer_int average;

    public Channel_ActivePower(String channelStr, String srcChannelStr, long window_nS, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcChannelStr);
        this.divisor = divisor;
        this.thisWindow_nS = window_nS;
        this.baseUnits = units;
        this.unitsString = units;
        this.origionalCmdStr = cmdStr;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String postAmble, String cmdStr) {
        int pIdx = 0;
        String[] parts = paramStr.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        try {
            long windowSize = ChannelFunctionIF.calcWindowSize(parts[pIdx++]);
            String srcChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            if (srcChannelStr.toLowerCase().contains("chan(")) {
                return null;
            }
            String units = "W";
            double divisor = 1.0;
            return new Channel_ActivePower(ChannelFunctionIF.getChannelStr(chanName), srcChannelStr, windowSize, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    @Override
    public int getChannelValue() {
        if (!this.average.isFull()) {
            return Integer.MIN_VALUE;
        }
        return this.average.getValue();
    }

    @Override
    public int calcDefault() {
        this.average.offer(this.srcChannels[0].getChannelRef().getChannelValue());
        return 0;
    }

    @Override
    public int calcResampled() {
        return this.calcDefault();
    }

    @Override
    public String getChannelUnits() {
        return this.unitsString;
    }

    @Override
    public DependentChannel[] getDependentChannels() {
        return this.srcChannels;
    }

    @Override
    public boolean makeActive(long dataSampleTime_nS) {
        this.activeSampleTime_nS = dataSampleTime_nS;
        this.window_nS.set(this.thisWindow_nS);
        this.average = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
        String multiplierStr0 = this.srcChannels[0].getChannelRef().getChannelUnits();
        if (!multiplierStr0.isEmpty()) {
            char multiplier0 = multiplierStr0.toLowerCase().charAt(0);
            if (multiplier0 == 'm') {
                this.unitsString = "m" + this.baseUnits;
            } else if (multiplier0 == 'u') {
                this.unitsString = "u" + this.baseUnits;
            } else {
                QuarchLogger.logMessage((Level)Level.SEVERE, (String)("Cannot resolve Units for <" + this.origionalCmdStr + ">. Channel NOT created"));
            }
        }
        return true;
    }

    @Override
    public int getValue() {
        return 0;
    }

    @Override
    public String getNameString() {
        return this.nameStr;
    }

    @Override
    public String getGroupname() {
        return this.groupStr;
    }

    @Override
    public String getUnitsString() {
        return this.unitsString;
    }

    @Override
    public long getMaxTValue() {
        ChannelDataSourceIF cd = this.srcChannels[0].getChannelRef();
        if (cd != null) {
            return cd.getMaxTValue();
        }
        return -1L;
    }

    @Override
    public String getOrigionalCmdStr() {
        return this.origionalCmdStr;
    }

    @Override
    public void onChannelLookup() {
    }

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

