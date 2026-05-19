/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.text.DecimalFormat;
import java.util.Arrays;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.Channel_Multiplier;
import src.com.quarch.utils.Magnitude;

public class Channel_ScalarMultiplier
implements ChannelFunctionIF,
ChannelDataSourceIF {
    private final String nameStr;
    private final String groupStr;
    private final double multiplier;
    private final String multiplierString;
    private final String baseUnitName;
    private String unitsString;
    private final String origionalCmdStr;
    private final Magnitude requestedOutputMagnitude;
    private final DependentChannel sourceChannel;
    private final DependentChannel[] dependentChannels = new DependentChannel[1];
    private int value;
    private long maxTValue;
    private int powerShift;

    public Channel_ScalarMultiplier(String channelStr, String sourceChannel, double multiplier, String multiplierString, String baseUnitName, String cmdStr) {
        this(channelStr, sourceChannel, multiplier, multiplierString, baseUnitName, cmdStr, null);
    }

    public Channel_ScalarMultiplier(String channelStr, String sourceChannel, double multiplier, String multiplierString, String baseUnitName, String cmdStr, Magnitude requestedOutputMagnitude) {
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        this.multiplierString = multiplierString;
        this.baseUnitName = baseUnitName;
        this.multiplier = multiplier;
        this.origionalCmdStr = cmdStr;
        this.requestedOutputMagnitude = requestedOutputMagnitude;
        this.sourceChannel = new DependentChannel();
        this.sourceChannel.setNameKey(sourceChannel);
        this.dependentChannels[0] = this.sourceChannel;
    }

    @Override
    public int getChannelValue() {
        return this.value;
    }

    @Override
    public int calcDefault() {
        int sourceValue = this.sourceChannel.getChannelRef().getChannelValue();
        if (sourceValue == Integer.MIN_VALUE) {
            this.value = Integer.MIN_VALUE;
            return 0;
        }
        long rawValue = this.roundedShiftedMultiply(sourceValue);
        this.value = rawValue > Integer.MAX_VALUE || rawValue <= Integer.MIN_VALUE ? Integer.MIN_VALUE : (int)rawValue;
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
        String sourceChannelUnits = this.sourceChannel.getChannelRef().getChannelUnits();
        Magnitude sourceMagnitude = Channel_Multiplier.getMagnitudeFromChannelUnitString(sourceChannelUnits);
        if (sourceMagnitude == null) {
            return false;
        }
        long maxSourceValue = this.sourceChannel.getChannelRef().getMaxTValue();
        if (this.requestedOutputMagnitude != null) {
            this.powerShift = Channel_ScalarMultiplier.derivePowerShiftFromRequestedOutputMagnitude(sourceMagnitude, this.requestedOutputMagnitude);
            this.maxTValue = (int)Math.max(this.roundedShiftedMultiply((int)maxSourceValue), Integer.MAX_VALUE);
            this.unitsString = this.requestedOutputMagnitude.getPrefix() + this.baseUnitName;
        } else {
            this.powerShift = Channel_ScalarMultiplier.derivePowerShiftFromMultiplier(maxSourceValue, this.multiplier, this.multiplierString);
            this.maxTValue = (int)Math.max(this.roundedShiftedMultiply((int)maxSourceValue), Integer.MAX_VALUE);
            Magnitude outputMagnitude = Magnitude.getMagnitudeFromPower(sourceMagnitude.getPower() - this.powerShift);
            if (outputMagnitude == null) {
                return false;
            }
            this.unitsString = outputMagnitude.getPrefix() + this.baseUnitName;
        }
        return true;
    }

    private long roundedShiftedMultiply(int sourceValue) {
        return Math.round((double)sourceValue * this.multiplier * Math.pow(10.0, this.powerShift));
    }

    public static int derivePowerShiftFromRequestedOutputMagnitude(Magnitude sourceMagnitude, Magnitude requestedOutputMagnitude) {
        return sourceMagnitude.getPower() - requestedOutputMagnitude.getPower();
    }

    public static int derivePowerShiftFromMultiplier(long sourceTMax, double multiplier, String originalMultiplier) {
        int shiftPower;
        block10: {
            originalMultiplier = originalMultiplier.trim();
            double absMultiplier = Math.abs(multiplier);
            if (absMultiplier == 0.0 || sourceTMax == 0L) {
                return 0;
            }
            boolean preserveRight = absMultiplier < 1.0;
            double testValue = (double)sourceTMax * absMultiplier;
            if (originalMultiplier.contains(".")) {
                originalMultiplier = originalMultiplier.replaceAll("0*$", "");
                originalMultiplier = originalMultiplier.replaceAll("\\.$", "");
            }
            shiftPower = 0;
            if (originalMultiplier.contains(".")) {
                int decimals = originalMultiplier.trim().split("\\.")[1].length();
                while (decimals % 3 > 0) {
                    ++decimals;
                }
                shiftPower = decimals;
            }
            StringBuilder formatPatternBuilder = new StringBuilder("##################################################################");
            if (shiftPower > 0) {
                formatPatternBuilder.append(".");
                for (int i = 0; i < shiftPower; ++i) {
                    formatPatternBuilder.append("0");
                }
            }
            DecimalFormat format = new DecimalFormat(formatPatternBuilder.toString());
            String updatedTestValueAsString = format.format(testValue);
            boolean canConvertToInt = Channel_ScalarMultiplier.canConvertToInt(updatedTestValueAsString = updatedTestValueAsString.replace(".", ""));
            if (canConvertToInt) break block10;
            if (preserveRight) {
                while (!canConvertToInt) {
                    updatedTestValueAsString = updatedTestValueAsString.substring(3);
                    shiftPower += 3;
                    canConvertToInt = Channel_ScalarMultiplier.canConvertToInt(updatedTestValueAsString);
                }
            } else {
                while (!canConvertToInt) {
                    updatedTestValueAsString = updatedTestValueAsString.substring(0, updatedTestValueAsString.length() - 3);
                    shiftPower -= 3;
                    canConvertToInt = Channel_ScalarMultiplier.canConvertToInt(updatedTestValueAsString);
                }
            }
        }
        return shiftPower;
    }

    public static boolean canConvertToInt(String testValueAsString) {
        boolean canConvertToInt = true;
        try {
            Integer.parseInt(testValueAsString);
        }
        catch (NumberFormatException nfe) {
            canConvertToInt = false;
        }
        return canConvertToInt;
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

    public double getMultiplier() {
        return this.multiplier;
    }

    public String getBaseUnitName() {
        return this.baseUnitName;
    }

    public DependentChannel getSourceChannel() {
        return this.sourceChannel;
    }

    public Magnitude getRequestedOutputMagnitude() {
        return this.requestedOutputMagnitude;
    }

    public String toString() {
        return "Channel_ScalarMultiplier{nameStr='" + this.nameStr + '\'' + ", groupStr='" + this.groupStr + '\'' + ", multiplier=" + this.multiplier + ", multiplierString='" + this.multiplierString + '\'' + ", baseUnitName='" + this.baseUnitName + '\'' + ", unitsString='" + this.unitsString + '\'' + ", origionalCmdStr='" + this.origionalCmdStr + '\'' + ", sourceChannel=" + this.sourceChannel + ", dependentChannels=" + Arrays.toString(this.dependentChannels) + ", value=" + this.value + ", maxTValue=" + this.maxTValue + ", powerShift=" + this.powerShift + '}';
    }
}

