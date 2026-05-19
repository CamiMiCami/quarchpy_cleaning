/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  commsInterface.CommsInterfaceType
 */
package src.com.quarch.beStream;

import commsInterface.CommsInterfaceType;
import src.com.quarch.beStream.StreamBase;
import src.com.quarch.beStream.TCPStream;
import src.com.quarch.beStream.USBStream;
import src.com.quarch.deviceInterface.DeviceListEntry;

public final class StreamFactory {
    private static volatile StreamFactory instance;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static StreamFactory getInstance() {
        if (instance != null) return instance;
        Class<StreamFactory> clazz = StreamFactory.class;
        synchronized (StreamFactory.class) {
            if (instance != null) return instance;
            instance = new StreamFactory();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    private StreamBase createDeviceStream(DeviceListEntry dle) {
        if (dle.deviceInfo.getqDevice().getCurrentCommsInterfaceType() == CommsInterfaceType.USB) {
            return new USBStream(dle);
        }
        if (dle.deviceInfo.getqDevice().getCurrentCommsInterfaceType() == CommsInterfaceType.ETHTCP) {
            return new TCPStream(dle);
        }
        return null;
    }

    public StreamBase createNewStream(DeviceListEntry dle) {
        if (dle.deviceInfo.isDeviceHasStream()) {
            return this.createDeviceStream(dle);
        }
        return null;
    }
}

