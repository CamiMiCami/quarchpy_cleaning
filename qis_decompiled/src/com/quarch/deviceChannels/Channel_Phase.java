/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.CircularBuffer_int;

public class Channel_Phase
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "phase";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "phase"), ChannelFunctionIF.asXMLField("returnType", "phase"), ChannelFunctionIF.asXMLField("description", "The difference in phase, expressed in degrees, between the two source channels."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("window"), ChannelFunctionIF.typeAsXMLField("timeWindow"), ChannelFunctionIF.timeWindowAverageDescriptionAsXMLField(), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source1"), ChannelFunctionIF.typeAsXMLField("chanFrequency"), ChannelFunctionIF.asXMLField("description", "A frequency channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source2"), ChannelFunctionIF.typeAsXMLField("chanFrequency"), ChannelFunctionIF.asXMLField("description", "A frequency channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String unitsString;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[2];
    private long thisWindow_nS;
    private AtomicLong window_nS = new AtomicLong();
    private long activeSampleTime_nS;
    private CircularBuffer_int highDeltaAverage;
    private CircularBuffer_int lowDeltaAverage;
    private long lastLowDataRecordCount;
    private long lastHighDataRecordCount;
    private int frequency;

    private Channel_Phase(String channelStr, String src1ChannelStr, String src2ChannelStr, long window_nS, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(src1ChannelStr);
        this.srcChannels[1].setNameKey(src2ChannelStr);
        this.thisWindow_nS = window_nS;
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
            String src1ChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String src2ChannelStr = ChannelFunctionIF.getChannelStr(parts, ++pIdx);
            if (src1ChannelStr.isEmpty() || src2ChannelStr.isEmpty()) {
                return null;
            }
            String units = "Deg.";
            double divisor = 1.0;
            return new Channel_Phase(ChannelFunctionIF.getChannelStr(chanName), src1ChannelStr, src2ChannelStr, windowSize, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    @Override
    public int getChannelValue() {
        int lowDelta;
        if (!this.highDeltaAverage.isFull() || !this.lowDeltaAverage.isFull()) {
            return Integer.MIN_VALUE;
        }
        int highDelta = Math.abs(this.highDeltaAverage.getValue());
        double phase = highDelta + (lowDelta = Math.abs(this.lowDeltaAverage.getValue()));
        if (phase == 0.0) {
            return 0;
        }
        phase = (double)highDelta / phase;
        return (int)(phase *= 360.0);
    }

    @Override
    public int calcDefault() {
        DependentChannel src1Channel = this.getDependentChannels()[0];
        long low1 = src1Channel.getChannelRef().getSpecialLong(1);
        long high1 = src1Channel.getChannelRef().getSpecialLong(2);
        DependentChannel src2Channel = this.getDependentChannels()[1];
        long low2 = src2Channel.getChannelRef().getSpecialLong(1);
        long high2 = src2Channel.getChannelRef().getSpecialLong(2);
        this.frequency = src1Channel.getChannelRef().getChannelValue();
        if (this.frequency == Integer.MIN_VALUE) {
            return 0;
        }
        if (low1 != 0L && high1 != 0L && low2 != 0L && high2 != 0L) {
            int lowDelta = (int)(low2 - low1);
            int highDelta = (int)(high2 - high1);
            this.lowDeltaAverage.offer(Math.abs(lowDelta));
            this.highDeltaAverage.offer(Math.abs(highDelta));
        }
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
        this.lowDeltaAverage = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
        this.highDeltaAverage = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
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
    public long getSpecialLong(int specialId) {
        switch (specialId) {
            case 1: {
                return this.lastLowDataRecordCount;
            }
            case 2: {
                return this.lastHighDataRecordCount;
            }
        }
        return 0L;
    }

    @Override
    public void onChannelLookup() {
    }

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

