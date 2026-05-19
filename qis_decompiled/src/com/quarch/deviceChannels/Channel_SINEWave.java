/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.Arrays;
import java.util.List;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;

public class Channel_SINEWave
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "sineWave";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "sineWave"), ChannelFunctionIF.asXMLField("returnType", "int"), ChannelFunctionIF.asXMLField("description", "A test function generating a sine wave based on the supplied parameters."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("length"), ChannelFunctionIF.typeAsXMLField("int"), ChannelFunctionIF.asXMLField("description", "The length or period of the sine wave expressed in counts of the source channel update rate."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("amplitude"), ChannelFunctionIF.typeAsXMLField("int"), ChannelFunctionIF.asXMLField("description", "The amplitude of the generated sine wave."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("offset"), ChannelFunctionIF.typeAsXMLField("int"), ChannelFunctionIF.asXMLField("description", "The offset, in source channel update counts, to the 'start' ie the first positive going, zero crossing point of the sine wave."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.asXMLField("description", "Any source channel. The channel update rate is used as a 'clock' source for stepping through the sine values."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String unitsString;
    private final double divisor;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[1];
    private long srcSamplesPerWave;
    private long amplitude;
    private long sampleCount = 0L;
    private long startOffset;
    int recCount;
    private int calculatedValue = 0;

    private Channel_SINEWave(String chanKey, String srcChannelStr, long srcSamplesPerWave, long amplitude, long startOffset, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(chanKey, 0);
        this.groupStr = this.keyToStr(chanKey, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcChannelStr);
        this.srcSamplesPerWave = srcSamplesPerWave;
        this.amplitude = amplitude;
        this.startOffset = startOffset;
        this.unitsString = units;
        this.divisor = divisor;
        this.origionalCmdStr = cmdStr;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String postAmble, String cmdStr) {
        int pIdx = 0;
        String[] parts = paramStr.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        try {
            long srcSamplesPerWave = Long.parseLong(ChannelFunctionIF.removeNonDigits(parts[pIdx++]));
            long amplitude = Long.parseLong(parts[pIdx++]);
            long startOffset = Long.parseLong(parts[pIdx++]);
            String srcChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String units = "";
            double divisor = 1.0;
            return new Channel_SINEWave(ChannelFunctionIF.getChannelStr(chanName), srcChannelStr, srcSamplesPerWave, amplitude, startOffset, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    @Override
    public int getChannelValue() {
        return this.calculatedValue;
    }

    @Override
    public int calcDefault() {
        double angle = (double)this.sampleCount / (double)this.srcSamplesPerWave * (Math.PI * 2);
        ++this.sampleCount;
        if (this.sampleCount >= this.srcSamplesPerWave) {
            this.sampleCount = 0L;
        }
        this.calculatedValue = (int)(Math.sin(angle) * (double)this.amplitude);
        return 0;
    }

    @Override
    public int calcResampled() {
        return this.calcDefault();
    }

    @Override
    public String getChannelUnits() {
        return null;
    }

    @Override
    public DependentChannel[] getDependentChannels() {
        return this.srcChannels;
    }

    @Override
    public boolean makeActive(long sampleTime_nS) {
        this.sampleCount = this.startOffset;
        return false;
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

