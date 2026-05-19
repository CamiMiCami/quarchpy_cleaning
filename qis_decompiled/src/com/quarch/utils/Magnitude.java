/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public enum Magnitude {
    ZETTA(21, "Z", "Zetta"),
    EXA(18, "E", "Exa"),
    PETA(15, "P", "Peta"),
    TERA(12, "T", "Tera"),
    GIGA(9, "G", "Giga"),
    MEGA(6, "M", "Mega"),
    KILO(3, "k", "Kilo"),
    UNIT(0, " ", "Unit", "unit"),
    MILLI(-3, "m", "Milli"),
    MICRO(-6, "u", "Micro"),
    NANO(-9, "n", "Nano"),
    PICO(-12, "p", "Pico"),
    FEMTO(-15, "f", "Femto"),
    ATTO(-18, "a", "Atto"),
    ZEPTO(-21, "z", "Zepto");

    private final double ratio;
    private final int power;
    private final String prefix;
    private final String displayName;
    private final String parameterValue;

    private Magnitude(int power, String prefix, String displayName) {
        this(power, prefix, displayName, prefix);
    }

    private Magnitude(int power, String prefix, String displayName, String parameterValue) {
        this.ratio = Math.pow(10.0, power);
        this.power = power;
        this.prefix = prefix;
        this.displayName = displayName;
        this.parameterValue = parameterValue;
    }

    public int getPower() {
        return this.power;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getParameterValue() {
        return this.parameterValue;
    }

    public double toBaseUnit(int sourceValue) {
        return this.toBaseUnit((double)sourceValue);
    }

    public double toBaseUnit(double sourceValue) {
        return sourceValue * this.ratio;
    }

    public int roundedToBaseUnit(double sourceValue) {
        return (int)Math.round(this.toBaseUnit(sourceValue));
    }

    public double fromBaseUnit(double value) {
        return value / this.ratio;
    }

    public int roundedFromBaseUnit(double value) {
        return (int)Math.round(this.fromBaseUnit(value));
    }

    public static Magnitude getMagnitudeFromPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        if (prefix.isEmpty()) {
            return UNIT;
        }
        return Arrays.stream(Magnitude.values()).filter(magnitude -> magnitude.prefix.equals(prefix)).findAny().orElse(null);
    }

    public static Magnitude getMagnitudeFromValue(double value) {
        double testValue = Math.abs(value);
        return ((Stream)Arrays.stream(Magnitude.values()).sequential()).filter(magnitude -> testValue >= magnitude.ratio).findFirst().orElse(null);
    }

    public static Magnitude getMagnitudeFromPower(int power) {
        return Arrays.stream(Magnitude.values()).filter(magnitude -> magnitude.power == power).findAny().orElse(null);
    }

    public static Magnitude getClosestMagnitudeFromPower(int power) {
        return Arrays.stream(Magnitude.values()).filter(magnitude -> magnitude.power <= power).findFirst().orElse(null);
    }

    public Magnitude inverse() {
        return Magnitude.getMagnitudeFromPower(-1 * this.getPower());
    }

    public Magnitude product(Magnitude operand) {
        return Magnitude.getMagnitudeFromPower(this.getPower() + operand.getPower());
    }

    public static Optional<Magnitude> fromParameter(String rawInput) {
        if (rawInput == null) {
            return Optional.empty();
        }
        String testValue = rawInput.trim();
        return Arrays.stream(Magnitude.values()).filter(magnitude -> magnitude.getDisplayName().equalsIgnoreCase(testValue) || magnitude.getParameterValue().equals(testValue)).findFirst();
    }
}

