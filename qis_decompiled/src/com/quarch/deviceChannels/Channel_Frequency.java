/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import properties.AppProperties;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.CircularBuffer_HighLow_Totaliser;
import src.com.quarch.deviceChannels.CircularBuffer_int;

public class Channel_Frequency
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final String functionName = "frequency";
    private static List<String> xmlDefinition = Arrays.asList(ChannelFunctionIF.asXMLField("name", "frequency"), ChannelFunctionIF.asXMLField("returnType", "Frequency"), ChannelFunctionIF.asXMLField("description", "The frequency of the signal specified by the source channel."), "<parameters>", "<parameter>", ChannelFunctionIF.nameAsXMLField("window"), ChannelFunctionIF.typeAsXMLField("timeWindow"), ChannelFunctionIF.timeWindowAverageDescriptionAsXMLField(), "</parameter>", "<parameter>", ChannelFunctionIF.nameAsXMLField("source"), ChannelFunctionIF.typeAsXMLField("chanAny"), ChannelFunctionIF.asXMLField("description", "Any source channel."), "</parameter>", "</parameters>");
    private final String nameStr;
    private final String groupStr;
    private final String unitsString;
    private final double divisor;
    private final String origionalCmdStr;
    private final DependentChannel[] srcChannels = new DependentChannel[1];
    private long thisWindow_nS;
    private AtomicLong window_nS = new AtomicLong();
    private CircularBuffer_int overallAverage;
    private CircularBuffer_int posAverage;
    private CircularBuffer_int negAverage;
    private CircularBuffer_int highAverage;
    private CircularBuffer_int lowAverage;
    private CircularBuffer_HighLow_Totaliser posNegAverage;
    private CircularBuffer_int fallingTimeDeltaAverage;
    private CircularBuffer_int risingTimeDeltaAverage;
    private boolean firstSignChange = true;
    private boolean prefillFromBuffer = true;
    private int posRunCount = 0;
    private int negRunCount = 0;
    private int countingPosNeg = 0;
    private long activeSampleTime_nS;
    private long lastLowDataRecordCount;
    private long lastHighDataRecordCount;
    private long recordCount;
    private long prevRecordCount;
    BufferedReader debugFileReader;

    private Channel_Frequency(String chanKey, String srcChannelStr, long window_nS, String units, double divisor, String cmdStr) {
        this.nameStr = this.keyToStr(chanKey, 0);
        this.groupStr = this.keyToStr(chanKey, 1);
        for (int i = 0; i < this.srcChannels.length; ++i) {
            this.srcChannels[i] = new DependentChannel();
        }
        this.srcChannels[0].setNameKey(srcChannelStr);
        this.thisWindow_nS = window_nS;
        this.unitsString = units;
        this.divisor = divisor;
        this.origionalCmdStr = cmdStr;
    }

    public static ChannelFunctionIF createFromStrings(String chanName, String paramStr, String postAmble, String cmdStr) {
        int pIdx = 0;
        String[] parts = paramStr.split(",");
        while (parts[pIdx].isEmpty()) {
            ++pIdx;
        }
        try {
            long windowSize = ChannelFunctionIF.calcWindowSize(parts[pIdx++]);
            String srcChannelStr = ChannelFunctionIF.getChannelStr(parts, pIdx);
            String units = "Hz";
            double divisor = 1.0;
            return new Channel_Frequency(ChannelFunctionIF.getChannelStr(chanName), srcChannelStr, windowSize, units, divisor, cmdStr);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private void updateWorkingValues_2(int value) {
        this.overallAverage.offer(value);
        if (this.overallAverage.isFull()) {
            double avgVal = this.overallAverage.getValue();
            int referenceLevelLow = (int)avgVal;
            int referenceLevelHi = (int)avgVal;
            this.posNegAverage.setLowThreshold(referenceLevelLow);
            this.posNegAverage.setHiThreshold(referenceLevelHi);
            if (this.prefillFromBuffer) {
                for (int oValue : this.overallAverage.getData()) {
                    this.posNegAverage.offer(oValue);
                }
                int hiValue = this.posNegAverage.getHighValue();
                int lowValue = this.posNegAverage.getLowValue();
                if (hiValue == Integer.MIN_VALUE || lowValue == Integer.MIN_VALUE) {
                    return;
                }
                int oRecordCount = 0;
                int[] data = this.overallAverage.getData();
                int oValue = data[0];
                if (oValue >= hiValue) {
                    this.countingPosNeg = 1;
                }
                if (oValue <= lowValue) {
                    this.countingPosNeg = -1;
                }
                for (int i = 0; i < data.length; ++i) {
                    oValue = data[i];
                    ++oRecordCount;
                    int countingSign = 0;
                    if (oValue >= hiValue) {
                        ++this.posRunCount;
                        countingSign = 1;
                    }
                    if (oValue <= lowValue) {
                        ++this.negRunCount;
                        countingSign = -1;
                    }
                    if (countingSign == 0 || this.countingPosNeg == countingSign) continue;
                    if (!this.firstSignChange) {
                        if (this.countingPosNeg > 0) {
                            this.risingTimeDeltaAverage.offer((int)((long)oRecordCount - this.prevRecordCount));
                            this.negRunCount = 0;
                            this.prevRecordCount = oRecordCount;
                            this.lastLowDataRecordCount = oRecordCount;
                        }
                        if (this.countingPosNeg < 0) {
                            this.fallingTimeDeltaAverage.offer((int)((long)oRecordCount - this.prevRecordCount));
                            this.posRunCount = 0;
                            this.prevRecordCount = oRecordCount;
                            this.lastHighDataRecordCount = oRecordCount;
                        }
                    } else {
                        this.prevRecordCount = oRecordCount;
                    }
                    this.firstSignChange = false;
                    this.countingPosNeg = countingSign;
                }
                this.prefillFromBuffer = false;
            } else {
                this.posNegAverage.offer(value);
                int hiValue = this.posNegAverage.getHighValue();
                int lowValue = this.posNegAverage.getLowValue();
                if (hiValue == Integer.MIN_VALUE || lowValue == Integer.MIN_VALUE) {
                    return;
                }
                int countingSign = 0;
                if (value >= hiValue) {
                    ++this.posRunCount;
                    countingSign = 1;
                }
                if (value <= lowValue) {
                    ++this.negRunCount;
                    countingSign = -1;
                }
                if (countingSign != 0 && this.countingPosNeg != countingSign) {
                    if (!this.firstSignChange) {
                        if (this.countingPosNeg > 0) {
                            this.risingTimeDeltaAverage.offer((int)(this.recordCount - this.prevRecordCount));
                            this.negRunCount = 0;
                            this.prevRecordCount = this.recordCount;
                            this.lastLowDataRecordCount = this.recordCount;
                            this.negRunCount = 0;
                        }
                        if (this.countingPosNeg < 0) {
                            this.fallingTimeDeltaAverage.offer((int)(this.recordCount - this.prevRecordCount));
                            this.posRunCount = 0;
                            this.prevRecordCount = this.recordCount;
                            this.lastHighDataRecordCount = this.recordCount;
                        }
                    } else {
                        this.prevRecordCount = this.recordCount;
                    }
                    this.firstSignChange = false;
                    this.countingPosNeg = countingSign;
                }
            }
        }
    }

    @Override
    public int getChannelValue() {
        if (!this.fallingTimeDeltaAverage.isEmpty() && !this.risingTimeDeltaAverage.isEmpty()) {
            double cycleLength = this.fallingTimeDeltaAverage.getDoubleValue() + this.risingTimeDeltaAverage.getDoubleValue();
            double duration = cycleLength * (double)this.activeSampleTime_nS;
            if (duration == 0.0) {
                return Integer.MIN_VALUE;
            }
            double dFreq = 1.0E9 / duration;
            return (int)Math.round(dFreq);
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public int calcDefault() {
        ++this.recordCount;
        DependentChannel srcChannel = this.getDependentChannels()[0];
        int value = srcChannel.getChannelRef().getChannelValue();
        if (this.debugFileReader != null) {
            try {
                String line = this.debugFileReader.readLine();
                if (line != null) {
                    int newValue;
                    value = newValue = Integer.parseInt(line);
                } else {
                    this.debugFileReader.close();
                    this.debugFileReader = null;
                }
            }
            catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
        if (value != Integer.MIN_VALUE) {
            this.updateWorkingValues_2(value);
        }
        return value;
    }

    @Override
    public int calcResampled() {
        return this.calcDefault();
    }

    @Override
    public String getChannelUnits() {
        return null;
    }

    @Override
    public DependentChannel[] getDependentChannels() {
        return this.srcChannels;
    }

    @Override
    public boolean makeActive(long dataSampleTime_nS) {
        this.activeSampleTime_nS = dataSampleTime_nS;
        this.window_nS.set(this.thisWindow_nS);
        this.overallAverage = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
        this.posAverage = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
        this.negAverage = new CircularBuffer_int(dataSampleTime_nS, this.window_nS.get());
        this.posNegAverage = new CircularBuffer_HighLow_Totaliser(dataSampleTime_nS, this.window_nS.get());
        this.fallingTimeDeltaAverage = new CircularBuffer_int(50);
        this.risingTimeDeltaAverage = new CircularBuffer_int(50);
        this.lowAverage = new CircularBuffer_int(5);
        this.highAverage = new CircularBuffer_int(5);
        this.firstSignChange = true;
        this.prefillFromBuffer = true;
        this.posRunCount = 0;
        this.negRunCount = 0;
        this.countingPosNeg = 0;
        this.prevRecordCount = 0L;
        this.recordCount = 0L;
        this.lastLowDataRecordCount = 0L;
        this.lastHighDataRecordCount = 0L;
        File file = new File(AppProperties.getApplicationRWFilesPath() + "/data/");
        file.mkdirs();
        file = new File(AppProperties.getApplicationRWFilesPath() + "/data/voltage_L1.txt");
        if (file.exists()) {
            try {
                this.debugFileReader = new BufferedReader(new FileReader(file));
            }
            catch (FileNotFoundException e) {
                this.debugFileReader = null;
                e.printStackTrace();
            }
        } else {
            this.debugFileReader = null;
        }
        return false;
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

    @Override
    public long getSpecialLong(int specialId) {
        switch (specialId) {
            case 1: {
                return this.lastLowDataRecordCount;
            }
            case 2: {
                return this.lastHighDataRecordCount;
            }
        }
        return 0L;
    }

    @Override
    public void onChannelLookup() {
    }

    public static List<String> getDefinitionAsXML() {
        return xmlDefinition;
    }
}

