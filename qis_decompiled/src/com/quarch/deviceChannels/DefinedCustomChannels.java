/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.ChannelFunctionsList;

public class DefinedCustomChannels {
    private final ChannelFunctionsList channelFunctionsList = new ChannelFunctionsList();
    private final Map<String, ChannelFunctionIF> definedMap = new LinkedHashMap<String, ChannelFunctionIF>();
    private boolean includeDimension = false;

    public DefinedCustomChannels() {
    }

    public DefinedCustomChannels(boolean includeDimension) {
        this.includeDimension = includeDimension;
    }

    public boolean addChannel(List<String> response, String cmdStr) {
        StringToParse stringToParse = new StringToParse(cmdStr);
        String destChan = this.makeChan(stringToParse);
        String functionStr = this.getCmdFunction(stringToParse);
        String paramStr = this.getParamStr(stringToParse);
        String postAmble = this.getDimension(stringToParse);
        if (destChan.isEmpty() || functionStr.isEmpty() || paramStr.isEmpty()) {
            return false;
        }
        if (this.includeDimension && postAmble.isEmpty()) {
            return false;
        }
        ChannelFunctionIF cf = this.channelFunctionsList.createFromStrings(destChan, functionStr, paramStr, postAmble, cmdStr);
        if (cf == null) {
            return false;
        }
        ChannelFunctionIF existing = this.channelFunctionExists(cf);
        if (existing != null) {
            this.updateChannelFunction(existing, cf);
        } else {
            String key = this.makeCmdKey(cmdStr);
            this.addChannel(key, cf);
        }
        return true;
    }

    public boolean deleteChannel(List<String> response, String cmdStr) {
        String key = this.makeCmdKey(cmdStr);
        ChannelFunctionIF dc = this.getDefinedMap().get(key);
        if (dc == null) {
            response.add("FAIL: Channel Not Found");
        } else {
            this.getDefinedMap().remove(key);
            response.add("OK");
        }
        return true;
    }

    public boolean deleteAll(List<String> response) {
        this.getDefinedMap().clear();
        response.add("OK");
        return true;
    }

    private void addChannel(String key, ChannelFunctionIF cf) {
        this.getDefinedMap().put(key, cf);
    }

    private void updateChannelFunction(ChannelFunctionIF existing, ChannelFunctionIF cf) {
    }

    private ChannelFunctionIF channelFunctionExists(ChannelFunctionIF cf) {
        return null;
    }

    private String makeChan(StringToParse stringToParse) {
        boolean done = false;
        StringBuilder sb = new StringBuilder();
        if (stringToParse.getToken().toLowerCase().equals("chan")) {
            stringToParse.incIdx();
            if (stringToParse.getToken().toLowerCase().equals("(")) {
                sb.append("chan(");
                int paranthCount = 1;
                stringToParse.incIdx();
                sb.append(stringToParse.getToken().trim());
                while (stringToParse.incIdx() != -1 && !done) {
                    String token = stringToParse.getToken().trim();
                    if (token.isEmpty()) continue;
                    if (token.equals("(")) {
                        ++paranthCount;
                        sb.append(token);
                    } else if (token.equals(")")) {
                        --paranthCount;
                        sb.append(token);
                    } else {
                        sb.append(",");
                        sb.append(token);
                    }
                    done = paranthCount == 0;
                }
            }
        }
        if (!done) {
            return "";
        }
        return sb.toString();
    }

    private String makeCmdKey(String cmdStr) {
        try {
            if (cmdStr.startsWith("(")) {
                int pos = cmdStr.indexOf(")");
                if (pos >= 0) {
                    return cmdStr.substring(0, pos);
                }
            } else {
                int pos = cmdStr.indexOf(" ");
                if (pos == -1 && cmdStr.endsWith(")")) {
                    pos = cmdStr.length();
                }
                if (pos > 0) {
                    String str = cmdStr.substring(0, pos).trim().replaceAll("\\s+", "");
                    return str;
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            return "";
        }
        return "";
    }

    private String getCmdFunction(StringToParse stringToParse) {
        while (stringToParse.incIdx() != -1) {
            String token = stringToParse.getToken().trim();
            if (token.equals(")")) {
                return "";
            }
            if (token.isEmpty() || token.equals("(")) continue;
            return token;
        }
        return "";
    }

    private String getParamStr(StringToParse stringToParse) {
        int paranthCount = -1;
        StringBuilder sb = new StringBuilder();
        while (stringToParse.incIdx() != -1 && paranthCount != 0) {
            String token = stringToParse.getToken().trim();
            if (token.isEmpty()) continue;
            if (token.equals("(")) {
                paranthCount = paranthCount == -1 ? 1 : ++paranthCount;
                sb.append("(");
                continue;
            }
            if (token.equals(")")) {
                --paranthCount;
                sb.append(")");
                continue;
            }
            sb.append(token);
            if (stringToParse.peekNextToken().equals(")") || stringToParse.peekNextToken().equals("(")) continue;
            sb.append(",");
        }
        if (paranthCount == 0) {
            return sb.toString();
        }
        return "";
    }

    private String getDimension(StringToParse stringToParse) {
        StringBuilder sb = new StringBuilder();
        while (stringToParse.incIdx() != -1) {
            String token = stringToParse.getToken().trim();
            if (token.isEmpty()) continue;
            sb.append(token);
            sb.append(",");
        }
        return sb.toString();
    }

    public Map<String, ChannelFunctionIF> getDefinedMap() {
        return this.definedMap;
    }

    public void listChannelDefinitions(List<String> response) {
        response.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        response.add("<functionDefinitions>");
        this.channelFunctionsList.definitionsXMLtoSB(response);
        response.add("</functionDefinitions>");
    }

    class StringToParse {
        public final String srcString;
        public int pIdx = 0;
        public final String[] parts;

        public StringToParse(String srcStr) {
            this.srcString = srcStr;
            String paddedCmdStr = srcStr.replaceAll("\\(", " ( ");
            paddedCmdStr = paddedCmdStr.replaceAll("\\)", " ) ");
            this.parts = paddedCmdStr.split("\\s|,");
        }

        public String getToken() {
            return this.parts[this.pIdx];
        }

        public int incIdx() {
            ++this.pIdx;
            if (this.pIdx >= this.parts.length) {
                --this.pIdx;
                return -1;
            }
            return this.pIdx;
        }

        public String peekNextToken() {
            if (this.pIdx + 1 >= this.parts.length) {
                return "";
            }
            return this.parts[this.pIdx + 1];
        }
    }
}

