/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceCalibrationData;

import java.util.ArrayList;
import src.com.quarch.deviceCalibrationData.CalibrationPoint;

public class Calibration {
    ArrayList<CalibrationPoint> V12Cal = new ArrayList();
    ArrayList<CalibrationPoint> V5Cal = new ArrayList();

    public void add5VCalPoint(int Limit, float Offset, float Multiplier) {
        this.V5Cal.add(new CalibrationPoint(Limit, Offset, Multiplier));
    }

    public void add12VCalPoint(int Limit, float Offset, float Multiplier) {
        this.V12Cal.add(new CalibrationPoint(Limit, Offset, Multiplier));
    }

    int get5VCurrentCaluAResult(int ThisValue) {
        for (CalibrationPoint ThisPoint : this.V5Cal) {
            if (ThisValue >= ThisPoint.Limit) continue;
            double retVal = ((float)ThisValue - ThisPoint.Offset) * ThisPoint.Multiplier;
            return (int)(retVal *= 1000.0);
        }
        System.err.println("no valid calibration point found");
        return 0;
    }

    int get12VCurrentCaluAResult(int ThisValue) {
        for (CalibrationPoint ThisPoint : this.V12Cal) {
            if (ThisValue >= ThisPoint.Limit) continue;
            double retVal = ((float)ThisValue - ThisPoint.Offset) * ThisPoint.Multiplier;
            return (int)(retVal *= 1000.0);
        }
        System.err.println("no valid calibration point found");
        return 0;
    }

    public int get5VCurrentCalResult(int ThisValue) {
        for (CalibrationPoint ThisPoint : this.V5Cal) {
            if (ThisValue >= ThisPoint.Limit) continue;
            return (int)(((float)ThisValue - ThisPoint.Offset) * ThisPoint.Multiplier);
        }
        System.err.println("no valid calibration point found");
        return 0;
    }

    public int get12VCurrentCalResult(int ThisValue) {
        for (CalibrationPoint ThisPoint : this.V12Cal) {
            if (ThisValue >= ThisPoint.Limit) continue;
            return (int)(((float)ThisValue - ThisPoint.Offset) * ThisPoint.Multiplier);
        }
        System.err.println("no valid calibration point found");
        return 0;
    }
}

