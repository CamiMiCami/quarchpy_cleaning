/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.beStream.StreamDataReadyListener;
import src.com.quarch.beStream.StreamStatusCodes;

public class StreamDataReadyInitiater {
    private List<StreamDataReadyListener> listeners = new ArrayList<StreamDataReadyListener>();

    public void addDataReadyListener(StreamDataReadyListener toAdd) {
        if (this.listeners.contains(toAdd)) {
            this.listeners.remove(toAdd);
        }
        this.listeners.add(toAdd);
    }

    public void fireDataReadyEvent(StreamStatusCodes code, int currentBufferSize) {
        for (StreamDataReadyListener hl : this.listeners) {
            hl.streamDataReadyEvent(code, currentBufferSize);
        }
    }
}

