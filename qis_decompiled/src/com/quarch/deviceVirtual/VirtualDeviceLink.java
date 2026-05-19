/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceVirtual;

import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class VirtualDeviceLink {
    final DeviceListEntry dle;
    final String prefix;
    AtomicInteger groupOffset = new AtomicInteger(0);
    int groupCount = -1;

    public VirtualDeviceLink(DeviceListEntry dle, String prefix) {
        this.dle = dle;
        this.prefix = prefix;
    }
}

