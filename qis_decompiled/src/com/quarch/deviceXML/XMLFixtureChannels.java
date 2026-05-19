/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.JAXBContext
 *  jakarta.xml.bind.JAXBException
 *  jakarta.xml.bind.Unmarshaller
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlRootElement
 */
package src.com.quarch.deviceXML;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Reader;
import java.io.StringReader;
import src.com.quarch.deviceXML.Channels;
import src.com.quarch.deviceXML.Features;
import src.com.quarch.deviceXML.General;
import src.com.quarch.deviceXML.XMLFixChlsValidator;

@XmlRootElement(name="XmlResponse")
public class XMLFixtureChannels {
    @XmlElement(name="General")
    public General general;
    @XmlElement(name="Features")
    public Features features;
    @XmlElement(name="Channels")
    public Channels channels;

    public XMLFixChlsValidator validator() {
        return new XMLFixChlsValidator(this);
    }

    public static XMLFixtureChannels createFromXML(String xmlData) {
        XMLFixtureChannels fixtureChannelsXML = null;
        try {
            StringReader reader = new StringReader(xmlData);
            JAXBContext jc = JAXBContext.newInstance((Class[])new Class[]{XMLFixtureChannels.class});
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            fixtureChannelsXML = (XMLFixtureChannels)unmarshaller.unmarshal((Reader)reader);
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
        return fixtureChannelsXML;
    }
}

