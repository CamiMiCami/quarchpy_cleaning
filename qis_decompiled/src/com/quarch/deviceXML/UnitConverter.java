/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.util.Pair
 */
package src.com.quarch.deviceXML;

import java.util.regex.PatternSyntaxException;
import javafx.util.Pair;

public final class UnitConverter {
    private Unit baseUnit;

    public UnitConverter(Unit baseUnit) {
        this.baseUnit = baseUnit;
    }

    public UnitConverter(String baseUnit) {
        this.baseUnit = Unit.makeUnit(baseUnit);
    }

    public Unit getBaseUnit() {
        return this.baseUnit;
    }

    public void setBaseUnit(Unit baseUnit) {
        this.baseUnit = baseUnit;
    }

    public boolean isZero(Unit unit) {
        return unit == Unit.V || unit == Unit.A || unit == Unit.W || unit == Unit.S || unit == Unit.D;
    }

    public boolean isMilli(Unit unit) {
        return unit == Unit.mV || unit == Unit.mA || unit == Unit.mW || unit == Unit.mS || unit == Unit.D;
    }

    public boolean isMicro(Unit unit) {
        return unit == Unit.uV || unit == Unit.uA || unit == Unit.uW || unit == Unit.uS || unit == Unit.D;
    }

    public boolean isNano(Unit unit) {
        return unit == Unit.nS;
    }

    public boolean isVoltage(Unit unit) {
        return unit == Unit.V || unit == Unit.mV || unit == Unit.uV;
    }

    public boolean isCurrent(Unit unit) {
        return unit == Unit.A || unit == Unit.mA || unit == Unit.uA;
    }

    public boolean isPower(Unit unit) {
        return unit == Unit.W || unit == Unit.mW || unit == Unit.uW;
    }

    public boolean isTime(Unit unit) {
        return unit == Unit.S || unit == Unit.mS || unit == Unit.uS || unit == Unit.nS;
    }

    public boolean isDigital(Unit unit) {
        return unit == Unit.D;
    }

    public boolean isSameUnit(Unit unit) {
        if (this.isVoltage(this.baseUnit) && this.isVoltage(unit)) {
            return true;
        }
        if (this.isCurrent(this.baseUnit) && this.isCurrent(unit)) {
            return true;
        }
        if (this.isPower(this.baseUnit) && this.isPower(unit)) {
            return true;
        }
        if (this.isTime(this.baseUnit) && this.isTime(unit)) {
            return true;
        }
        return this.isDigital(this.baseUnit) && this.isDigital(unit);
    }

    public double valueInNanoUnits(double value) {
        double valueNano = this.isZero(this.baseUnit) ? value * 1.0E9 : (this.isMilli(this.baseUnit) ? value * 1000000.0 : (this.isMicro(this.baseUnit) ? value * 1000.0 : (this.isNano(this.baseUnit) ? value : -1.0)));
        return valueNano;
    }

    public Pair<Double, Unit> convertToBaseUnits(double value, Unit unit) {
        if (this.isZero(this.baseUnit) && this.isZero(unit) || this.isMilli(this.baseUnit) && this.isMilli(unit) || this.isMicro(this.baseUnit) && this.isMicro(unit) || this.isNano(this.baseUnit) && this.isNano(unit)) {
            return new Pair((Object)value, (Object)unit);
        }
        if (this.isZero(this.baseUnit) && this.isMilli(unit)) {
            return new Pair((Object)(value / 1000.0), (Object)this.baseUnit);
        }
        if (this.isZero(this.baseUnit) && this.isMicro(unit)) {
            return new Pair((Object)(value / 1000000.0), (Object)this.baseUnit);
        }
        if (this.isZero(this.baseUnit) && this.isNano(unit)) {
            return new Pair((Object)(value / 1.0E9), (Object)this.baseUnit);
        }
        if (this.isMilli(this.baseUnit) && this.isZero(unit)) {
            return new Pair((Object)(value * 1000.0), (Object)this.baseUnit);
        }
        if (this.isMilli(this.baseUnit) && this.isMicro(unit)) {
            return new Pair((Object)(value / 1000.0), (Object)this.baseUnit);
        }
        if (this.isMilli(this.baseUnit) && this.isNano(unit)) {
            return new Pair((Object)(value / 1000000.0), (Object)this.baseUnit);
        }
        if (this.isMicro(this.baseUnit) && this.isZero(unit)) {
            return new Pair((Object)(value * 1000000.0), (Object)this.baseUnit);
        }
        if (this.isMicro(this.baseUnit) && this.isMilli(unit)) {
            return new Pair((Object)(value * 1000.0), (Object)this.baseUnit);
        }
        if (this.isMicro(this.baseUnit) && this.isNano(unit)) {
            return new Pair((Object)(value * 1000000.0), (Object)this.baseUnit);
        }
        if (this.isNano(this.baseUnit) && this.isZero(unit)) {
            return new Pair((Object)(value * 1.0E9), (Object)this.baseUnit);
        }
        if (this.isNano(this.baseUnit) && this.isMilli(unit)) {
            return new Pair((Object)(value * 1000000.0), (Object)this.baseUnit);
        }
        if (this.isNano(this.baseUnit) && this.isMicro(unit)) {
            return new Pair((Object)(value * 1000.0), (Object)this.baseUnit);
        }
        return new Pair((Object)-1.0, (Object)unit);
    }

    public Pair<Double, Unit> convertToBaseUnits(String valueAndUnitString) {
        Pair<Double, Unit> valUnitPair = UnitConverter.valueUnit(valueAndUnitString);
        if (valUnitPair == null) {
            return null;
        }
        double val = (Double)valUnitPair.getKey();
        Unit unit = (Unit)((Object)valUnitPair.getValue());
        Pair<Double, Unit> convertedPair = this.convertToBaseUnits(val, unit);
        return convertedPair;
    }

    public double convertToBaseUnits_ValueOnly(double value, Unit unit) {
        if (this.isZero(this.baseUnit) && this.isZero(unit) || this.isMilli(this.baseUnit) && this.isMilli(unit) || this.isMicro(this.baseUnit) && this.isMicro(unit) || this.isNano(this.baseUnit) && this.isNano(unit)) {
            return value;
        }
        if (this.isZero(this.baseUnit) && this.isMilli(unit)) {
            return value / 1000.0;
        }
        if (this.isZero(this.baseUnit) && this.isMicro(unit)) {
            return value / 1000000.0;
        }
        if (this.isZero(this.baseUnit) && this.isNano(unit)) {
            return value / 1.0E9;
        }
        if (this.isMilli(this.baseUnit) && this.isZero(unit)) {
            return value * 1000.0;
        }
        if (this.isMilli(this.baseUnit) && this.isMicro(unit)) {
            return value / 1000.0;
        }
        if (this.isMilli(this.baseUnit) && this.isNano(unit)) {
            return value / 1000000.0;
        }
        if (this.isMicro(this.baseUnit) && this.isZero(unit)) {
            return value * 1000000.0;
        }
        if (this.isMicro(this.baseUnit) && this.isMilli(unit)) {
            return value * 1000.0;
        }
        if (this.isMicro(this.baseUnit) && this.isNano(unit)) {
            return value * 1000000.0;
        }
        if (this.isNano(this.baseUnit) && this.isZero(unit)) {
            return value * 1.0E9;
        }
        if (this.isNano(this.baseUnit) && this.isMilli(unit)) {
            return value * 1000000.0;
        }
        if (this.isNano(this.baseUnit) && this.isMicro(unit)) {
            return value * 1000.0;
        }
        return -1.0;
    }

    public double convertToBaseUnits_ValueOnly(double value, String unit) {
        return this.convertToBaseUnits_ValueOnly(value, Unit.makeUnit(unit));
    }

    public double convertToBaseUnits_ValueOnly(String valueAndUnitString) {
        Pair<Double, Unit> valUnitPair = UnitConverter.valueUnit(valueAndUnitString);
        if (valUnitPair == null) {
            return Double.NaN;
        }
        double val = (Double)valUnitPair.getKey();
        Unit unit = (Unit)((Object)valUnitPair.getValue());
        double converted = this.convertToBaseUnits_ValueOnly(val, unit);
        return converted;
    }

    public static Pair<Double, Unit> valueUnit(String valueAndUnitString) {
        double value;
        String valueStr = valueAndUnitString.replaceAll("[^\\d.]", "");
        String unitStr = valueAndUnitString.replaceAll("[\\.0123456789]", "");
        try {
            value = Double.parseDouble(valueStr);
        }
        catch (NumberFormatException e) {
            return null;
        }
        Unit unit = Unit.makeUnit(unitStr);
        Pair valueUnitPair = new Pair((Object)value, (Object)unit);
        return valueUnitPair;
    }

    public static UnitConverter makeUnitConverter(String valueAndUnitString) {
        String unit;
        try {
            unit = valueAndUnitString.replaceAll("[\\.0123456789]", "");
        }
        catch (PatternSyntaxException e) {
            return null;
        }
        UnitConverter uc = new UnitConverter(unit);
        return uc;
    }

    public static String removeUnits(String string) {
        return UnitConverter.ScalableUnitsRemoveUnits(string);
    }

    public static String removeUnitsAndToInteger(String string) {
        int integer;
        String stringNumber = UnitConverter.removeUnits(string);
        try {
            integer = (int)Double.parseDouble(stringNumber);
        }
        catch (NumberFormatException exception) {
            return null;
        }
        String stringInt = Integer.toString(integer);
        return stringInt;
    }

    public static String removeNumber(String string) {
        String stripped = string.replaceAll("[\\.0123456789]", "");
        return stripped;
    }

    public static boolean isSame(double value1, Unit unit1, double value2, Unit unit2) {
        double val2n;
        UnitConverter uc1 = new UnitConverter(unit1);
        if (!uc1.isSameUnit(unit2)) {
            return false;
        }
        UnitConverter uc2 = new UnitConverter(unit2);
        double val1n = uc1.valueInNanoUnits(value1);
        return !(Math.abs(val1n - (val2n = uc2.valueInNanoUnits(value2))) > 0.001);
    }

    public static String ScalableUnitsRemoveUnits(String string) {
        String str = string.replaceAll("[^\\d.-]", "");
        return str;
    }

    public static enum UnitType {
        VOLTAGE,
        CURRENT,
        POWER,
        TIME,
        DIGITAL;


        public static UnitType unitType(String unit) {
            UnitType uType = unit.toLowerCase().contains("v") ? VOLTAGE : (unit.toLowerCase().contains("a") ? CURRENT : (unit.toLowerCase().contains("w") ? POWER : (unit.toLowerCase().contains("s") ? TIME : (unit.toLowerCase().contains("d") ? DIGITAL : null))));
            return uType;
        }
    }

    public static enum Unit {
        V("V"),
        mV("mV"),
        uV("uV"),
        A("A"),
        mA("mA"),
        uA("uA"),
        W("W"),
        mW("mW"),
        uW("uW"),
        S("S"),
        mS("mS"),
        uS("uS"),
        nS("nS"),
        D("D");

        private final String unit;

        private Unit(String unit) {
            this.unit = unit;
        }

        public String getUnit() {
            return this.unit;
        }

        public UnitType unitType() {
            return UnitType.unitType(this.unit);
        }

        public int multiplier() {
            int mult = this.unit.toLowerCase().contains("n") ? 1000000000 : (this.unit.toLowerCase().contains("u") ? 1000000 : (this.unit.toLowerCase().contains("m") ? 1000 : 1));
            return mult;
        }

        public boolean is_nS() {
            return this.unit.equals("nS");
        }

        public boolean is_uS() {
            return this.unit.equals("uS");
        }

        public boolean is_mS() {
            return this.unit.equals("mS");
        }

        public boolean is_S() {
            return this.unit.equals("S");
        }

        public static Unit makeUnit(String unit) {
            Unit u = unit.equals("v") || unit.equals("V") ? V : (unit.equals("mv") || unit.equals("mV") ? mV : (unit.equals("uv") || unit.equals("uV") ? uV : (unit.equals("a") || unit.equals("A") ? A : (unit.equals("ma") || unit.equals("mA") ? mA : (unit.equals("ua") || unit.equals("uA") ? uA : (unit.equals("w") || unit.equals("W") ? W : (unit.equals("mw") || unit.equals("mW") ? mW : (unit.equals("uw") || unit.equals("uW") ? uW : (unit.equals("s") || unit.equals("S") ? S : (unit.equals("ms") || unit.equals("mS") ? mS : (unit.equals("us") || unit.equals("uS") ? uS : (unit.equals("ns") || unit.equals("nS") ? nS : (unit.equals("d") || unit.equals("D") ? D : null)))))))))))));
            return u;
        }

        public static Unit makeUnit(UnitType unitType, int multiplier) {
            String multiplierPrefix = multiplier == 1 ? "" : (multiplier == 1000 ? "m" : (multiplier == 1000000 ? "u" : (multiplier == 1000000000 ? "n" : "")));
            String typeName = unitType == UnitType.VOLTAGE ? "V" : (unitType == UnitType.CURRENT ? "A" : (unitType == UnitType.POWER ? "W" : (unitType == UnitType.TIME ? "S" : (unitType == UnitType.DIGITAL ? "D" : "UNKNOWN"))));
            return Unit.makeUnit(multiplierPrefix + typeName);
        }
    }
}

