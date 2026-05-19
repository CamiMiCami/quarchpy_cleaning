/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;
import src.com.quarch.deviceXML.MeasureVoltage;

public class SelfTest {
    private List<MeasureVoltage> MeasureVoltages = new ArrayList<MeasureVoltage>();

    @XmlElement(name="MeasureVoltage")
    public List<MeasureVoltage> getMeasureVoltages() {
        return this.MeasureVoltages;
    }

    public void setMeasureVoltages(MeasureVoltage measureVoltage) {
        this.MeasureVoltages.add(measureVoltage);
    }
}

