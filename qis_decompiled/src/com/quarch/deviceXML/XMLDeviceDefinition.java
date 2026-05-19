/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLoggerInterface
 *  jakarta.xml.bind.JAXBContext
 *  jakarta.xml.bind.JAXBException
 *  jakarta.xml.bind.Marshaller
 *  jakarta.xml.bind.Unmarshaller
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlRootElement
 */
package src.com.quarch.deviceXML;

import QuarchLogging.QuarchLoggerInterface;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import src.com.quarch.deviceXML.Feature;
import src.com.quarch.deviceXML.Features;
import src.com.quarch.deviceXML.Identity;
import src.com.quarch.deviceXML.Param;
import src.com.quarch.deviceXML.ParamList;
import src.com.quarch.deviceXML.PolledMeasurements;
import src.com.quarch.deviceXML.SelfTest;
import src.com.quarch.deviceXML.Supply;

@XmlRootElement(name="XmlResponse")
public class XMLDeviceDefinition {
    @XmlElement
    public Identity Identity;
    @XmlElement(name="Supply")
    public Supply supply;
    @XmlElement(name="Measure")
    public Features measure;
    @XmlElement(name="Triggering")
    public Features triggering;
    @XmlElement(name="SelfTest")
    public SelfTest selfTest;
    @XmlElement(name="PolledMeasurements")
    public PolledMeasurements polledMeasurements;
    private static boolean enableDebugOutput = false;

    public static XMLDeviceDefinition createFromXML(String path, String fileName) {
        XMLDeviceDefinition deviceDefinition = null;
        String fullFileName = path + fileName;
        try {
            JAXBContext jc = JAXBContext.newInstance((Class[])new Class[]{XMLDeviceDefinition.class});
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            File xml = new File(fullFileName);
            deviceDefinition = (XMLDeviceDefinition)unmarshaller.unmarshal(xml);
            if (enableDebugOutput) {
                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty("jaxb.formatted.output", (Object)true);
                marshaller.marshal((Object)deviceDefinition, (OutputStream)System.out);
            }
        }
        catch (JAXBException e) {
            e.printStackTrace();
            QuarchLoggerInterface.logToDefault((String)"createFromXML", (Level)Level.SEVERE, (String)(" Error In: " + fullFileName));
        }
        return deviceDefinition;
    }

    public static XMLDeviceDefinition createFromXML(String xmlData) {
        XMLDeviceDefinition deviceDefinition = null;
        try {
            StringReader reader = new StringReader(xmlData);
            JAXBContext jc = JAXBContext.newInstance((Class[])new Class[]{XMLDeviceDefinition.class});
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            deviceDefinition = (XMLDeviceDefinition)unmarshaller.unmarshal((Reader)reader);
            if (enableDebugOutput) {
                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty("jaxb.formatted.output", (Object)true);
                marshaller.marshal((Object)deviceDefinition, (OutputStream)System.out);
            }
        }
        catch (JAXBException e) {
            e.printStackTrace();
            QuarchLoggerInterface.logToDefault((String)"createFromXML", (Level)Level.SEVERE, (String)(" Error In: " + xmlData));
        }
        return deviceDefinition;
    }

    public static String getStreamType(XMLDeviceDefinition devDef) {
        if (devDef.measure != null) {
            for (Feature feature : devDef.measure.features) {
                if (!feature.getName().equals("SampleEngine")) continue;
                for (Param param : feature.getParam()) {
                    if (!param.getName().equals("StreamType")) continue;
                    return param.getValue();
                }
            }
        }
        return null;
    }

    public int[] getAverageArray() {
        int[] retVal = null;
        try {
            if (this.measure != null) {
                for (Feature f1 : this.measure.features) {
                    if (!f1.getName().equals("SampleEngine")) continue;
                    for (ParamList p1 : f1.getParamList()) {
                        if (!p1.getName().equals("HwAveraging")) continue;
                        List<String> valueList = p1.getValueList();
                        int size = valueList.size();
                        int[] arry = new int[size];
                        for (int i = 0; i < size; ++i) {
                            String strVal = valueList.get(i);
                            if (strVal.toLowerCase().endsWith("k")) {
                                strVal = strVal.substring(0, strVal.length() - 1);
                                arry[i] = Integer.parseInt(strVal) * 1024;
                            } else {
                                arry[i] = Integer.parseInt(strVal);
                            }
                            if (arry[i] != 0) continue;
                            arry[i] = 1;
                        }
                        retVal = arry;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public boolean hasFixtureSupport() {
        if (this.measure != null) {
            for (Feature f1 : this.measure.features) {
                if (!f1.getName().equals("FixtureSupport")) continue;
                return true;
            }
        }
        return false;
    }
}

