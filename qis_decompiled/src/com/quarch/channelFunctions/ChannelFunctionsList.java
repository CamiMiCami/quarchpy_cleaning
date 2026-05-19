/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.channelFunctions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.deviceChannels.Channel_ActivePower;
import src.com.quarch.deviceChannels.Channel_ApparentPower;
import src.com.quarch.deviceChannels.Channel_Frequency;
import src.com.quarch.deviceChannels.Channel_InstantaneousPower;
import src.com.quarch.deviceChannels.Channel_Multiplier;
import src.com.quarch.deviceChannels.Channel_Phase;
import src.com.quarch.deviceChannels.Channel_PowerFactor;
import src.com.quarch.deviceChannels.Channel_RMS;
import src.com.quarch.deviceChannels.Channel_ReactivePower;
import src.com.quarch.deviceChannels.Channel_SINEWave;
import src.com.quarch.deviceChannels.Channel_Sum;

public class ChannelFunctionsList {
    private static Map<String, FunctionFromString> functionNameMap = new LinkedHashMap<String, FunctionFromString>();
    private static boolean isInitialised;

    public ChannelFunctionIF createFromStrings(String chanName, String functionStr, String paramStr, String postAmble, String cmdStr) {
        FunctionFromString functionFromString = functionNameMap.get(functionStr.toLowerCase());
        if (functionFromString != null) {
            return functionFromString.decode(chanName, paramStr, postAmble, cmdStr);
        }
        return null;
    }

    public void definitionsXMLtoSB(List<String> response) {
        for (String key : functionNameMap.keySet()) {
            response.add("<function>");
            functionNameMap.get(key).definitionXMLtoSB(response);
            response.add("</function>");
        }
    }

    static {
        functionNameMap.put("rms".toLowerCase(), new RMS_FromStrings());
        functionNameMap.put("sineWave".toLowerCase(), new SINEWave_FromStrings());
        functionNameMap.put("frequency".toLowerCase(), new Frequency_FromStrings());
        functionNameMap.put("phase".toLowerCase(), new Phase_FromStrings());
        functionNameMap.put("pInstantaneous".toLowerCase(), new InstantaneousPower_FromStrings());
        functionNameMap.put("pApparent".toLowerCase(), new ApparentPower_FromStrings());
        functionNameMap.put("pActive".toLowerCase(), new ActivePower_FromStrings());
        functionNameMap.put("pReactive".toLowerCase(), new ReactivePower_FromStrings());
        functionNameMap.put("powerFactor".toLowerCase(), new PowerFactor_FromStrings());
        functionNameMap.put("sum".toLowerCase(), new Sum_FromStrings());
        functionNameMap.put("multiplier".toLowerCase(), new Multiplier_FromStrings());
        isInitialised = false;
    }

    static class Multiplier_FromStrings
    implements FunctionFromString {
        Multiplier_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_Multiplier.createFromStrings(chanName, paramStr, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_Multiplier.getDefinitionAsXML();
        }
    }

    static class Sum_FromStrings
    implements FunctionFromString {
        Sum_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_Sum.createFromStrings(chanName, paramStr, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_Sum.getDefinitionAsXML();
        }
    }

    static class PowerFactor_FromStrings
    implements FunctionFromString {
        PowerFactor_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_PowerFactor.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_PowerFactor.getDefinitionAsXML();
        }
    }

    static class ReactivePower_FromStrings
    implements FunctionFromString {
        ReactivePower_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_ReactivePower.createFromStrings(chanName, paramStr, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_ReactivePower.getDefinitionAsXML();
        }
    }

    static class ActivePower_FromStrings
    implements FunctionFromString {
        ActivePower_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_ActivePower.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_ActivePower.getDefinitionAsXML();
        }
    }

    static class ApparentPower_FromStrings
    implements FunctionFromString {
        ApparentPower_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_ApparentPower.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_ApparentPower.getDefinitionAsXML();
        }
    }

    static class InstantaneousPower_FromStrings
    implements FunctionFromString {
        InstantaneousPower_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_InstantaneousPower.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_InstantaneousPower.getDefinitionAsXML();
        }
    }

    static class Phase_FromStrings
    implements FunctionFromString {
        Phase_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_Phase.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_Phase.getDefinitionAsXML();
        }
    }

    static class Frequency_FromStrings
    implements FunctionFromString {
        Frequency_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_Frequency.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_Frequency.getDefinitionAsXML();
        }
    }

    static class SINEWave_FromStrings
    implements FunctionFromString {
        SINEWave_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_SINEWave.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_SINEWave.getDefinitionAsXML();
        }
    }

    static class RMS_FromStrings
    implements FunctionFromString {
        RMS_FromStrings() {
        }

        @Override
        public ChannelFunctionIF decode(String chanName, String paramStr, String postAmble, String cmdStr) {
            return Channel_RMS.createFromStrings(chanName, paramStr, postAmble, cmdStr);
        }

        @Override
        public List<String> getDefinitionAsXML() {
            return Channel_RMS.getDefinitionAsXML();
        }
    }

    static interface FunctionFromString {
        public ChannelFunctionIF decode(String var1, String var2, String var3, String var4);

        public List<String> getDefinitionAsXML();

        default public void definitionXMLtoSB(List<String> response) {
            response.addAll(this.getDefinitionAsXML());
        }
    }
}

