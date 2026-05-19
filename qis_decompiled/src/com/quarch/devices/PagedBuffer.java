/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beStream.PPMResample;
import src.com.quarch.devices.BufferPage;
import src.com.quarch.utils.DebugUtil;

public class PagedBuffer {
    static final int s64k = 65536;
    static final int s48k = 49152;
    static final int defaultPageSize = 65536;
    static final int defaultMaxPagesTotalBytes = 80000000;
    private int pageSize;
    private int maxPageCount;
    private AtomicInteger pageBufferInUsePageCount = new AtomicInteger(0);
    private AtomicInteger pageAllocatedCount = new AtomicInteger(0);
    private AtomicBoolean fullPageRequested = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<BufferPage> bufferPoolFreeQ = new ConcurrentLinkedQueue();
    private final ConcurrentLinkedQueue<BufferPage> bufferPoolFullQ = new ConcurrentLinkedQueue();
    private AtomicInteger totalDataEntriesInFullQ = new AtomicInteger();
    private PPMResample ppmResample;

    public PagedBuffer() {
        this.initialise(65536, (int)Math.ceil(1220.0));
    }

    public PagedBuffer(int pageSize, int maxPages) {
        this.initialise(pageSize, maxPages);
    }

    private void initialise(int pageSize, int maxPages) {
        this.pageSize = pageSize;
        this.maxPageCount = maxPages;
    }

    public boolean isOutOfSpace() {
        return this.pageBufferInUsePageCount.get() >= this.maxPageCount;
    }

    public BufferPage getFreeBufferPage() {
        DebugUtil.devDebugMsgln("getFreeBufferPage:pageBufferInUsePageCount (max " + this.maxPageCount + " ): " + this.pageBufferInUsePageCount + " pageAllocatedCount (NB, return to FreeQ now implimented) " + this.pageAllocatedCount);
        if (this.bufferPoolFreeQ.isEmpty()) {
            if (!this.isOutOfSpace()) {
                this.pageBufferInUsePageCount.incrementAndGet();
                this.pageAllocatedCount.incrementAndGet();
                return new BufferPage(this.pageSize, this.bufferPoolFreeQ);
            }
            return null;
        }
        BufferPage retVal = this.bufferPoolFreeQ.poll();
        if (retVal != null) {
            retVal.clear();
        }
        return retVal;
    }

    public BufferPage getFullBufferPage() {
        if (this.bufferPoolFullQ.isEmpty()) {
            this.setFullBufferPageRequested();
            return null;
        }
        BufferPage entry = this.bufferPoolFullQ.poll();
        this.totalDataEntriesInFullQ.addAndGet(-1 * entry.getNumberOfDataGroupEntries());
        return entry;
    }

    private void setFullBufferPageRequested() {
        this.fullPageRequested.set(true);
    }

    private void clearFullBufferPageRequested() {
        this.fullPageRequested.set(false);
    }

    public boolean isFullPageRequested() {
        return this.fullPageRequested.get();
    }

    public BufferPage getEmptyBufferPage() {
        BufferPage empty = this.getFreeBufferPage();
        return empty;
    }

    public void freeBuferPage(BufferPage bp) {
        bp.reset();
        this.bufferPoolFreeQ.add(bp);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void qFullBufferPage(BufferPage bp) {
        ByteBuffer data = bp.getData();
        int position = data.position();
        data.limit(position);
        data.putInt(4, position);
        bp.setEntryCounts();
        if (DebugUtil.isEnableDevDebug()) {
            PagedBuffer.printBufferDebug(" qFullBufferPage: ", bp);
        }
        data.rewind();
        ConcurrentLinkedQueue<BufferPage> concurrentLinkedQueue = this.bufferPoolFullQ;
        synchronized (concurrentLinkedQueue) {
            this.bufferPoolFullQ.add(bp);
            this.totalDataEntriesInFullQ.addAndGet(bp.getNumberOfDataGroupEntries());
        }
        this.clearFullBufferPageRequested();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getTotalBufferedDataGroupEntries(BufferPage currentLiveBufferPage) {
        int retVal = 0;
        ConcurrentLinkedQueue<BufferPage> concurrentLinkedQueue = this.bufferPoolFullQ;
        synchronized (concurrentLinkedQueue) {
            if (currentLiveBufferPage != null) {
                retVal = currentLiveBufferPage.getNumberOfDataGroupEntries();
            }
        }
        return retVal += this.totalDataEntriesInFullQ.get();
    }

    private void clearBufferPoolFreeQ() {
        this.pageBufferInUsePageCount.set(0);
        this.pageAllocatedCount.set(0);
        this.bufferPoolFreeQ.clear();
    }

    private void clearBufferPoolFullQ() {
        this.totalDataEntriesInFullQ.set(0);
        this.bufferPoolFullQ.clear();
    }

    public void clear() {
        this.clearBufferPoolFreeQ();
        this.clearBufferPoolFullQ();
    }

    public BufferPage honourFullPageRequest(BufferPage binaryStreamDataBuffer) {
        if (this.isFullPageRequested()) {
            DebugUtil.devDebugMsgln(System.currentTimeMillis() + " Page Requested");
            if (binaryStreamDataBuffer != null && !binaryStreamDataBuffer.isEmpty()) {
                this.qFullBufferPage(binaryStreamDataBuffer);
                binaryStreamDataBuffer = null;
            } else {
                DebugUtil.devDebugMsgln(System.currentTimeMillis() + " No Page Ready");
            }
        }
        return binaryStreamDataBuffer;
    }

    public static void printBufferDebug(String txt, BufferPage binaryStreamDataBuffer) {
        ByteBuffer data = binaryStreamDataBuffer.getData();
        int posSave = data.position();
        data.rewind();
        try {
            DebugUtil.devDebugMsgln(System.currentTimeMillis() + txt + "Queued: " + data.getShort() + " " + data.getShort() + " Size:" + data.getInt() + " [" + data.getInt() + "," + data.getInt() + "," + data.getInt() + "," + data.getInt() + "," + data.getInt() + "]");
        }
        catch (Exception e) {
            DebugUtil.devDebugMsgln(System.currentTimeMillis() + txt + "Error creating Debug Message, insufficient data. Only " + posSave + " bytes in the buffer. At least 28 bytes expected");
        }
        data.position(posSave);
    }

    public void ppmHelperConfigureResampling(boolean resamplingActive, int devicePerioduS, int requestedResampleuS, int dataFields) {
        this.ppmResample = new PPMResample(resamplingActive, devicePerioduS, requestedResampleuS, dataFields);
    }

    public PPMResample getPpmResample() {
        return this.ppmResample;
    }

    public void printDebugInfo() {
        System.out.println("bufferPoolFreeQ is empty: " + this.bufferPoolFreeQ.isEmpty());
        System.out.println("pageBufferInUsePageCount: " + this.pageBufferInUsePageCount);
        System.out.println("maxPageCount: " + this.maxPageCount);
        System.out.println("bufferPoolFullQ is empty: " + this.bufferPoolFullQ.isEmpty());
        System.out.println("Full page requested: " + this.fullPageRequested.get());
    }
}

