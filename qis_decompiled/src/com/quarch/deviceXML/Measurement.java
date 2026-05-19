/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.annotation.XmlElement;

public class Measurement {
    @XmlElement(name="Name")
    public String name;
    @XmlElement(name="Source")
    public String source;
    @XmlElement(name="Type")
    public String type;
}

