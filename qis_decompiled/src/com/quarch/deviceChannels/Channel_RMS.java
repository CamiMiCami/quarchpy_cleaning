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
import src.com.quarch.deviceChannels.CircularBuffer_intRMS;

public class Channel_RMS
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "rms";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "rms"), ChannelFunctionIF.asXMLField("returnType", "int"), ChannelFunctionIF.asXMLField("description", "The effective value of a sinusoidal waveform which gives the same heating effect as an equivalent DC supply."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("window"), ChannelFunctionIF.typeAsXMLField("timeWindow"), ChannelFunctionIF.timeWindowAverageDescriptionAsXMLField(), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.asXMLField("description", "Any source channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private String unitsString;
    private final String origionalCmdStr;
    private AtomicLong window_nS = new AtomicLong();
    private long thisWindow_nS;
    private final double divisor;
    private CircularBuffer_intRMS rmsBuffer = null;
    private long activeSampleTime_nS;
    private final DependentChannel[] srcChannels = new DependentChannel[1];

    private void testBuffer() {
        CircularBuffer_intRMS cb = new CircularBuffer_intRMS(5);
        cb.offer(1);
        cb.offer(2);
        cb.offer(3);
        cb.offer(4);
        cb.offer(5);
        cb.offer(6);
        cb.offer(7);
        cb.offer(8);
        cb.offer(9);
        cb.offer(10);
        cb.offer(11);
    }

    private Channel_RMS(String chanKey, String srcChannelStr, long window_nS, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(chanKey, 0);
        this.groupStr = this.keyToStr(chanKey, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcChannelStr);
        this.thisWindow_nS = window_nS;
        this.unitsString = units;
        this.divisor = divisor;
        this.origionalCmdStr = cmdStr;
    }

    @Override
    public DependentChannel[] getDependentChannels() {
        return this.srcChannels;
    }

    @Override
    public boolean makeActive(long dataSampleTime_nS) {
        String chan0Units;
        this.activeSampleTime_nS = dataSampleTime_nS;
        this.window_nS.set(this.thisWindow_nS);
        this.rmsBuffer = new CircularBuffer_intRMS(dataSampleTime_nS, this.window_nS.get());
        this.unitsString = chan0Units = this.srcChannels[0].getChannelRef().getChannelUnits();
        return false;
    }

    @Override
    public int getValue() {
        return 0;
    }

    @Override
    public int getChannelValue() {
        if (this.rmsBuffer.isFull()) {
            return (int)((double)this.rmsBuffer.getValue() / this.divisor);
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public int calcDefault() {
        DependentChannel srcChannel = this.getDependentChannels()[0];
        int value = srcChannel.getChannelRef().getChannelValue();
        this.rmsBuffer.offer(value);
        return value;
    }

    @Override
    public int calcResampled() {
        DependentChannel srcChannel = this.getDependentChannels()[0];
        int value = srcChannel.getChannelRef().getChannelValue();
        this.rmsBuffer.offer(value);
        return value;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String postAmble, String cmdStr) {
        int pIdx = 0;
        String[] parts = paramStr.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        try {
            double divisor;
            String units;
            long windowSize = ChannelFunctionIF.calcWindowSize(parts[pIdx++]);
            String srcChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String[] postParts = postAmble.split(",");
            if (postParts.length == 1 && postParts[0].equals("")) {
                units = "";
                divisor = 1.0;
            } else {
                units = Channel_RMS.getUnits(postParts[0]);
                divisor = Channel_RMS.getDivisor(postParts[1]);
            }
            return new Channel_RMS(ChannelFunctionIF.getChannelStr(chanName), srcChannelStr, windowSize, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private static double getDivisor(String string) {
        return Double.parseDouble(string);
    }

    private static String getUnits(String string) {
        return string;
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
        ChannelDataSourceIF cd = this.srcChannels[0].getChannelRef();
        if (cd != null) {
            return cd.getChannelUnits();
        }
        return "Error";
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
    public String getChannelUnits() {
        return this.unitsString;
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

