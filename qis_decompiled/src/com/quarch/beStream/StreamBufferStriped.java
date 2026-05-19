/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beStream;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beStream.PPMResample;
import src.com.quarch.beStreamData.StreamRawData;

public class StreamBufferStriped {
    private int maxRamStripes;
    private Long seqNum = 0L;
    private final BufferPool retiredStripeBuffer = new BufferPool();
    private final Semaphore listLock = new Semaphore(1, true);
    public static final int maxStripeData = 7;
    private final BufferQueue stripeBufferQ = new BufferQueue();
    private final BufferQueue preCommitStripeBufferQ = new BufferQueue();
    private static ExecutorService executor = Executors.newWorkStealingPool();
    public AtomicBoolean commitInProgress = new AtomicBoolean(false);
    private PPMResample ppmResample;
    private int simulateDelayMs = -1;
    private long lastSimulateStart = 0L;

    public StreamBufferStriped() {
        this.maxRamStripes = (int)Math.pow(2.0, 23.0);
        this.clear();
    }

    private void testCode() {
        BufferManager bm = new BufferManager(65536, 13);
        BufferData buff = bm.getWorkingBuffer();
        buff.data.put((byte)1);
    }

    public void clear() {
        try {
            this.listLock.acquire();
            try {
                this.seqNum = 0L;
                this.stripeBufferQ.clear();
                this.retiredStripeBuffer.clear();
                this.preCommitStripeBufferQ.clear();
            }
            finally {
                this.listLock.release();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void bufferForCommit(boolean trigger, int[] stripe) {
        BufferListEntry ble = this.retiredStripeBuffer.getEntryQ(trigger, stripe);
        if (ble == null) {
            System.out.println("bfc NULL");
        } else {
            this.preCommitStripeBufferQ.add(ble);
        }
    }

    public void commitBufferedData() {
        if (!this.preCommitStripeBufferQ.isEmpty()) {
            executor.submit(() -> {
                this.commitInProgress.set(true);
                this.stripeBufferQ.addAll(this.preCommitStripeBufferQ);
                this.preCommitStripeBufferQ.clear();
            });
        }
    }

    public void configureResampling(boolean resamplingActive, int devicePerioduS, int requestedResampleuS, int dataFields) {
        this.ppmResample = new PPMResample(resamplingActive, devicePerioduS, requestedResampleuS, dataFields);
    }

    public void add(boolean trigger, int[] stripe) {
        if (this.ppmResample.isResampleActive()) {
            boolean stripeTrigger = this.ppmResample.isResampleTrigger();
            stripe = this.ppmResample.resampleData(trigger, stripe);
        }
        if (stripe != null && this.stripeBufferQ.size() < this.maxRamStripes) {
            BufferListEntry ble = this.retiredStripeBuffer.getEntryQ(trigger, stripe);
            this.stripeBufferQ.add(ble);
        }
    }

    public int size() {
        int sze = this.stripeBufferQ.size();
        return sze;
    }

    public int getMaxRamStripes() {
        return this.maxRamStripes;
    }

    public void setMaxRamStripes(int maxRamStripes) {
        this.maxRamStripes = maxRamStripes;
    }

    public StreamRawData getOldestStripe(StreamRawData modifiableRawData) {
        BufferListEntry ble = this.stripeBufferQ.poll();
        if (ble != null) {
            Long l = this.seqNum;
            Long l2 = this.seqNum = Long.valueOf(this.seqNum + 1L);
            modifiableRawData.seqNum = l;
            modifiableRawData.stripeTrigger = ble.stripeTrigger;
            modifiableRawData.stripe = ble.stripe;
            this.retiredStripeBuffer.returnEntryQ(ble);
            return modifiableRawData;
        }
        return null;
    }

    public StreamRawData getOldestStripe() {
        StreamRawData retVal = null;
        BufferListEntry ble = this.stripeBufferQ.poll();
        if (ble != null) {
            this.retiredStripeBuffer.returnEntryQ(ble);
            Long l = this.seqNum;
            Long l2 = this.seqNum = Long.valueOf(this.seqNum + 1L);
            retVal = new StreamRawData(l, ble.stripeTrigger, ble.stripe);
        }
        return retVal;
    }

    public StreamRawData getSimulatedStripe(int interStripeDelay, int numberOfDataElements) {
        StreamRawData retVal = null;
        if (System.currentTimeMillis() - this.lastSimulateStart > (long)this.simulateDelayMs) {
            this.simulateDelayMs = -1;
        }
        if (this.simulateDelayMs < 0) {
            this.simulateDelayMs = interStripeDelay;
            this.lastSimulateStart = System.currentTimeMillis();
            int[] simData = new int[numberOfDataElements];
            for (int i = 0; i < simData.length; ++i) {
                simData[i] = (int)(1000.0 + (double)(100 * i) * Math.sin(this.seqNum / (long)(i + 1)));
            }
            Long l = this.seqNum;
            Long l2 = this.seqNum = Long.valueOf(this.seqNum + 1L);
            retVal = new StreamRawData(l, true, simData);
        }
        return retVal;
    }

    public void addPPMStripeToBuffer(boolean stripeTrigger, int[] decodedStripe) {
        this.add(stripeTrigger, decodedStripe);
    }

    public class BufferQueue {
        ConcurrentLinkedQueue<BufferListEntry> queue = new ConcurrentLinkedQueue();
        private final AtomicInteger entryCount = new AtomicInteger(0);

        public BufferQueue() {
            this.clear();
        }

        public void add(BufferListEntry ble) {
            if (ble != null) {
                this.queue.add(ble);
                this.entryCount.incrementAndGet();
            }
        }

        public BufferListEntry poll() {
            BufferListEntry ble = this.queue.poll();
            if (ble != null) {
                this.entryCount.decrementAndGet();
            }
            return ble;
        }

        public int size() {
            return this.entryCount.get();
        }

        public void clear() {
            this.entryCount.set(0);
            this.queue.clear();
        }

        public ConcurrentLinkedQueue<BufferListEntry> getQueue() {
            return this.queue;
        }

        public boolean isEmpty() {
            if (this.queue.isEmpty()) {
                int count = this.entryCount.get();
                if (count != 0) {
                    System.out.println("Buff Q Count 0 missmatch " + count);
                }
                return true;
            }
            return false;
        }

        public void addAll(ConcurrentLinkedQueue<BufferListEntry> q) {
            for (BufferListEntry ble : q) {
                if (ble == null) continue;
                this.add(ble);
            }
        }

        public void addAll(BufferQueue bQ) {
            this.addAll(bQ.getQueue());
        }
    }

    protected class BufferPool {
        private final LinkedList<BufferListEntry> bufferPool = new LinkedList();
        private final Semaphore listLock = new Semaphore(1, true);
        private final AtomicInteger entryCount = new AtomicInteger(0);
        private final ConcurrentLinkedQueue<BufferListEntry> bufferPoolQ = new ConcurrentLinkedQueue();

        public BufferPool() {
            this.clear();
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public BufferListEntry getEntry(boolean trigger, int[] stripe) {
            BufferListEntry ble = null;
            try {
                this.listLock.acquire();
                try {
                    if (this.bufferPool.isEmpty()) {
                        ble = new BufferListEntry(trigger, stripe);
                    } else {
                        ble = this.bufferPool.remove();
                        ble.set(trigger, stripe);
                    }
                }
                finally {
                    this.listLock.release();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            return ble;
        }

        public void returnEntry(BufferListEntry ble) {
            try {
                if (ble != null) {
                    this.listLock.acquire();
                    this.bufferPool.add(ble);
                    this.listLock.release();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public BufferListEntry getEntryQ(boolean trigger, int[] stripe) {
            BufferListEntry ble = null;
            ble = this.bufferPoolQ.poll();
            if (ble == null) {
                ble = new BufferListEntry(trigger, stripe);
            } else {
                this.entryCount.decrementAndGet();
                ble.set(trigger, stripe);
            }
            return ble;
        }

        public void returnEntryQ(BufferListEntry ble) {
            if (ble != null) {
                this.bufferPoolQ.add(ble);
                this.entryCount.incrementAndGet();
            }
        }

        public void clear() {
            this.entryCount.set(0);
            this.bufferPoolQ.clear();
            this.bufferPool.clear();
        }

        public int getEntryCount() {
            return this.entryCount.get();
        }

        public boolean isEmpty() {
            if (this.bufferPoolQ.isEmpty()) {
                int count = this.getEntryCount();
                if (count != 0) {
                    System.out.println("Pool Count 0 missmatch " + count);
                }
                return true;
            }
            return false;
        }
    }

    private class BufferListEntry {
        public boolean stripeTrigger;
        public final int[] stripe;

        public BufferListEntry(boolean trigger, int[] stripe) {
            this.stripeTrigger = trigger;
            this.stripe = Arrays.copyOf(stripe, stripe.length);
        }

        public void set(boolean trigger, int[] stripe) {
            this.stripeTrigger = trigger;
            for (int i = 0; i < stripe.length; ++i) {
                this.stripe[i] = stripe[i];
            }
        }
    }

    private class StructuredStreamBufferWriter {
        private BufferManager bufferManager;
        private int numberOfGroups;
        private byte currentGroup;

        public StructuredStreamBufferWriter(BufferManager bufferManager, int numberOfGroups) {
            this.bufferManager = bufferManager;
            this.numberOfGroups = numberOfGroups;
            bufferManager.setBufferFormatter(new BufferFormatter());
        }

        public void startGroup(byte groupId) {
            this.currentGroup = groupId;
        }

        public void endGroup() {
        }

        public void writeData(byte[] value) {
        }

        public void writeData(int value) {
        }

        public void writeData(long value) {
        }

        public void writeData(boolean value) {
        }

        public void writeData(short value) {
        }

        public void writeData(byte value) {
        }

        class BufferFormatter
        implements IFBufferFormatter {
            BufferFormatter() {
            }

            @Override
            public void execute(BufferData bd) {
                bd.data.clear();
                bd.data.putShort((short)1);
                bd.data.putShort((short)2);
                bd.data.putInt(0);
            }
        }
    }

    private class BufferManager {
        BufferData workingBuffer;
        BufferData readBuffer;
        Queue<BufferData> fullBufferQ = new ConcurrentLinkedQueue<BufferData>();
        Queue<BufferData> freeBufferQ = new ConcurrentLinkedQueue<BufferData>();
        final int bufferSize;
        final int minimumFreeSpace;
        final boolean useDirect = true;
        final AtomicBoolean workingBufferLocked = new AtomicBoolean(false);
        IFBufferFormatter bufferFormatter;

        public BufferManager(int bufferSize, int minimumFreeSpace) {
            this.bufferSize = bufferSize;
            this.minimumFreeSpace = minimumFreeSpace;
            this.workingBuffer = this.newBuffer();
        }

        private void initialiseBuffer(BufferData bd) {
            bd.data.clear();
            if (this.bufferFormatter != null) {
                this.bufferFormatter.execute(bd);
            }
        }

        private BufferData newBuffer() {
            BufferData bd = new BufferData(this.bufferSize, this.minimumFreeSpace, true);
            this.initialiseBuffer(bd);
            return bd;
        }

        public void waitLockWorkingBuffer() {
            while (!this.workingBufferLocked.compareAndSet(false, true)) {
            }
        }

        public void freeLockWorkingBuffer() {
            this.workingBufferLocked.set(false);
        }

        private void newWorkingDataBuffer() {
            if (this.freeBufferQ.isEmpty()) {
                this.workingBuffer = this.newBuffer();
            } else {
                this.workingBuffer = this.freeBufferQ.poll();
                this.initialiseBuffer(this.workingBuffer);
            }
        }

        public BufferData getWorkingBuffer() {
            this.waitLockWorkingBuffer();
            try {
                if (this.workingBuffer == null || this.workingBuffer.isFull()) {
                    if (this.workingBuffer != null) {
                        this.fullBufferQ.add(this.workingBuffer);
                    }
                    this.newWorkingDataBuffer();
                }
            }
            finally {
                this.freeLockWorkingBuffer();
            }
            return this.workingBuffer;
        }

        public BufferData getFilledBuffer() {
            BufferData retVal = null;
            if (this.fullBufferQ.isEmpty()) {
                this.waitLockWorkingBuffer();
                try {
                    retVal = this.workingBuffer;
                    this.newWorkingDataBuffer();
                    retVal.data.rewind();
                }
                finally {
                    this.freeLockWorkingBuffer();
                }
            } else {
                retVal = this.fullBufferQ.poll();
                retVal.data.rewind();
            }
            return retVal;
        }

        public ByteBuffer getFilledBackingBuffer() {
            BufferData bd = this.getFilledBuffer();
            if (bd == null) {
                return null;
            }
            return bd.data;
        }

        public void setBufferFormatter(IFBufferFormatter bufferFormatter) {
            this.bufferFormatter = bufferFormatter;
        }
    }

    public static interface IFBufferFormatter {
        public void execute(BufferData var1);
    }

    private class BufferData {
        final ByteBuffer data;
        final int minimumFreeSpace;
        boolean readOnly = false;

        public BufferData(int bufferSize, int minimumFreeSpace, boolean directBuffer) {
            this.data = directBuffer ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
            this.minimumFreeSpace = minimumFreeSpace;
        }

        public boolean isFull() {
            return this.data.remaining() < this.minimumFreeSpace;
        }
    }
}

