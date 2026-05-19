/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.devices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketCapture {
    final File captureFile;
    FileOutputStream captureFileOutStream = null;
    boolean userDataWritten = false;
    private final String pathName;
    private long startTime_NS;
    private long prevCaptureTime_NS;
    private boolean firstCapture = true;
    ByteBuffer int32ToByte = ByteBuffer.allocate(4);
    ByteBuffer longToByte = ByteBuffer.allocate(8);

    public PacketCapture(String pathName) throws IOException {
        this.pathName = pathName;
        this.captureFile = new File(pathName);
        this.captureFile.createNewFile();
        this.open();
    }

    private void open() throws IOException {
        this.captureFileOutStream = new FileOutputStream(this.captureFile);
        this.captureFileOutStream.write(0);
        this.captureFileOutStream.write(2);
    }

    public void close() throws IOException {
        this.firstCapture = true;
        if (this.captureFileOutStream != null) {
            this.captureFileOutStream.close();
        }
        if (!this.userDataWritten && this.captureFile.exists()) {
            this.captureFile.delete();
        }
    }

    public void captureData(byte[] data) {
        try {
            if (this.firstCapture) {
                this.startTime_NS = System.nanoTime();
                this.prevCaptureTime_NS = System.nanoTime();
                this.firstCapture = false;
            }
            long nsTime = System.nanoTime();
            long sampleTime = nsTime - this.prevCaptureTime_NS;
            this.prevCaptureTime_NS = nsTime;
            this.userDataWritten = true;
            this.longToByte.rewind();
            this.longToByte.putLong(sampleTime);
            this.longToByte.flip();
            this.captureFileOutStream.write(this.longToByte.array());
            this.int32ToByte.rewind();
            this.int32ToByte.putInt(data.length);
            this.int32ToByte.flip();
            this.captureFileOutStream.write(this.int32ToByte.array());
            this.captureFileOutStream.write(data);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPathName() {
        return this.pathName;
    }

    public void reopen() {
        if (!this.userDataWritten) {
            return;
        }
        this.userDataWritten = false;
        try {
            this.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.captureFile.createNewFile();
            this.open();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

