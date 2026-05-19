/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.deviceChannels.PowerCalcPair;
import src.com.quarch.deviceMOM.FixtureChannel;
import src.com.quarch.deviceMOM.FixtureChannelList;
import src.com.quarch.devices.BaseStreamDevicePPM;

public class StreamHeader {
    private final BaseStreamDevicePPM.BasePPM basePPM;
    static final int fixedHeaderByteSizeV1 = 20;
    static final int fixedHeaderByteSizeV2 = 12;
    static final int headerChannelSizeV2 = 8;
    static final int headerGroupSizeV2 = 8;
    final int deviceHeaderVersion;
    int average;
    private boolean isValid = false;
    private boolean isInitialisationComplete = false;
    private short sampleRateBase;
    private short sampleRateExponent;
    private short channelCount;
    private short channelTableSizeBytes;
    private short stringTableSize;
    private long timeBaseNS;
    private double calcedSampleFrequency;
    private int devicePerioduS;
    private final ChannelGroupList enabledGroupChannelsList = new ChannelGroupList();
    private final ChannelGroupList allGroupChannelsList = new ChannelGroupList();
    private final List<FixtureGroupV2> groupList = new ArrayList<FixtureGroupV2>();
    private int bytesConsumed;
    private int fastestSampleRateGroup;
    private int fastestSampleTimeuS;
    private long fastestSampleTimenS;
    private boolean multiRate;
    public Exception lastException;
    private boolean noEnabledGroups = false;

    public StreamHeader(BaseStreamDevicePPM.BasePPM basePPM, byte[] data) {
        this.basePPM = basePPM;
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        this.deviceHeaderVersion = bb.get();
        if (this.deviceHeaderVersion == 2) {
            bb.get();
            this.headerV2(basePPM, data, bb);
        } else {
            this.headerV1(basePPM, data, bb);
        }
        if (!this.isValid()) {
            return;
        }
        CountDownLatch allheadersReadSignal = basePPM.getParent().allheadersReadSignal;
        int totalGroups = this.groupList.size();
        basePPM.getParent().totalGroups.set(totalGroups);
        if (allheadersReadSignal != null) {
            basePPM.getParent().headerReceived(totalGroups);
            allheadersReadSignal.countDown();
            try {
                allheadersReadSignal.await();
                basePPM.getParent().allHeadersWaitDone();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getEnabledGroupCount() {
        int retVal = 0;
        for (FixtureGroupV2 group : this.groupList) {
            if (group.getEnabledChannelCount() <= 0) continue;
            ++retVal;
        }
        return retVal;
    }

    private void headerV1(BaseStreamDevicePPM.BasePPM basePPM, byte[] data, ByteBuffer bb) {
        this.average = bb.get();
        short tmp = bb.getShort();
        tmp = bb.getShort();
        tmp = bb.getShort();
        tmp = bb.getShort();
        this.sampleRateBase = bb.getShort();
        this.sampleRateExponent = bb.getShort();
        this.channelCount = bb.getShort();
        this.channelTableSizeBytes = bb.getShort();
        this.stringTableSize = bb.getShort();
        this.timeBaseNS = (long)((double)this.sampleRateBase * Math.pow(10.0, this.sampleRateExponent));
        this.calcedSampleFrequency = 1.0 / (double)this.timeBaseNS;
        this.devicePerioduS = (int)(Math.pow(2.0, this.average) * (this.calcedSampleFrequency * 1000000.0));
        basePPM.setDevicePerioduS(this.devicePerioduS);
        String namePrefix = basePPM.getParent().getExtendedChannelNamePrefix();
        for (int i = 0; i < this.channelCount; ++i) {
            FixtureChannel fc = new FixtureChannel(i, data, 20 + i * 6, 20 + this.channelTableSizeBytes, basePPM.getParent().getGroupOffset(), namePrefix);
            this.enabledGroupChannelsList.addChannel(fc);
            this.allGroupChannelsList.addChannel(fc);
        }
        this.bytesConsumed = 20 + this.channelTableSizeBytes + this.stringTableSize;
        int currentGroupId = -1;
        for (FixtureChannelList gChs : this.enabledGroupChannelsList.getList()) {
            short channelCount = (short)gChs.size();
            FixtureGroupV2 fg = new FixtureGroupV2(channelCount, this.sampleRateBase, this.sampleRateExponent, (short)this.average);
            this.groupList.add(fg);
            for (FixtureChannel fc : gChs.list) {
                int thisGroup = fc.getGroupId();
                if (thisGroup != currentGroupId) {
                    currentGroupId = thisGroup;
                }
                fc.setGroup(fg);
            }
        }
        this.setValid(true);
    }

    private void headerV2(BaseStreamDevicePPM.BasePPM basePPM, byte[] data, ByteBuffer bb) {
        try {
            short internalStatus = bb.getShort();
            short fixturePartNo = bb.getShort();
            short fixtureVersionNo = bb.getShort();
            this.stringTableSize = bb.getShort();
            int groupCount = bb.getShort();
            int channelTotalCount = 0;
            String namePrefix = basePPM.getParent().getExtendedChannelNamePrefix();
            for (int group = 0; group < groupCount; ++group) {
                FixtureGroupV2 fixtureGroup = new FixtureGroupV2(bb);
                this.groupList.add(fixtureGroup);
                channelTotalCount = (short)(channelTotalCount + fixtureGroup.channelCount);
            }
            int stringTableStart = 12;
            stringTableStart += this.groupList.size() * 8;
            stringTableStart += channelTotalCount * 8;
            AtomicInteger groupOffset = basePPM.getParent().getGroupOffset();
            for (int groupIdx = 0; groupIdx < groupCount; ++groupIdx) {
                FixtureGroupV2 enclosingFixtureGroupV2;
                FixtureGroupV2 group = enclosingFixtureGroupV2 = this.groupList.get(groupIdx);
                int v2ChannelCount = group.getChannelCount();
                for (int chan = 0; chan < v2ChannelCount; ++chan) {
                    FixtureChannel fc = new FixtureChannel(chan, 0, data, bb, groupIdx, stringTableStart, this.stringTableSize, group, groupOffset, namePrefix);
                    if (fc.isEnabled()) {
                        enclosingFixtureGroupV2.effectiveGroupIdx = fc.getModifiedGroupId();
                        enclosingFixtureGroupV2.incrementEnabledChannelCount();
                        this.enabledGroupChannelsList.addChannel(fc);
                    }
                    this.allGroupChannelsList.addChannel(fc);
                }
                channelTotalCount = (short)(channelTotalCount + v2ChannelCount);
            }
            if (this.enabledGroupChannelsList.list.size() == 0) {
                this.setNoEnabledGroups(true);
                return;
            }
            this.bytesConsumed = stringTableStart + this.stringTableSize;
            int fastestGroup = this.getFastestGroup();
            this.setFastestSampleRateInfo(fastestGroup);
            this.devicePerioduS = this.getFastestSampleTimeuS();
            this.calcedSampleFrequency = 1.0 / (double)this.devicePerioduS;
            this.timeBaseNS = this.getFastestSampleTimenS();
            basePPM.setDevicePerioduS(this.devicePerioduS);
            this.setValid(true);
        }
        catch (Exception e) {
            this.lastException = e;
        }
    }

    private void setNoEnabledGroups(boolean b) {
        this.noEnabledGroups = true;
    }

    public boolean getNoEnabledGroups() {
        return this.noEnabledGroups;
    }

    private int getFastestGroup() {
        long fastestSample = Long.MAX_VALUE;
        int fastestGroup = -1;
        for (int i = 0; i < this.enabledGroupChannelsList.getGroupIdCount(); ++i) {
            FixtureChannelList fcl = this.enabledGroupChannelsList.getList().get(i);
            long deviceSamplePeriod_nS = fcl.getDeviceSamplePeriod_nS();
            if (deviceSamplePeriod_nS >= fastestSample) continue;
            fastestSample = deviceSamplePeriod_nS;
            fastestGroup = i;
        }
        return fastestGroup;
    }

    public List<FixtureChannelList> getEnabledGroupChannelsList() {
        return this.enabledGroupChannelsList.getList();
    }

    public int getEnabledGroupIdCount() {
        return this.enabledGroupChannelsList.getGroupIdCount();
    }

    public FixtureChannelList getEnabledChannelsList(int groupId) {
        return this.enabledGroupChannelsList.getChannelsList(groupId);
    }

    public FixtureChannelList getEnabledChannelsList() {
        return this.enabledGroupChannelsList.getChannelsList(0);
    }

    public int getBytesConsumed() {
        return this.bytesConsumed;
    }

    public void addChannelStrings(List<String> headerStrs, String name, String group, String units, long maxValue, int dataPosition) {
        headerStrs.add("<channel>");
        headerStrs.add("<name>" + name + "</name>");
        headerStrs.add("<group>" + group + "</group>");
        headerStrs.add("<units>" + units + "</units>");
        headerStrs.add("<maxTValue>" + maxValue + "</maxTValue>");
        headerStrs.add("<dataPosition>" + Integer.toString(dataPosition) + "</dataPosition>");
        headerStrs.add("</channel>");
    }

    public List<String> getAsXML() {
        long vMax = (int)Math.pow(2.0, 14.0);
        long cMax = (int)Math.pow(2.0, 24.0);
        long pMax = vMax * cMax / 1000L;
        LinkedList<String> retVal = new LinkedList<String>();
        StreamHeader.setHeaderPreamble(retVal);
        String mainPeriodStr = this.basePPM.isResamplingActive() ? Integer.toString(this.basePPM.getRequestedResampleuS()) + "us" : Integer.toString(this.devicePerioduS) + "us";
        if (this.deviceHeaderVersion == 1) {
            this.setMultiRate(false);
            retVal.add("<version>V3</version>");
            retVal.add("<mode>Single Rate</mode>");
            retVal.add("<devicePeriod>" + this.devicePerioduS + "us</devicePeriod>");
            retVal.add("<mainPeriod>" + mainPeriodStr + "</mainPeriod>");
            retVal.add("<legacyVersion>" + this.deviceHeaderVersion + "</legacyVersion>");
            retVal.add("<legacyFormat>" + this.channelCount + "</legacyFormat>");
            retVal.add("<legacyAverage>" + this.average + "</legacyAverage>");
        }
        if (this.deviceHeaderVersion == 2) {
            this.setMultiRate(true);
            StreamHeader.setV3MultiRatePreamble(this, retVal);
            StreamHeader.setGroupsPreamble(retVal);
            this.setMultiRateGroups(retVal);
            StreamHeader.setCloseGroupsPreamble(retVal);
        }
        this.setChannels(retVal);
        StreamHeader.setCloseHeaderPreamble(retVal);
        return retVal;
    }

    public void setChannels(List<String> retVal) {
        int skippedGroups = 0;
        for (FixtureChannelList chanList : this.enabledGroupChannelsList.getGroupChannelsList()) {
            if (chanList.isAllDisabled()) continue;
            StreamHeader.setChannelsPreamble(retVal);
            int modifiedGroupId = chanList.get(0).getModifiedGroupId() - skippedGroups;
            retVal.add("<groupId>" + modifiedGroupId + "</groupId>");
            int position = 1;
            for (FixtureChannel channel : chanList.list) {
                retVal.add("<channel>");
                retVal.addAll(channel.getAsXML(position++));
                retVal.add("</channel>");
            }
            String namePrefix = this.basePPM.getParent().getExtendedChannelNamePrefix();
            if (chanList.powerCalcPairList.size() > 0) {
                for (PowerCalcPair powerCalcPair : chanList.powerCalcPairList) {
                    this.addChannelStrings(retVal, namePrefix + powerCalcPair.getNameString(), powerCalcPair.getGroupname(), powerCalcPair.getUnitsString(), powerCalcPair.getPMax(), position++);
                }
                if (this.basePPM.isEnableTotalPowerCalc()) {
                    this.addChannelStrings(retVal, namePrefix + "Tot", chanList.powerCalcPairList.get(0).getGroupname(), chanList.powerCalcPairList.get(0).getUnitsString(), PowerCalcPair.getPMax(chanList.powerCalcPairList), position++);
                }
            }
            if (!chanList.activeCustomChannels.isEmpty()) {
                for (ChannelFunctionIF acChan : chanList.activeCustomChannels.getActiveChannelList()) {
                    this.addChannelStrings(retVal, acChan.getNameString(), acChan.getGroupname(), acChan.getUnitsString(), acChan.getMaxTValue(), position++);
                }
            }
            if (chanList.triggerEnabled) {
                this.addChannelStrings(retVal, namePrefix + "Trigger", "Digital", "(Digital)", 1L, position++);
            }
            StreamHeader.setCloseChannelsPreamble(retVal);
        }
    }

    public static void setChannelsPreamble(List<String> retVal) {
        retVal.add("<channels>");
    }

    public static void setCloseChannelsPreamble(List<String> retVal) {
        retVal.add("</channels>");
    }

    public void setMultiRateGroups(List<String> retVal) {
        int groupIdx = this.basePPM.getParent().getGroupOffsetValue();
        for (FixtureGroupV2 group : this.groupList) {
            retVal.add("<group>");
            retVal.add("<groupId>" + groupIdx + "</groupId>");
            String groupIdStr = Integer.toString(groupIdx);
            if (this.basePPM.isResamplingActive(groupIdStr)) {
                group.asXMLChannelCount(retVal);
                this.basePPM.asXMLSampleRateInfo(groupIdStr, retVal);
            } else {
                group.asXML(retVal);
            }
            retVal.add("</group>");
            ++groupIdx;
        }
    }

    public static void setHeaderPreamble(List<String> retVal) {
        retVal.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        retVal.add("<header>");
    }

    public static void setCloseHeaderPreamble(List<String> retVal) {
        retVal.add("</header>");
    }

    public static void setV3MultiRatePreamble(StreamHeader headerRef, List<String> retVal) {
        retVal.add("<version>V3</version>");
        retVal.add("<mode>Multi Rate</mode>");
        if (headerRef == null) {
            retVal.add("<mainPeriod>?</mainPeriod>");
        } else {
            String mainPeriodStr = headerRef.basePPM.isResamplingActive() ? Integer.toString(headerRef.basePPM.getRequestedResampleuS()) + "us" : Integer.toString(headerRef.devicePerioduS) + "us";
            retVal.add("<mainPeriod>" + mainPeriodStr + "</mainPeriod>");
        }
    }

    public static void setGroupsPreamble(List<String> retVal) {
        retVal.add("<groups>");
    }

    public static void setCloseGroupsPreamble(List<String> retVal) {
        retVal.add("</groups>");
    }

    boolean isValid() {
        return this.isValid;
    }

    void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isInitialisationComplete() {
        return this.isInitialisationComplete;
    }

    public void setInitialisationComplete(boolean isInitialisationComplete) {
        this.isInitialisationComplete = isInitialisationComplete;
    }

    private void setFastestSampleRateInfo(int fastestSampleRateGroup) {
        this.setFastestSampleRateGroup(fastestSampleRateGroup);
        FixtureChannelList fcl = this.enabledGroupChannelsList.getList().get(fastestSampleRateGroup);
        long deviceSamplePeriod_nS = fcl.getDeviceSamplePeriod_nS();
        this.setFastestSampleTimenS(deviceSamplePeriod_nS);
        this.setFastestSampleTimeuS((int)(deviceSamplePeriod_nS / 1000L));
    }

    private void setFastestSampleTimenS(long fastestSampleTimenS) {
        this.fastestSampleTimenS = fastestSampleTimenS;
    }

    public long getFastestSampleTimenS() {
        return this.fastestSampleTimenS;
    }

    public int getFastestSampleTimeuS() {
        return this.fastestSampleTimeuS;
    }

    private void setFastestSampleTimeuS(int fastestSampleTimeuS) {
        this.fastestSampleTimeuS = fastestSampleTimeuS;
    }

    public int getFastestSampleRateGroup() {
        return this.fastestSampleRateGroup;
    }

    private void setFastestSampleRateGroup(int fastestSampleRateGroup) {
        this.fastestSampleRateGroup = fastestSampleRateGroup;
    }

    public boolean isMultiRate() {
        return this.multiRate;
    }

    private void setMultiRate(boolean multiRate) {
        this.multiRate = multiRate;
    }

    public boolean isGroupValid(String groupStr) {
        int groupId = this.getNumericGroupId(groupStr);
        return groupId != -1;
    }

    private int getNumericGroupId(String gStr) {
        int groupId;
        try {
            groupId = Integer.parseInt(gStr);
        }
        catch (Exception e) {
            groupId = -1;
        }
        return groupId;
    }

    public LinkedHashMap<String, String> getAllGroupsDetailsMap() {
        LinkedHashMap<String, String> retVal = new LinkedHashMap<String, String>();
        retVal.put("Device Header Version", Integer.toString(this.deviceHeaderVersion));
        retVal.put("timeBaseNS", Long.toString(this.timeBaseNS));
        retVal.put("calcedSampleFrequency", Double.toString(this.calcedSampleFrequency));
        retVal.put("devicePerioduS", Integer.toString(this.devicePerioduS));
        if (this.deviceHeaderVersion == 1) {
            retVal.put("Mode", "Single Rate");
        } else {
            retVal.put("Mode", "Multi Rate");
        }
        for (FixtureChannelList chanList : this.allGroupChannelsList.getGroupChannelsList()) {
            boolean allDisabled = true;
            for (FixtureChannel channel : chanList.list) {
                if (!channel.isEnabled()) continue;
                allDisabled = false;
                break;
            }
            String groupTextDisabled = allDisabled ? "(Disabled) " : "";
            retVal.put(groupTextDisabled + "Group Id: " + Integer.toString(chanList.get(0).getGroupId()), chanList.list.get(0).getGroupString());
            for (FixtureChannel channel : chanList.list) {
                String channelTextDisabled = channel.isEnabled() ? "" : "(Disabled) ";
                retVal.put(channelTextDisabled + channel.getNameString() + " " + channel.getUnitsString(), "Width: " + channel.getDataWidthBits());
            }
        }
        return retVal;
    }

    public int[] getGroupByteSizes() {
        int[] retVal = new int[this.enabledGroupChannelsList.getGroupChannelsList().size()];
        int groupIdx = 0;
        for (FixtureChannelList chanList : this.enabledGroupChannelsList.getGroupChannelsList()) {
            int bitSize = 0;
            for (FixtureChannel channel : chanList.list) {
                bitSize += channel.getDataWidthBits();
            }
            int bytes = bitSize / 8;
            if (bitSize % 8 != 0) {
                // empty if block
            }
            retVal[groupIdx] = ++bytes;
            ++groupIdx;
        }
        return retVal;
    }

    public class ChannelGroupList {
        private final List<FixtureChannelList> list = new ArrayList<FixtureChannelList>();

        public List<FixtureChannelList> getList() {
            return this.list;
        }

        public int getGroupIdCount() {
            return this.getList().size();
        }

        public void addGroupList(FixtureChannelList fcl) {
            this.getList().add(fcl);
        }

        public List<FixtureChannelList> getGroupChannelsList() {
            return this.getList();
        }

        public void addChannel(FixtureChannel fc) {
            int groupId = fc.getGroupId();
            while (groupId >= this.getGroupIdCount()) {
                FixtureChannelList fcl = new FixtureChannelList(fc.getGroup(), StreamHeader.this.devicePerioduS);
                this.addGroupList(fcl);
            }
            this.getChannelsList(groupId).add(fc);
        }

        public FixtureChannelList getChannelsList(int groupId) {
            return this.getList().get(groupId);
        }
    }

    class FixtureGroupV2 {
        private short channelCount;
        private final short sampleRateBase;
        private final short sampleRateExponent;
        private final short averagingRate;
        private int enabledChannelCount = 0;
        public int effectiveGroupIdx;

        public FixtureGroupV2(ByteBuffer bb) {
            this.channelCount = bb.getShort();
            this.sampleRateBase = bb.getShort();
            this.sampleRateExponent = bb.getShort();
            this.averagingRate = bb.getShort();
        }

        public FixtureGroupV2(short channelCount, short sampleRateBase, short sampleRateExponent, short averagingRate) {
            this.channelCount = channelCount;
            this.sampleRateBase = sampleRateBase;
            this.sampleRateExponent = sampleRateExponent;
            this.averagingRate = averagingRate;
        }

        protected short getChannelCount() {
            return this.channelCount;
        }

        protected int getSampleRateBase() {
            int retVal;
            if (this.sampleRateBase < 0) {
                retVal = this.sampleRateBase;
                retVal &= 0xFFFF;
            } else {
                retVal = this.sampleRateBase;
            }
            return retVal;
        }

        protected short getSampleRateExponent() {
            return this.sampleRateExponent;
        }

        protected short getAveragingRate() {
            return this.averagingRate;
        }

        public void asXML(List<String> retVal) {
            this.asXMLChannelCount(retVal);
            this.asXMLSampleRateInfo(retVal);
        }

        private void asXMLSampleRateInfo(List<String> retVal) {
            retVal.add("<sampleRateBase>" + this.getSampleRateBase() + "</sampleRateBase>");
            retVal.add("<sampleRateExponent>" + this.getSampleRateExponent() + "</sampleRateExponent>");
            retVal.add("<averagingRate>" + this.getAveragingRate() + "</averagingRate>");
        }

        public void asXMLChannelCount(List<String> retVal) {
        }

        public int getEnabledChannelCount() {
            return this.enabledChannelCount;
        }

        public void resetEnabledChannelCount() {
            this.enabledChannelCount = 0;
        }

        public void incrementEnabledChannelCount() {
            ++this.enabledChannelCount;
        }
    }
}

