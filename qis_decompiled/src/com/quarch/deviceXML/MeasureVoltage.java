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

public class MeasureVoltage {
    private List<String> params = new ArrayList<String>();

    @XmlElement(name="Param")
    public List<String> getParams() {
        return this.params;
    }

    public void setParams(String param) {
        this.params.add(param);
    }
}

