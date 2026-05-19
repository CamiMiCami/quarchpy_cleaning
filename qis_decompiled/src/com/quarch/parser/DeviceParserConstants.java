/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.parser;

public interface DeviceParserConstants {
    public static final int EOF = 0;
    public static final int PLUS = 5;
    public static final int MINUS = 6;
    public static final int MULTIPLY = 7;
    public static final int DIVIDE = 8;
    public static final int OBRK = 9;
    public static final int CBRK = 10;
    public static final int COMMA = 11;
    public static final int DOLLAR = 12;
    public static final int QUOTED_STRING = 13;
    public static final int QUARCHDEVICECOMMSID = 14;
    public static final int QUARCHSERIALNUMBER = 15;
    public static final int CMD_CREATEDEVICE = 16;
    public static final int NEWDEVICENAME = 17;
    public static final int CMD_NEWDEVICE = 18;
    public static final int DEVICEDESCRIPTION = 19;
    public static final int BASEDEVICES = 20;
    public static final int CMD_DEVICEREF = 21;
    public static final int DEVICE_ID = 22;
    public static final int DEVICECHANPREFIX = 23;
    public static final int IDENTIFIER = 24;
    public static final int LETTER = 25;
    public static final int DIGIT = 26;
    public static final int NAME = 27;
    public static final int FUNCTION = 28;
    public static final int DEFAULT = 0;
    public static final String[] tokenImage = new String[]{"<EOF>", "\" \"", "\"\\t\"", "\"\\r\"", "\"\\n\"", "\"+\"", "\"-\"", "\"*\"", "\"/\"", "\"(\"", "\")\"", "\",\"", "\"$\"", "<QUOTED_STRING>", "<QUARCHDEVICECOMMSID>", "<QUARCHSERIALNUMBER>", "\"create device\"", "<NEWDEVICENAME>", "<CMD_NEWDEVICE>", "<DEVICEDESCRIPTION>", "<BASEDEVICES>", "<CMD_DEVICEREF>", "<DEVICE_ID>", "<DEVICECHANPREFIX>", "<IDENTIFIER>", "<LETTER>", "<DIGIT>", "<NAME>", "<FUNCTION>"};
}

