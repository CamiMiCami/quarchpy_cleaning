/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlTransient
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

public class ParamList {
    private String name;
    private ArrayList<String> valueList = new ArrayList();

    @XmlElement(name="Value")
    public List<String> getValuet() {
        return this.valueList;
    }

    public void setValue(String value) {
        this.valueList.add(value);
    }

    @XmlTransient
    public List<String> getValueList() {
        return this.valueList;
    }

    public void setValueList(ArrayList<String> value) {
        this.valueList = value;
    }

    @XmlElement(name="Name")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

