/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import src.com.quarch.devices.IFCustomManagedBuffer;

public class BufferPage
implements IFCustomManagedBuffer {
    private ByteBuffer data;
    private int pageSize;
    private int[] entryCounters = new int[1];
    private int entryCountersStartPosition;
    private AtomicBoolean inUse = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<BufferPage> parentReturnQ;
    public int positionAfterInitialisation;

    public BufferPage(int pageSize, ConcurrentLinkedQueue<BufferPage> parentReturnQ) {
        this.initialise(pageSize);
        this.parentReturnQ = parentReturnQ;
    }

    private void initialise(int pageSize) {
        this.setPageSize(pageSize);
        this.setData(ByteBuffer.allocateDirect(pageSize));
    }

    int getPageSize() {
        return this.pageSize;
    }

    void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public ByteBuffer getData() {
        return this.data;
    }

    void setData(ByteBuffer data) {
        this.data = data;
    }

    AtomicBoolean getInUse() {
        return this.inUse;
    }

    void setInUse(AtomicBoolean inUse) {
        this.inUse = inUse;
    }

    public void reset() {
        this.data.clear();
    }

    public boolean isSpaceFor(int size) {
        return this.data.remaining() > size;
    }

    public boolean isEmpty() {
        return this.data.position() == this.positionAfterInitialisation;
    }

    public void createEntryCounts(int nGroups) {
        if (this.entryCounters.length != nGroups) {
            this.entryCounters = new int[nGroups];
        }
        this.entryCountersStartPosition = this.getData().position();
        for (int i = 0; i < this.entryCounters.length; ++i) {
            this.entryCounters[i] = 0;
            this.getData().putInt(0);
        }
    }

    public void skipOverEntryCounts() {
        this.getData().position(this.entryCountersStartPosition);
        for (int i = 0; i < this.entryCounters.length; ++i) {
            this.getData().getInt();
        }
    }

    public void incEntryCounter(int groupId) {
        int n = groupId;
        this.entryCounters[n] = this.entryCounters[n] + 1;
    }

    public void setEntryCounts() {
        this.getData().position(this.entryCountersStartPosition);
        for (int i = 0; i < this.entryCounters.length; ++i) {
            this.getData().putInt(this.entryCounters[i]);
        }
    }

    public int getNumberOfDataGroupEntries() {
        int retVal = 0;
        for (int count : this.entryCounters) {
            retVal += count;
        }
        return retVal;
    }

    @Override
    public ByteBuffer getUnderlyingBuffer() {
        return this.getData();
    }

    @Override
    public void freeBuffer() {
        this.clear();
        this.parentReturnQ.add(this);
    }

    public void clear() {
        this.getData().clear();
    }

    public int getNumberOfGroups() {
        return this.entryCounters.length;
    }
}

