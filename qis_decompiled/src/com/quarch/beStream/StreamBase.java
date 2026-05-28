/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beStream.RawStripeDataHeader;
import src.com.quarch.beStream.StreamDataReadyInitiater;
import src.com.quarch.beStream.StreamEventInitiater;
import src.com.quarch.beStream.StreamEventListener;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.DeviceDataBuffer;

public class StreamBase extends StreamDataReadyInitiater implements StreamEventListener,
Runnable {
    static List<StreamBase> streamList = new ArrayList<StreamBase>();
    protected volatile Object thread = Thread.State.NEW;
    public StreamEventInitiater streamEventInitiater;
    protected DeviceListEntry myDle;
    protected long timeCount;
    public boolean exitThread = false;
    public boolean gracefulExit = false;
    public AtomicInteger activityCount = new AtomicInteger(0);
    public AtomicInteger statusCode = new AtomicInteger(-1);
    protected AtomicInteger lastReadLen = new AtomicInteger(0);
    protected AtomicInteger lastNZReadLen = new AtomicInteger(0);

    public static List<String> getStreamList() {
        ArrayList<String> retVal = new ArrayList<String>();
        for (StreamBase sb : streamList) {
            retVal.add(sb.getStatus());
        }
        return retVal;
    }

    public StreamBase(DeviceListEntry dle) {
        this.myDle = dle;
        this.streamEventInitiater = new StreamEventInitiater();
        streamList.add(this);
    }

    public void stop() {
    }

    @Override
    public void run() {
    }

    public RawStripeDataHeader decodeRawStripeDataHeader(byte[] data) {
        RawStripeDataHeader retVal = new RawStripeDataHeader(data);
        return retVal;
    }

    @Override
    public void streamEvent(DeviceDataBuffer ddBuff) {
    }

    public String getStatusString() {
        String retVal = "N/A";
        int ai = this.statusCode.get();
        if (ai == -1) {
            retVal = "Not Started";
        }
        if (ai == 0) {
            retVal = "Invalid";
        }
        if (ai == 1) {
            retVal = "Thread Starting";
        }
        if (ai == 2) {
            retVal = "Await Data";
        }
        if (ai == 3) {
            retVal = "Processing Data";
        }
        if (ai == 4) {
            retVal = "Timeout Occurred";
        }
        if (ai == 5) {
            retVal = "Thread Exit";
        }
        return retVal;
    }

    public String getStatus() {
        String retVal = "";
        retVal = this.myDle.getDeviceIdStr() + ": " + this.getStateString() + ", " + this.getStatusString() + ", Count " + this.activityCount.get() + ", Exit " + this.exitThread + ", Graceful " + this.gracefulExit + ", LR " + this.lastReadLen.get() + ", LNZR " + this.lastNZReadLen.get();
        return retVal;
    }

    public Thread.State getState() {
        Object t = this.thread;
        return t instanceof Thread.State ? (Thread.State)((Object)t) : ((Thread)t).getState();
    }

    public String getStateString() {
        String retVal = "";
        switch (this.getState()) {
            case BLOCKED: {
                retVal = "Blocked";
                break;
            }
            case NEW: {
                retVal = "New";
                break;
            }
            case RUNNABLE: {
                retVal = "Runable";
                break;
            }
            case TERMINATED: {
                retVal = "Terminated";
                break;
            }
            case TIMED_WAITING: {
                retVal = "Timed_Waiting";
                break;
            }
            case WAITING: {
                retVal = "Waiting";
                break;
            }
            default: {
                retVal = "Unknown";
            }
        }
        return retVal;
    }
}

