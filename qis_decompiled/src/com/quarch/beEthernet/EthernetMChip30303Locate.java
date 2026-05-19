/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLoggerInterface
 */
package src.com.quarch.beEthernet;

import QuarchLogging.QuarchLoggerInterface;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import properties.AppProperties;
import src.com.quarch.utils.DebugUtil;

public class EthernetMChip30303Locate
extends Thread
implements QuarchLoggerInterface {
    static String[] OUI_Microchip_byte = new String[]{"00-04-A3", "00-1E-C0", "D8-80-39"};
    private static final int defaultSocket = 30303;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private String discoveryString = new String("Discovery: Who is out there?\u0000\n");
    private String received = " ";
    private int defaultScanPort = -1;
    private Thread recvThread = null;

    public EthernetMChip30303Locate(int defaultScanPort) {
        this.defaultScanPort = defaultScanPort;
    }

    public boolean udpAction() {
        boolean retVal = false;
        try {
            if (AppProperties.scanBindIpAddress.isEmpty()) {
                this.socket = this.defaultScanPort == -1 ? new DatagramSocket() : new DatagramSocket(this.defaultScanPort);
            } else if (this.defaultScanPort == -1) {
                this.socket = new DatagramSocket(30303, InetAddress.getByName(AppProperties.scanBindIpAddress));
            }
            this.socket.setBroadcast(true);
            InetAddress address = InetAddress.getByName("255.255.255.255");
            this.packet = new DatagramPacket(this.discoveryString.getBytes(), this.discoveryString.length(), address, EthernetMChip30303Locate.getDefaultsocket());
            DebugUtil.debugMsgln("Sending NW packet");
            this.socket.send(this.packet);
            this.start();
            retVal = true;
        }
        catch (IOException e) {
            DebugUtil.debugMsgln("Scan udpAction: Exception");
            this.logToDefault(Level.WARNING, "Failed to bind to IP address [" + AppProperties.scanBindIpAddress + "]");
            retVal = false;
        }
        return retVal;
    }

    public static void sendBuffer(byte[] buffer) throws IOException {
        DatagramSocket localSocket = new DatagramSocket();
        localSocket.setBroadcast(true);
        InetAddress address = InetAddress.getByName("255.255.255.255");
        DatagramPacket localPacket = new DatagramPacket(buffer, buffer.length, address, EthernetMChip30303Locate.getDefaultsocket());
        localSocket.send(localPacket);
        try {
            Thread.sleep(100L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        localSocket.close();
    }

    public static void sendString(String str) throws IOException {
        EthernetMChip30303Locate.sendBuffer(str.getBytes());
    }

    public void close_action() {
        if (this.socket != null && this.socket.isConnected()) {
            this.socket.close();
        }
    }

    public String manualLocate(String ipAddress) {
        return "Not Found";
    }

    public static int getDefaultsocket() {
        return 30303;
    }
}

