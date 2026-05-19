/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import src.com.quarch.deviceXML.Channel;

public class Channels {
    @XmlElement(name="Channel")
    public List<Channel> channels;
}

