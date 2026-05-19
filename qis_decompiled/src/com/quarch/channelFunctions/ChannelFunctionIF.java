/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.channelFunctions;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.DependentChannel;

public interface ChannelFunctionIF
extends ChannelDataSourceIF {
    public static final String GROUP_DELTA_VOLTAGE_STR = "dV";
    public static final String GROUP_VOLTAGE_STR = "voltage";
    public static final String GROUP_CURRENT_STR = "current";
    public static final String GROUP_POWER_STR = "power";
    public static final String DigitalUnitsSpecifierString = "(Digital)";
    public static final int NoValueAvailable = Integer.MIN_VALUE;
    public static final String str_ACTIVE_POWER = "ActivePower";
    public static final String str_APPARENT_POWER = "ApparentPower";
    public static final String str_REACTIVE_POWER = "ReactivePower";
    public static final String str_INSTANTANEOUS_POWER = "InstantaneousPower";
    public static final String str_FREQUENCY = "Frequency";
    public static final String str_VOLTAGE = "Voltage";
    public static final String str_CURRENT = "Current";
    public static final String str_POWER = "Power";
    public static final String str_RMS_CURRENT = "RMSCurrent";
    public static final String str_RMS_VOLTAGE = "RMSVoltage";
    public static final String str_ANY = "Any";
    public static final Pattern CHANNEL_PARAMETER_LIST_PATTERN = Pattern.compile("chan\\s*\\([^),:]+,[^):]+\\)((\\s*,\\s*)?chan\\s*\\([^),:]+,[^,):]+\\))*", 2);
    public static final Pattern CHANNEL_PARAMETER_PATTERN = Pattern.compile("(?<wholeParameter>chan\\s*\\((?<channelName>[^),:]+),(?<channelGroup>[^,):]+)\\))(\\s*,\\s*)?", 2);

    default public String keyToStr(String chanKey, int i) {
        String[] parts = chanKey.split(":");
        if (i < parts.length) {
            return parts[i];
        }
        return "";
    }

    public static String[] toChannelReferenceStrings(String rawChannelListString) {
        if (!CHANNEL_PARAMETER_LIST_PATTERN.matcher(rawChannelListString).matches()) {
            return null;
        }
        ArrayList<String> results = new ArrayList<String>();
        Matcher matcher = CHANNEL_PARAMETER_PATTERN.matcher(rawChannelListString.trim());
        while (matcher.find()) {
            String channelNamePart = matcher.group("channelName").trim();
            String channelGroupPart = matcher.group("channelGroup").trim();
            String result = ChannelFunctionIF.toChannelReference(channelNamePart, channelGroupPart);
            results.add(result);
        }
        return results.toArray(new String[0]);
    }

    public static String toChannelReferenceString(String rawChannelParameter) {
        Matcher matcher = CHANNEL_PARAMETER_PATTERN.matcher(rawChannelParameter.trim());
        if (!matcher.matches()) {
            return "";
        }
        String channelNamePart = matcher.group("channelName").trim();
        String channelGroupPart = matcher.group("channelGroup").trim();
        return ChannelFunctionIF.toChannelReference(channelNamePart, channelGroupPart);
    }

    public static String toChannelReference(String channelNamePart, String channelGroupPart) {
        String convertedGroup = channelGroupPart;
        switch (channelGroupPart.toLowerCase()) {
            case "v": {
                convertedGroup = GROUP_VOLTAGE_STR;
                break;
            }
            case "a": {
                convertedGroup = GROUP_CURRENT_STR;
                break;
            }
            case "w": {
                convertedGroup = GROUP_POWER_STR;
            }
        }
        return channelNamePart + ":" + convertedGroup;
    }

    public static String getChannelStr(String str) {
        int pIdx = 0;
        String[] parts = str.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        return ChannelFunctionIF.getChannelStr(parts, pIdx);
    }

    public static long calcWindowSize(String sizeStr) throws NumberFormatException {
        String nStr = ChannelFunctionIF.removeNonDigits(sizeStr);
        String uStr = sizeStr.replaceAll("\\d|\\W", "");
        long windowValue = Integer.parseInt(nStr);
        long multiplier = 1L;
        switch (uStr.toLowerCase()) {
            case "ns": {
                multiplier = 1L;
                break;
            }
            case "us": {
                multiplier = 1000L;
                break;
            }
            case "ms": {
                multiplier = 1000000L;
                break;
            }
            case "s": {
                multiplier = 1000000L;
                break;
            }
            default: {
                throw new NumberFormatException();
            }
        }
        return windowValue * multiplier;
    }

    public static int getIntValue(String str) throws NumberFormatException {
        String nStr = ChannelFunctionIF.removeNonDigits(str);
        int value = Integer.parseInt(nStr);
        return value;
    }

    public static String getChannelStr(String[] parts, int pIdx) {
        if (!parts[pIdx].startsWith("chan(")) {
            int pos = parts[pIdx].indexOf("chan(");
            if (pos < 0) {
                return "";
            }
            parts[pIdx] = parts[pIdx].substring(pos);
        }
        if (parts[pIdx].toLowerCase().startsWith("chan(")) {
            String retStr = parts[pIdx].substring(parts[pIdx].indexOf("(") + 1);
            if (++pIdx < parts.length) {
                int pos = parts[pIdx].indexOf(")");
                String field = pos == -1 ? parts[pIdx] : parts[pIdx].substring(0, pos);
                retStr = ChannelFunctionIF.toChannelReference(retStr, field);
            }
            return retStr;
        }
        return "";
    }

    public static String removeNonDigits(String str) {
        return str.replaceAll("\\D", "");
    }

    public static String asXMLField(String fieldName, String fieldValue) {
        return "<" + fieldName + ">" + fieldValue + "</" + fieldName + ">";
    }

    public static String nameAsXMLField(String name) {
        return ChannelFunctionIF.asXMLField("name", name);
    }

    public static String typeAsXMLField(String type) {
        return ChannelFunctionIF.asXMLField("type", type);
    }

    public static String functionSubTypeAsXMLField(String functionSubType) {
        return ChannelFunctionIF.asXMLField("functionSubType", functionSubType);
    }

    public DependentChannel[] getDependentChannels();

    public boolean makeActive(long var1);

    public int getValue();

    @Override
    public String getNameString();

    @Override
    public String getGroupname();

    public String getUnitsString();

    @Override
    public long getMaxTValue();

    public String getOrigionalCmdStr();

    default public void onChannelLookup() {
    }

    public static String timeWindowAverageDescriptionAsXMLField() {
        return ChannelFunctionIF.asXMLField("description", "Discrete values from the source channel pass through a rolling average function. This value specifies the time window of the averaging function.");
    }

    public static String timeWindowDescriptionAsXMLField() {
        return ChannelFunctionIF.asXMLField("description", "Discrete values from the source channel pass through a rolling window function. This value specifies the time span of the window.");
    }
}

