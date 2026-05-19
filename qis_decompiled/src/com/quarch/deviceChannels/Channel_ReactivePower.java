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

public class Channel_ReactivePower
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "pReactive";
    private static final List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "pReactive"), ChannelFunctionIF.asXMLField("returnType", "ReactivePower"), ChannelFunctionIF.asXMLField("description", "Power absorbed by and returned from the load due to its reactive properties."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source1"), ChannelFunctionIF.typeAsXMLField("chanApparentPower"), ChannelFunctionIF.asXMLField("description", "An Apparent Power channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source2"), ChannelFunctionIF.typeAsXMLField("chanActivePower"), ChannelFunctionIF.asXMLField("description", "An Active Power channel."), "</parameter>", "</parameters>");
    private static final String BASE_UNIT = "VAR";
    private final String nameStr;
    private final String groupStr;
    private String unitsString;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[2];
    private int value;

    public Channel_ReactivePower(String channelStr, String srcPApparentChannelStr, String srcPActiveChannelStr, String cmdStr) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcPApparentChannelStr);
        this.srcChannels[1].setNameKey(srcPActiveChannelStr);
        this.unitsString = BASE_UNIT;
        this.origionalCmdStr = cmdStr;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String cmdStr) {
        int pIdx = 0;
        String[] parts = paramStr.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        String srcPApparentChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
        String srcPActiveChannelStr = ChannelFunctionIF.getChannelStr(parts, ++pIdx);
        if (srcPApparentChannelStr.isEmpty() || srcPActiveChannelStr.isEmpty() || srcPActiveChannelStr.toLowerCase().startsWith("chan(")) {
            return null;
        }
        return new Channel_ReactivePower(ChannelFunctionIF.getChannelStr(chanName), srcPApparentChannelStr, srcPActiveChannelStr, cmdStr);
    }

    @Override
    public int getChannelValue() {
        return this.value;
    }

    @Override
    public int calcDefault() {
        double difOfSquares;
        double w1 = this.srcChannels[0].getChannelRef().getChannelValue();
        double w2 = this.srcChannels[1].getChannelRef().getChannelValue();
        this.value = w1 == -2.147483648E9 || w2 == -2.147483648E9 ? Integer.MIN_VALUE : ((difOfSquares = w1 * w1 - w2 * w2) < 0.0 ? Integer.MIN_VALUE : (int)Math.sqrt(difOfSquares));
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
        if (!multiplierStr0.isEmpty()) {
            char multiplier0 = multiplierStr0.toLowerCase().charAt(0);
            if (multiplier0 == 'm') {
                this.unitsString = "mVAR";
            } else if (multiplier0 == 'u') {
                this.unitsString = "uVAR";
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

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

