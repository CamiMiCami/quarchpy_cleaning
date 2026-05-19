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
import java.util.logging.Level;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;

public class Channel_ApparentPower
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "pApparent";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "pApparent"), ChannelFunctionIF.asXMLField("returnType", "ApparentPower"), ChannelFunctionIF.asXMLField("description", "The product of RMS voltage and RMS current."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source1"), ChannelFunctionIF.typeAsXMLField("chanRMSVoltage"), ChannelFunctionIF.asXMLField("description", "A RMS Voltage channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source2"), ChannelFunctionIF.typeAsXMLField("chanRMSCurrent"), ChannelFunctionIF.asXMLField("description", "A RMS Current channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String baseUnits;
    private String unitsString;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[2];
    private double divisor = 1.0;
    private int value;

    public Channel_ApparentPower(String channelStr, String srcVRMSChannelStr, String srcIRMSChannelStr, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcVRMSChannelStr);
        this.srcChannels[1].setNameKey(srcIRMSChannelStr);
        this.divisor = divisor;
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
            String srcVRMSChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String srcIRMSChannelStr = ChannelFunctionIF.getChannelStr(parts, ++pIdx);
            if (srcVRMSChannelStr.isEmpty() || srcIRMSChannelStr.isEmpty() || srcIRMSChannelStr.toLowerCase().startsWith("chan(")) {
                return null;
            }
            String units = "VA";
            double divisor = 1.0;
            return new Channel_ApparentPower(ChannelFunctionIF.getChannelStr(chanName), srcVRMSChannelStr, srcIRMSChannelStr, units, divisor, cmdStr);
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
        int v = this.srcChannels[0].getChannelRef().getChannelValue();
        int a = this.srcChannels[1].getChannelRef().getChannelValue();
        this.value = v == Integer.MIN_VALUE || a == Integer.MIN_VALUE ? Integer.MIN_VALUE : (int)((double)v * (double)a / this.divisor);
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
        String multiplierStr0 = this.srcChannels[0].getChannelRef().getChannelUnits();
        String multiplierStr1 = this.srcChannels[1].getChannelRef().getChannelUnits();
        if (!multiplierStr0.isEmpty() && !multiplierStr1.isEmpty()) {
            char multiplier0 = multiplierStr0.toLowerCase().charAt(0);
            char multiplier1 = multiplierStr1.toLowerCase().charAt(0);
            if (multiplier0 == 'm' && multiplier1 == 'm') {
                this.divisor = 1000.0;
                this.unitsString = "m" + this.baseUnits;
            } else if (multiplier0 == 'm' && multiplier1 == 'u' || multiplier0 == 'u' && multiplier1 == 'm') {
                this.divisor = 1000.0;
                this.unitsString = "u" + this.baseUnits;
            } else {
                this.divisor = 1.0;
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
        ChannelDataSourceIF cd1 = this.srcChannels[0].getChannelRef();
        ChannelDataSourceIF cd2 = this.srcChannels[1].getChannelRef();
        if (cd1 != null && cd2 != null) {
            return (long)((double)cd1.getMaxTValue() * (double)cd2.getMaxTValue() / this.divisor);
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

