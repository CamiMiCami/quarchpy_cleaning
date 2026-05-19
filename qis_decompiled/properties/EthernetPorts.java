/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlElement
 */
package properties;

import jakarta.xml.bind.annotation.XmlElement;

public class EthernetPorts {
    private int telnet = -1;
    private int rest = -1;
    private int tcp = -1;
    private int scan = -1;
    private static final int portNoUsed = -666;
    public static final String portNoUsedStr = "NA";

    public EthernetPorts() {
    }

    public EthernetPorts(String telnetStr, String restStr, String tcpStr, String scanStr) {
        this.telnet = this.decodeStr(telnetStr);
        this.rest = this.decodeStr(restStr);
        this.tcp = this.decodeStr(tcpStr);
        this.scan = this.decodeStr(scanStr);
    }

    private int decodeStr(String str) {
        int retVal = -666;
        if (!str.toLowerCase().startsWith("na")) {
            retVal = Integer.valueOf(str);
        }
        return retVal;
    }

    private String toString(int port) {
        if (port == -666) {
            return portNoUsedStr;
        }
        return Integer.toString(port);
    }

    @XmlElement(name="telnet")
    public String getTelnet() {
        return this.toString(this.telnet);
    }

    public int getTelnetInt() {
        return this.telnet;
    }

    public void setTelnet(int telnet) {
        this.telnet = telnet;
    }

    public void setTelnet(String port) {
        this.telnet = this.decodeStr(port);
    }

    @XmlElement(name="rest")
    public String getRest() {
        return this.toString(this.rest);
    }

    public int getRestInt() {
        return this.rest;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public void setRest(String port) {
        this.rest = this.decodeStr(port);
    }

    @XmlElement(name="tcp")
    public String getTcp() {
        return this.toString(this.tcp);
    }

    public int getTcpInt() {
        return this.tcp;
    }

    public void setTcp(int tcp) {
        this.tcp = tcp;
    }

    public void setTcp(String port) {
        this.tcp = this.decodeStr(port);
    }

    @XmlElement(name="scan")
    public String getScan() {
        return this.toString(this.scan);
    }

    public int getScanInt() {
        return this.scan;
    }

    public void setScan(int scan) {
        this.scan = scan;
    }

    public void setScan(String value) {
        this.scan = this.decodeStr(value);
    }
}

