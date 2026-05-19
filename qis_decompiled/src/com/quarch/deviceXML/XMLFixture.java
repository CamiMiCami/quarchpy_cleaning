/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.JAXBContext
 *  jakarta.xml.bind.JAXBException
 *  jakarta.xml.bind.Unmarshaller
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlRootElement
 *  jakarta.xml.bind.annotation.XmlTransient
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import src.com.quarch.deviceXML.Channel;
import src.com.quarch.deviceXML.Channels;
import src.com.quarch.deviceXML.Features;
import src.com.quarch.deviceXML.General;
import src.com.quarch.deviceXML.Group;
import src.com.quarch.deviceXML.Groups;
import src.com.quarch.deviceXML.Param;

@XmlRootElement(name="XmlResponse")
public class XMLFixture {
    @XmlElement(name="General")
    public General general;
    @XmlElement(name="Features")
    public Features features;
    @XmlElement(name="Groups")
    public Groups groups;
    @XmlElement(name="Channels")
    public Channels channels;

    public static XMLFixture createFromXML(String xmlData) {
        XMLFixture xmlFixture = null;
        try {
            StringReader reader = new StringReader(xmlData);
            JAXBContext jc = JAXBContext.newInstance((Class[])new Class[]{XMLFixture.class});
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            xmlFixture = (XMLFixture)unmarshaller.unmarshal((Reader)reader);
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
        return xmlFixture;
    }

    @XmlTransient
    public List<String> getFullEnabledChannelNames() {
        ArrayList<String> retVal = new ArrayList<String>();
        for (Channel chan : this.channels.channels) {
            String name = "";
            String type = "";
            boolean enabled = false;
            for (Param param : chan.getParam()) {
                if (param.getName().equalsIgnoreCase("Name")) {
                    name = param.getValue();
                    continue;
                }
                if (param.getName().equalsIgnoreCase("Type")) {
                    String pType = param.getValue();
                    switch (pType.toLowerCase()) {
                        case "v": {
                            type = "Voltage";
                            break;
                        }
                        case "a": {
                            type = "Current";
                            break;
                        }
                        default: {
                            type = pType;
                            break;
                        }
                    }
                    continue;
                }
                if (!param.getName().equalsIgnoreCase("Enabled")) continue;
                enabled = true;
            }
            if (!enabled) continue;
            retVal.add(name + ":" + type);
        }
        return retVal;
    }

    @XmlTransient
    public long[] getGroupSampleRates() {
        int size = this.groups.groups.size();
        try {
            long[] arry = new long[size];
            for (int i = 0; i < size; ++i) {
                Group g = this.groups.groups.get(i);
                int groupN = -1;
                long base = -1L;
                long exp = -1L;
                for (Param p : g.getParam()) {
                    if (p.getName().equals("SampleBase")) {
                        base = Long.parseLong(p.getValue());
                        continue;
                    }
                    if (p.getName().equals("SampleExponent")) {
                        exp = Long.parseLong(p.getValue());
                        continue;
                    }
                    if (!p.getName().equals("Number")) continue;
                    groupN = Integer.parseInt(p.getValue());
                }
                if (groupN == -1 || base == -1L || exp == -1L) {
                    return null;
                }
                arry[groupN] = (long)((double)base * Math.pow(10.0, exp));
            }
            return arry;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @XmlTransient
    public long[] getGroupSampleTimes_ns() {
        long[] retVal = this.getGroupSampleRates();
        if (retVal != null) {
            for (int i = 0; i < retVal.length; ++i) {
                retVal[i] = 1000000000L / retVal[i];
            }
        }
        return retVal;
    }
}

