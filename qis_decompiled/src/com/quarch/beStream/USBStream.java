/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

import src.com.quarch.beStream.StreamBase;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.devices.DeviceDataBuffer;

public class USBStream
extends StreamBase {
    public USBStream(DeviceListEntry dle) {
        super(dle);
    }

    @Override
    public void stop() {
        this.exitThread = true;
    }

    @Override
    public void run() {
        int bufferSize = this.myDle.deviceInfo.streamBufferSize;
        this.thread = Thread.currentThread();
        this.timeCount = System.currentTimeMillis();
        this.statusCode.set(1);
        while (!this.exitThread) {
            this.statusCode.set(2);
            this.activityCount.incrementAndGet();
            DeviceDataBuffer ddBuff = this.myDle.deviceInfo.readStream();
            if (ddBuff != null) {
                this.statusCode.set(3);
                this.lastReadLen.set(ddBuff.len);
                this.lastNZReadLen.set(ddBuff.len);
                this.streamEventInitiater.fireStreamEvent(ddBuff);
                this.timeCount = System.currentTimeMillis();
                continue;
            }
            this.lastReadLen.set(0);
            if (System.currentTimeMillis() - this.timeCount <= 500L) continue;
            this.statusCode.set(4);
            ddBuff = new DeviceDataBuffer();
            ddBuff.len = 1;
            ddBuff.buffer = new byte[]{99};
            this.streamEventInitiater.fireStreamEvent(ddBuff);
            this.timeCount = System.currentTimeMillis();
        }
        this.gracefulExit = true;
    }
}

