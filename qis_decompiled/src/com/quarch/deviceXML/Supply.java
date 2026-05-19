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
import src.com.quarch.deviceXML.Channel;
import src.com.quarch.deviceXML.Feature;

public class Supply {
    private List<Feature> features = new ArrayList<Feature>();
    private List<Channel> supplyChannels = new ArrayList<Channel>();

    @XmlElement(name="Feature")
    public List<Feature> getFeatures() {
        return this.features;
    }

    public void setFeatures(Feature feature) {
        this.features.add(feature);
    }

    @XmlElement(name="SupplyChannel")
    public List<Channel> getSupplyChannels() {
        return this.supplyChannels;
    }

    public void setSupplyChannels(Channel supplyChannel) {
        this.supplyChannels.add(supplyChannel);
    }
}

