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
import src.com.quarch.deviceXML.Param;

public class Group {
    private String name;
    private String value;
    private ArrayList<Param> params = new ArrayList();

    @XmlElement(name="Value")
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlElement(name="Name")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name="Param")
    public List<Param> getParam() {
        return this.params;
    }

    public void setParam(Param param) {
        this.params.add(param);
    }
}

