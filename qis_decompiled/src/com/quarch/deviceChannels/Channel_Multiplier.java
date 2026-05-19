/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.deviceChannels.Channel_ChannelMultiplier;
import src.com.quarch.deviceChannels.Channel_ScalarMultiplier;
import src.com.quarch.utils.Magnitude;

public class Channel_Multiplier {
    public static final String functionName = "multiplier";
    public static final String MAGNITUDE_PATTERN_FRAGMENT = "auto|" + Arrays.stream(Magnitude.values()).map(magnitude -> magnitude.getParameterValue() + "|" + magnitude.getDisplayName()).collect(Collectors.joining("|"));
    public static final Pattern SCALAR_MULTIPLIER_PARAMETERS_PATTERN = Pattern.compile("\\(\\s?(?<baseUnit>\\w+)\\s?,\\s?((?<magnitude>" + MAGNITUDE_PATTERN_FRAGMENT + ")\\s?,\\s?)?(?<scalar>([+\\-])?\\d+(\\.\\d+)?)\\s?,\\s?(?<channel>chan\\([^),:]+,[^,):]+\\))\\s?\\)", 2);
    public static final Pattern UNIT_AND_OTHER_PATTERN = Pattern.compile("\\(\\s?(?<baseUnit>\\w+)\\s?,\\s?((?<magnitude>" + MAGNITUDE_PATTERN_FRAGMENT + ")\\s?,\\s?)?(?<channels>.*)\\s?\\)", 2);
    public static final Pattern SOURCE_UNITS_PATTERN = Pattern.compile("(?<magnitude>[\\sZEPTGMkmunpfaz])?(?<unit>\\S.*)");
    private static final List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "multiplier"), ChannelFunctionIF.asXMLField("returnType", "int"), ChannelFunctionIF.asXMLField("description", "Either the scalar multiple of a single source channel, or the product of at least two source channels."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("baseUnit"), ChannelFunctionIF.typeAsXMLField("string"), ChannelFunctionIF.functionSubTypeAsXMLField("all"), ChannelFunctionIF.asXMLField("description", "The base unit for the generated channel. This should not include magnitude."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("magnitude"), ChannelFunctionIF.typeAsXMLField("magnitude"), ChannelFunctionIF.functionSubTypeAsXMLField("all"), ChannelFunctionIF.asXMLField("description", "The output magnitude. Note that this may result in loss of precision or values that cannot fit within the output range of the channel, depending on the source channel magnitudes. Set this value to auto to let the software choose a suitable output magnitude."), ChannelFunctionIF.asXMLField("validValues", "Auto,Tera,Giga,Mega,Kilo,Unit,Milli,Micro,Nano,Pico"), ChannelFunctionIF.asXMLField("abbreviatedValues", "T,G,M,k,m,u,n,p"), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("scalar"), ChannelFunctionIF.typeAsXMLField("decimal"), ChannelFunctionIF.functionSubTypeAsXMLField("scalar"), ChannelFunctionIF.asXMLField("description", "A scalar by which the values of the source channel should be multiplied. Sign may be included. If scalar is present there should only be one source channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.functionSubTypeAsXMLField("scalar"), ChannelFunctionIF.asXMLField("description", "Any source channel."), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("sources"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.functionSubTypeAsXMLField("channel"), ChannelFunctionIF.asXMLField("description", "Any comma separated list of at least two channels."), "</parameter>", "</parameters>");

    public static Magnitude getMagnitudeFromChannelUnitString(String sourceChannelUnits) {
        if (sourceChannelUnits == null || sourceChannelUnits.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = SOURCE_UNITS_PATTERN.matcher(sourceChannelUnits);
        if (matcher.matches()) {
            String magnitudePart = matcher.group("magnitude");
            if (magnitudePart == null) {
                magnitudePart = "";
            }
            return Magnitude.getMagnitudeFromPrefix(magnitudePart);
        }
        return null;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String cmdStr) {
        String trimmedParamString = paramStr.trim();
        Matcher scalarMatcher = SCALAR_MULTIPLIER_PARAMETERS_PATTERN.matcher(trimmedParamString);
        if (scalarMatcher.matches()) {
            Optional<Object> magnitudeOptional;
            String baseUnitPart = scalarMatcher.group("baseUnit").trim();
            String multiplierPart = scalarMatcher.group("scalar").trim();
            String sourceChannelPart = scalarMatcher.group("channel");
            String sourceChannel = ChannelFunctionIF.toChannelReferenceString(sourceChannelPart);
            String outputChannel = ChannelFunctionIF.toChannelReferenceString(chanName);
            if (sourceChannel.isEmpty() || outputChannel.isEmpty()) {
                return null;
            }
            String magnitudePart = scalarMatcher.group("magnitude");
            if (magnitudePart != null) {
                magnitudeOptional = Magnitude.fromParameter(magnitudePart.trim());
                if (!magnitudeOptional.isPresent() && !magnitudePart.equalsIgnoreCase("auto")) {
                    return null;
                }
            } else {
                magnitudeOptional = Optional.empty();
            }
            try {
                double multiplier = Double.parseDouble(multiplierPart);
                return magnitudeOptional.map(magnitude -> new Channel_ScalarMultiplier(outputChannel, sourceChannel, multiplier, multiplierPart, baseUnitPart, cmdStr, (Magnitude)((Object)magnitude))).orElseGet(() -> new Channel_ScalarMultiplier(outputChannel, sourceChannel, multiplier, multiplierPart, baseUnitPart, cmdStr));
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        Matcher channelMultiplierMatcher = UNIT_AND_OTHER_PATTERN.matcher(trimmedParamString);
        if (channelMultiplierMatcher.matches()) {
            String outputChannel;
            Optional<Object> magnitudeOptional;
            String baseUnitPart = channelMultiplierMatcher.group("baseUnit").trim();
            String channelsPart = channelMultiplierMatcher.group("channels").trim();
            String[] channelReferenceStrings = ChannelFunctionIF.toChannelReferenceStrings(channelsPart);
            if (channelReferenceStrings == null || channelReferenceStrings.length < 2) {
                return null;
            }
            String magnitudePart = channelMultiplierMatcher.group("magnitude");
            if (magnitudePart != null) {
                magnitudeOptional = Magnitude.fromParameter(magnitudePart);
                if (!magnitudeOptional.isPresent() && !magnitudePart.equalsIgnoreCase("auto")) {
                    return null;
                }
            } else {
                magnitudeOptional = Optional.empty();
            }
            if ((outputChannel = ChannelFunctionIF.toChannelReferenceString(chanName)).isEmpty()) {
                return null;
            }
            return magnitudeOptional.map(magnitude -> new Channel_ChannelMultiplier(outputChannel, baseUnitPart, cmdStr, (Magnitude)((Object)magnitude), channelReferenceStrings)).orElseGet(() -> new Channel_ChannelMultiplier(outputChannel, baseUnitPart, cmdStr, channelReferenceStrings));
        }
        return null;
    }

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

