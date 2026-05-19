/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlRootElement
 */
package properties;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import properties.EthernetPorts;
import src.com.quarch.utils.DebugUtil;

@XmlRootElement(name="Properties")
public class AppPropertiesData {
    public String lastModified = "";
    private EthernetPorts localPorts = null;
    private int defaultTCPTimeout = 5000;
    private int defaultTCPSemaphoreTimeout = 5000;

    @XmlElement(name="localPorts")
    public EthernetPorts getLocalPorts() {
        if (this.localPorts == null) {
            DebugUtil.debugMsgln("Getting Local Ports : Is NULL!");
            this.setDefaultPorts();
        } else {
            DebugUtil.debugMsgln("Getting Local Ports " + this.localPorts.toString());
        }
        return this.localPorts;
    }

    public void setDefaultPorts() {
        this.setLocalPorts(new EthernetPorts("9722", "9780", "NA", "-1"));
    }

    public void setLocalPorts(EthernetPorts localPorts) {
        DebugUtil.debugMsgln("Setting Local Ports " + localPorts.toString());
        this.localPorts = localPorts;
    }

    @XmlElement(name="defaultTCPTimeout")
    public int getDefaultTCPTimeout() {
        return this.defaultTCPTimeout;
    }

    public void setDefaultTCPTimeout(int defaultTCPTimeout) {
        this.defaultTCPTimeout = defaultTCPTimeout;
    }

    @XmlElement(name="defaultTCPSemaphoreTimeout")
    public int getDefaultTCPSemaphoreTimeout() {
        return this.defaultTCPSemaphoreTimeout;
    }

    public void setDefaultTCPSemaphoreTimeout(int defaultTCPSemaphoreTimeout) {
        this.defaultTCPSemaphoreTimeout = defaultTCPSemaphoreTimeout;
    }
}

