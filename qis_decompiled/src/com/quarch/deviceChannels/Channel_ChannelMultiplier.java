/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceChannels;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import src.com.quarch.channelFunctions.ChannelDataSourceIF;
import src.com.quarch.channelFunctions.ChannelFunctionIF;
import src.com.quarch.channelFunctions.DependentChannel;
import src.com.quarch.deviceChannels.Channel_Multiplier;
import src.com.quarch.utils.Magnitude;

public class Channel_ChannelMultiplier
implements ChannelFunctionIF,
ChannelDataSourceIF {
    public static final BigDecimal MAX_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
    public static final BigDecimal MIN_INT_AS_BIG_DECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);
    public static final BigDecimal THOUSAND_AS_BIG_DECIMAL = BigDecimal.valueOf(1000L);
    private final String nameStr;
    private final String groupStr;
    private final String baseUnitName;
    private final String cmdStr;
    private final List<DependentChannel> operandList;
    private final DependentChannel[] dependentChannels;
    private final Magnitude requestedOutputMagnitude;
    private int value;
    private long maxTValue;
    private String unitsString;
    private BigDecimal divisor = BigDecimal.ONE;

    public Channel_ChannelMultiplier(String channelStr, String baseUnitName, String cmdStr, String ... sourceChannels) {
        this(channelStr, baseUnitName, cmdStr, (Magnitude)null, sourceChannels);
    }

    public Channel_ChannelMultiplier(String channelStr, String baseUnitName, String cmdStr, Magnitude requestedOutputMagnitude, String ... sourceChannels) {
        this.requestedOutputMagnitude = requestedOutputMagnitude;
        if (sourceChannels == null || sourceChannels.length < 2) {
            throw new IllegalArgumentException("Channel_ChannelMultiplier - sourceChannels may not contain less than two values");
        }
        this.nameStr = this.keyToStr(channelStr, 0);
        this.groupStr = this.keyToStr(channelStr, 1);
        this.baseUnitName = baseUnitName;
        this.cmdStr = cmdStr;
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
    }

    @Override
    public int getChannelValue() {
        return this.value;
    }

    @Override
    public int calcDefault() {
        if (this.operandList.isEmpty()) {
            this.value = Integer.MIN_VALUE;
            return 0;
        }
        List rawValues = this.operandList.stream().map(DependentChannel::getChannelRef).mapToInt(ChannelDataSourceIF::getChannelValue).boxed().collect(Collectors.toList());
        if (rawValues.stream().anyMatch(integer -> integer.equals(Integer.MIN_VALUE))) {
            this.value = Integer.MIN_VALUE;
            return 0;
        }
        Optional<BigDecimal> rawResultOptional = rawValues.stream().map(BigInteger::valueOf).reduce(BigInteger::multiply).map(BigDecimal::new).map(value -> value.divide(this.divisor, RoundingMode.HALF_UP));
        if (!rawResultOptional.isPresent()) {
            this.value = Integer.MIN_VALUE;
            return 0;
        }
        BigDecimal rawResult = rawResultOptional.get();
        if (rawResult.compareTo(MAX_INT_AS_BIG_DECIMAL) > 0 || rawResult.compareTo(MIN_INT_AS_BIG_DECIMAL) <= 0) {
            this.value = Integer.MIN_VALUE;
            return 0;
        }
        this.value = rawResult.intValue();
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
        if (this.operandList.size() < 2) {
            return false;
        }
        Magnitude magnitude = Magnitude.UNIT;
        for (DependentChannel dependentChannel : this.operandList) {
            String sourceChannelUnits = dependentChannel.getChannelRef().getChannelUnits();
            Magnitude sourceMagnitude = Channel_Multiplier.getMagnitudeFromChannelUnitString(sourceChannelUnits);
            if (sourceMagnitude == null) {
                return false;
            }
            magnitude = magnitude.product(sourceMagnitude);
        }
        BigDecimal rawMaxTValue = this.operandList.stream().map(DependentChannel::getChannelRef).map(channelDataSourceIF -> BigInteger.valueOf(channelDataSourceIF.getMaxTValue())).reduce(BigInteger::multiply).map(BigDecimal::new).orElse(BigDecimal.ZERO);
        if (this.requestedOutputMagnitude != null) {
            int multipliedMagnitudePower = magnitude.getPower();
            int adjustedPower = multipliedMagnitudePower - this.requestedOutputMagnitude.getPower();
            this.divisor = BigDecimal.valueOf(Math.pow(10.0, -1 * adjustedPower));
            this.maxTValue = (rawMaxTValue = rawMaxTValue.divide(this.divisor, RoundingMode.HALF_UP)).compareTo(MAX_INT_AS_BIG_DECIMAL) > 0 ? Integer.MAX_VALUE : rawMaxTValue.longValue();
            this.unitsString = this.requestedOutputMagnitude.getPrefix() + this.baseUnitName;
        } else {
            this.divisor = BigDecimal.ONE;
            while (rawMaxTValue.compareTo(MAX_INT_AS_BIG_DECIMAL) >= 0) {
                magnitude = Magnitude.getMagnitudeFromPower(magnitude.getPower() + 3);
                rawMaxTValue = rawMaxTValue.divide(THOUSAND_AS_BIG_DECIMAL, RoundingMode.HALF_UP);
                this.divisor = this.divisor.multiply(THOUSAND_AS_BIG_DECIMAL);
            }
            this.maxTValue = rawMaxTValue.longValue();
            this.unitsString = magnitude.getPrefix() + this.baseUnitName;
        }
        return true;
    }

    @Override
    public int getValue() {
        return this.value;
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
        return this.cmdStr;
    }

    public String getBaseUnitName() {
        return this.baseUnitName;
    }

    public Magnitude getRequestedOutputMagnitude() {
        return this.requestedOutputMagnitude;
    }

    public String toString() {
        return "Channel_ChannelMultiplier{nameStr='" + this.nameStr + '\'' + ", groupStr='" + this.groupStr + '\'' + ", baseUnitName='" + this.baseUnitName + '\'' + ", cmdStr='" + this.cmdStr + '\'' + ", operandList=" + this.operandList + ", dependentChannels=" + Arrays.toString(this.dependentChannels) + ", requestedOutputMagnitude=" + (Object)((Object)this.requestedOutputMagnitude) + ", value=" + this.value + ", maxTValue=" + this.maxTValue + ", unitsString='" + this.unitsString + '\'' + ", divisor=" + this.divisor + '}';
    }
}

