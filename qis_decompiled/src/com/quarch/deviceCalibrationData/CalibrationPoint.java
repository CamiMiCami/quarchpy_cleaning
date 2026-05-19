/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceCalibrationData;

public class CalibrationPoint {
    int Limit;
    float Offset;
    float Multiplier;

    CalibrationPoint(int limit, float offset, float multiplier) {
        this.Limit = limit;
        this.Offset = offset;
        this.Multiplier = multiplier;
    }
}

