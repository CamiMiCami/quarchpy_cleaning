/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devicePPM;

public enum EnumPPMAverage {
    AV_0(0, "0"),
    AV_2(2, "2"),
    AV_4(4, "4"),
    AV_8(8, "8"),
    AV_16(16, "16"),
    AV_32(32, "32"),
    AV_64(64, "64"),
    AV_128(128, "128"),
    AV_256(256, "256"),
    AV_512(512, "512"),
    AV_1K(1024, "1K"),
    AV_2K(2048, "2K"),
    AV_4K(4096, "4K"),
    AV_8K(8192, "8K"),
    AV_16K(16384, "16K"),
    AV_32K(32768, "32K");

    private final int value;
    private final String string;

    private EnumPPMAverage(int value, String string2) {
        this.value = value;
        this.string = string2;
    }

    public int getValue() {
        return this.value;
    }

    public String toString() {
        return this.string;
    }

    public static String[] Strings() {
        String[] returnArray = new String[EnumPPMAverage.values().length];
        for (int i = 0; i < EnumPPMAverage.values().length; ++i) {
            returnArray[i] = EnumPPMAverage.values()[i].toString();
        }
        return returnArray;
    }

    public static EnumPPMAverage getPPMAverageEnum(String thisString) {
        int pos = thisString.indexOf(" :");
        if (pos > 0) {
            thisString = thisString.substring(0, pos);
        }
        for (EnumPPMAverage thisAverage : EnumPPMAverage.values()) {
            if (!thisString.toUpperCase().equals(thisAverage.toString())) continue;
            return thisAverage;
        }
        return null;
    }
}

