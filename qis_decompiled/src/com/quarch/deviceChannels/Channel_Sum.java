/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;

public class Channel_Sum
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "sum";
    private static final List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "sum"), ChannelFunctionIF.asXMLField("returnType", "int"), ChannelFunctionIF.asXMLField("description", "The sum of the specified channels."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("sources"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.asXMLField("description", "1 to N channels."), "</parameter>", "</parameters>");
    private static final Pattern EXTRACT_PARAMS_PATTERN = Pattern.compile("^\\((?<params>.*)\\)$");
    private final String nameStr;
    private final String groupStr;
    private String unitsString;
    private final String origionalCmdStr;
    private final List<DependentChannel> operandList;
    private final DependentChannel[] dependentChannels;
    private int value;
    private long maxTValue;

    public Channel_Sum(String outputChannel, String cmdStr, String ... sourceChannels) {
        this.nameStr = this.keyToStr(outputChannel, 0);
        this.groupStr = this.keyToStr(outputChannel, 1);
        if (sourceChannels == null || sourceChannels.length < 2) {
            throw new IllegalArgumentException("Channel_Sum - sourceChannels may not contain less than two values");
        }
        this.operandList = new ArrayList<DependentChannel>();
        LinkedHashMap sourceChannelMap = new LinkedHashMap();
        Arrays.stream(sourceChannels).forEach(sourceChannel -> {
            DependentChannel operandChannel = sourceChannelMap.computeIfAbsent(sourceChannel, channelKey -> {
                DependentChannel dependentChannel = new DependentChannel();
                dependentChannel.setNameKey((String)sourceChannel);
                return dependentChannel;
            });
            this.operandList.add(operandChannel);
        });
        this.dependentChannels = sourceChannelMap.values().toArray(new DependentChannel[0]);
        this.origionalCmdStr = cmdStr;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String cmdStr) {
        String[] channelReferenceStrings;
        Matcher matcher = EXTRACT_PARAMS_PATTERN.matcher(paramStr);
        if (matcher.matches()) {
            paramStr = matcher.group("params");
        }
        if ((channelReferenceStrings = ChannelFunctionIF.toChannelReferenceStrings(paramStr)) == null || channelReferenceStrings.length < 2) {
            return null;
        }
        String outputChannel = ChannelFunctionIF.toChannelReferenceString(chanName);
        if (outputChannel.isEmpty()) {
            return null;
        }
        return new Channel_Sum(outputChannel, cmdStr, channelReferenceStrings);
    }

    @Override
    public int getChannelValue() {
        return this.value;
    }

    @Override
    public int calcDefault() {
        long result = this.operandList.stream().map(DependentChannel::getChannelRef).mapToLong(ChannelDataSourceIF::getChannelValue).reduce((left, right) -> {
            if (left == Integer.MIN_VALUE || right == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return left + right;
        }).orElse(Integer.MIN_VALUE);
        this.value = result > Integer.MAX_VALUE || result <= Integer.MIN_VALUE ? Integer.MIN_VALUE : (int)result;
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
        return this.dependentChannels;
    }

    @Override
    public boolean makeActive(long dataSampleTime_nS) {
        this.maxTValue = this.operandList.stream().map(DependentChannel::getChannelRef).mapToLong(ChannelDataSourceIF::getMaxTValue).reduce(Long::sum).orElse(Integer.MAX_VALUE);
        this.maxTValue = Math.min(this.maxTValue, Integer.MAX_VALUE);
        this.unitsString = this.operandList.stream().findFirst().map(DependentChannel::getChannelRef).map(ChannelDataSourceIF::getChannelUnits).orElse("");
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
        return this.maxTValue;
    }

    @Override
    public String getOrigionalCmdStr() {
        return this.origionalCmdStr;
    }

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

