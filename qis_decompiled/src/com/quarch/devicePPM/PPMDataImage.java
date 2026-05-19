/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devicePPM;

import javax.swing.event.EventListenerList;
import src.com.quarch.devicePPM.EnumPPMAverage;
import src.com.quarch.devicePPM.EnumPPMDataImageIds;
import src.com.quarch.devicePPM.EnumPPMTrigger;
import src.com.quarch.devicePPM.PPMDataImageEvent;
import src.com.quarch.devicePPM.PPMDataImageEventListener;

public class PPMDataImage {
    protected EventListenerList listenerList = new EventListenerList();
    private boolean Connected = false;
    private String SerialNumber = "";
    private EnumPPMAverage Averaging;
    private EnumPPMTrigger Trigger;
    public final int avgRawDataRate = 4;
    public final int avgRawDataUnits = 1;
    public EnumPPMDataImageIds dataChanged;

    public PPMDataImage() {
        this.setSerialNumber("");
    }

    public void removeMyEventListener(PPMDataImageEventListener listener) {
        this.listenerList.remove(PPMDataImageEventListener.class, listener);
    }

    void fireMyEvent(PPMDataImageEvent evt) {
        Object[] listeners = this.listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] != PPMDataImageEventListener.class) continue;
            ((PPMDataImageEventListener)listeners[i + 1]).PPMDataImageEventOccurred(evt);
        }
    }

    public void addMyEventListener(PPMDataImageEventListener ppmDataImageEventListener) {
        this.listenerList.add(PPMDataImageEventListener.class, ppmDataImageEventListener);
    }

    public boolean isConnected() {
        return this.Connected;
    }

    public void setConnected(boolean connected) {
        if (this.Connected != connected) {
            this.Connected = connected;
            this.dataChanged = EnumPPMDataImageIds.CONNECTED;
            this.fireMyEvent(new PPMDataImageEvent(this));
        }
    }

    public String getSerialNumber() {
        return this.SerialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.SerialNumber = serialNumber;
    }

    public EnumPPMAverage getAveraging() {
        return this.Averaging;
    }

    public void setAveraging(EnumPPMAverage averaging) {
        if (this.Averaging != averaging) {
            this.Averaging = averaging;
            this.dataChanged = EnumPPMDataImageIds.AVERAGING;
            this.fireMyEvent(new PPMDataImageEvent(this));
        }
    }

    public EnumPPMTrigger getTrigger() {
        return this.Trigger;
    }

    public void setTrigger(EnumPPMTrigger trigger) {
        if (this.Trigger != trigger) {
            this.Trigger = trigger;
            this.dataChanged = EnumPPMDataImageIds.TRIGGER;
            this.fireMyEvent(new PPMDataImageEvent(this));
        }
    }
}

