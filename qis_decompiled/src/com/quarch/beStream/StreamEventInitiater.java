/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.beStream.StreamEventListener;
import src.com.quarch.devices.DeviceDataBuffer;

public class StreamEventInitiater {
    private List<StreamEventListener> listeners = new ArrayList<StreamEventListener>();

    public void addListener(StreamEventListener toAdd) {
        this.listeners.add(toAdd);
    }

    public void fireStreamEvent(DeviceDataBuffer ddBuff) {
        for (StreamEventListener hl : this.listeners) {
            hl.streamEvent(ddBuff);
        }
    }
}

