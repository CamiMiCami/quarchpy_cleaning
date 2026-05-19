/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.Arrays;
import java.util.List;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;

public class Channel_PowerFactor
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "powerFactor";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "powerFactor"), ChannelFunctionIF.asXMLField("returnType", "int%"), ChannelFunctionIF.asXMLField("description", "The ratio of the real power absorbed by the load to the apparent power flowing in the circuit."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source1"), ChannelFunctionIF.typeAsXMLField("chanActivePower"), ChannelFunctionIF.asXMLField("description", "An Active Power channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source2"), ChannelFunctionIF.typeAsXMLField("chanApparentPower"), ChannelFunctionIF.asXMLField("description", "An Apparent Power channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String unitsString;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[2];
    private int value;

    public Channel_PowerFactor(String channelStr, String srcPActiveChannelStr, String srcPApparentChannelStr, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcPActiveChannelStr);
        this.srcChannels[1].setNameKey(srcPApparentChannelStr);
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
            String srcPActiveChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String srcPApparentChannelStr = ChannelFunctionIF.getChannelStr(parts, ++pIdx);
            if (srcPActiveChannelStr.isEmpty() || srcPApparentChannelStr.isEmpty() || srcPApparentChannelStr.toLowerCase().startsWith("chan(")) {
                return null;
            }
            String units = "%";
            double divisor = 1.0;
            return new Channel_PowerFactor(ChannelFunctionIF.getChannelStr(chanName), srcPActiveChannelStr, srcPApparentChannelStr, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    @Override
    public int getChannelValue() {
        return this.value;
    }

    @Override
    public int calcDefault() {
        int w1 = this.srcChannels[0].getChannelRef().getChannelValue();
        int w2 = this.srcChannels[1].getChannelRef().getChannelValue();
        if (w1 == Integer.MIN_VALUE || w2 == Integer.MIN_VALUE || w2 == 0) {
            this.value = Integer.MIN_VALUE;
        } else {
            double v = (double)w1 / (double)w2 * 100.0;
            this.value = (int)v;
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

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

