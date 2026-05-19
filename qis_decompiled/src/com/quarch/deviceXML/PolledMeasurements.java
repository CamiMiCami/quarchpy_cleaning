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
import src.com.quarch.deviceXML.Measurement;

public class PolledMeasurements {
    private List<Measurement> measurements = new ArrayList<Measurement>();

    @XmlElement(name="Measurement")
    public List<Measurement> getMeasurements() {
        return this.measurements;
    }

    public void setMeasurements(Measurement measurement) {
        this.measurements.add(measurement);
    }
}

