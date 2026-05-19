/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.deviceMOM.StreamHeader;

class FixtureChannel {
    public static final String DIGITAL_GroupString = "Digital";
    public static final int channelDefinitionSizeInBytes = 6;
    private final short version;
    private String name;
    private String units;
    private String groupString;
    private final int position;
    private final int groupId;
    private int rawDataPrecision;
    private final byte dataWidth;
    private final short nameStrIdx;
    private final short unitstrIdx;
    private final short channelFlags;
    private double dataPrecision;
    private StreamHeader.FixtureGroupV2 group;
    private AtomicInteger groupOffset;
    private static final String[] unitModifiers = new String[]{"p", "n", "u", "m", "", "k", "M", "G"};

    public FixtureChannel(int position, byte[] data, int channelOffset, int stringTableStartOffset, AtomicInteger groupOffset, String namePrefix) {
        this.version = 0;
        this.group = null;
        ByteBuffer bb = ByteBuffer.wrap(data, channelOffset, 6);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        this.position = position;
        this.nameStrIdx = bb.getShort();
        this.unitstrIdx = bb.getShort();
        this.dataWidth = bb.get();
        byte tempByte = bb.get();
        this.name = namePrefix + this.getStringAt(data, stringTableStartOffset + this.nameStrIdx);
        this.groupOffset = groupOffset;
        this.groupId = tempByte >> 4 & 0xF;
        this.channelFlags = (short)-1;
        if (this.unitstrIdx < 0 || this.getStringAt(data, stringTableStartOffset + this.unitstrIdx).endsWith("D")) {
            this.units = "(Digital)";
            this.groupString = DIGITAL_GroupString;
            this.rawDataPrecision = 0;
            this.dataPrecision = 1.0;
        } else {
            this.units = this.getStringAt(data, stringTableStartOffset + this.unitstrIdx);
            this.groupString = this.units.endsWith("A") ? "current" : (this.units.endsWith("V") ? "voltage" : "NA");
            tempByte = (byte)(tempByte & 0xF);
            this.rawDataPrecision = (tempByte & 8) > 0 ? (tempByte & 7) - 8 : tempByte & 7;
            this.dataPrecision = Math.pow(1000.0, this.rawDataPrecision);
        }
    }

    public FixtureChannel(int chan, short v2ChannelTotalCount, byte[] data, ByteBuffer bb, int groupIdx, int stringTableStartOffset, short stringTableSize, StreamHeader.FixtureGroupV2 groupV2, AtomicInteger groupOffset, String namePrefix) {
        this.version = (short)2;
        this.group = groupV2;
        this.position = chan + v2ChannelTotalCount;
        this.nameStrIdx = bb.getShort();
        this.unitstrIdx = bb.getShort();
        this.dataWidth = bb.get();
        byte tempByte = bb.get();
        this.name = namePrefix + this.getStringAt(data, stringTableStartOffset + this.nameStrIdx);
        this.groupOffset = groupOffset;
        this.groupId = tempByte >> 4 & 0xF;
        this.channelFlags = bb.getShort();
        if (this.unitstrIdx < 0 || this.getStringAt(data, stringTableStartOffset + this.unitstrIdx).endsWith("D")) {
            this.units = "(Digital)";
            this.groupString = DIGITAL_GroupString;
            this.rawDataPrecision = 0;
            this.dataPrecision = 1.0;
        } else {
            this.units = this.getStringAt(data, stringTableStartOffset + this.unitstrIdx);
            this.groupString = this.units.endsWith("A") ? "current" : (this.units.toUpperCase().endsWith("DV") ? "dV" : (this.units.endsWith("V") ? "voltage" : "NA"));
            tempByte = (byte)(tempByte & 0xF);
            this.rawDataPrecision = (tempByte & 8) > 0 ? (tempByte & 7) - 8 : tempByte & 7;
            this.dataPrecision = Math.pow(1000.0, this.rawDataPrecision);
        }
    }

    private String getStringAt(byte[] data, int startIdx) {
        StringBuilder sb = new StringBuilder();
        byte cbyte = data[startIdx++];
        while (cbyte != 0) {
            sb.append((char)cbyte);
            cbyte = data[startIdx++];
        }
        return sb.toString();
    }

    public List<String> getAsXML(int position) {
        LinkedList<String> retVal = new LinkedList<String>();
        retVal.add("<name>" + this.name + "</name>");
        retVal.add("<group>" + this.getGroupString() + "</group>");
        if (this.units.equals("D")) {
            retVal.add("<units>" + this.getSIPrefix() + "(Digital)" + "</units>");
        } else {
            retVal.add("<units>" + this.getSIPrefix() + this.units + "</units>");
        }
        if (this.version == 2) {
            retVal.add("<flags>" + this.channelFlags + "</flags>");
        }
        retVal.add("<maxTValue>" + this.getMaxTValueStr() + "</maxTValue>");
        retVal.add("<dataPosition>" + position + "</dataPosition>");
        return retVal;
    }

    public String getUnitsString() {
        if (this.units.equals("D")) {
            return this.getSIPrefix() + "(Digital)";
        }
        return this.getSIPrefix() + this.units;
    }

    public String getMaxTValueStr() {
        return Long.toString(this.getMaxTValue());
    }

    public long getMaxTValue() {
        return Math.round(Math.pow(2.0, this.getDataWidthBits())) - 1L;
    }

    private String getSIPrefix() {
        int idx = this.rawDataPrecision + 4;
        if (idx < 0 || idx >= unitModifiers.length) {
            return "?";
        }
        return unitModifiers[idx];
    }

    protected int getGroupId() {
        return this.groupId;
    }

    protected int getModifiedGroupId() {
        return this.groupId + this.groupOffset.get();
    }

    byte getDataWidthBits() {
        return this.dataWidth;
    }

    String getGroupString() {
        return this.groupString;
    }

    public String getNameString() {
        return this.name;
    }

    public StreamHeader.FixtureGroupV2 getGroup() {
        return this.group;
    }

    protected void setGroup(StreamHeader.FixtureGroupV2 fg) {
        this.group = fg;
    }

    public String getChannelKey() {
        String retVal = this.getNameString() + ":" + this.getGroupString();
        return retVal;
    }

    public boolean isEnabled() {
        return (this.channelFlags & 1) != 0;
    }
}

