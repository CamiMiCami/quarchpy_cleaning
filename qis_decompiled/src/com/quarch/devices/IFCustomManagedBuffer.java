/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.nio.ByteBuffer;

public interface IFCustomManagedBuffer {
    public ByteBuffer getUnderlyingBuffer();

    public void freeBuffer();
}

