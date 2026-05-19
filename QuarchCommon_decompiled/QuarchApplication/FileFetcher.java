/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleDoubleProperty
 */
package QuarchApplication;

import QuarchApplication.FileInformation;
import QuarchApplication.HttpFetchProgress;
import QuarchLogging.QuarchLoggerInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import javafx.beans.property.SimpleDoubleProperty;

public class FileFetcher
implements Runnable,
QuarchLoggerInterface {
    public SimpleDoubleProperty filesProgress;
    public SimpleDoubleProperty fileProgress;
    private final List<FileInformation> fileInfoList;
    private CountDownLatch countDone;
    private File[] fileLocation;
    private volatile boolean downloadOk = false;
    private Path tempPath;
    ScheduledExecutorService executor;
    Future fetcherStatus;

    private void setDefaults() {
        this.countDone = new CountDownLatch(1);
        this.filesProgress = new SimpleDoubleProperty();
        this.fileProgress = new SimpleDoubleProperty();
        this.fileProgress.set(0.0);
        this.filesProgress.set(0.0);
        this.executor = new ScheduledThreadPoolExecutor(2);
    }

    public FileFetcher(List<FileInformation> fileInfoList) {
        this.fileInfoList = fileInfoList;
        this.setDefaults();
    }

    public void waitDone() {
        try {
            this.countDone.await();
        }
        catch (InterruptedException e) {
            return;
        }
    }

    public boolean getDownloadOk() {
        return this.downloadOk;
    }

    public void start() {
        this.fetcherStatus = this.executor.submit(this);
    }

    public synchronized File[] getFileLocation() {
        return this.fileLocation;
    }

    private String calcDigest(File f) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(f);
            byte[] byteArray = new byte[4096];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            byte[] bdigest = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bdigest.length; ++i) {
                sb.append(Integer.toString((bdigest[i] & 0xFF) + 256, 16).substring(1));
            }
            return sb.toString();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        this.fileLocation = new File[this.fileInfoList.size()];
        try {
            this.tempPath = Files.createTempDirectory("QuarchInstall", new FileAttribute[0]);
            for (int i = 0; i < this.fileInfoList.size(); ++i) {
                String digest;
                this.fileInfoList.get(i).checkFileName();
                this.fileLocation[i] = new File(this.tempPath.toFile(), this.fileInfoList.get((int)i).FileName);
                HttpFetchProgress fetcher = new HttpFetchProgress(this.fileInfoList.get((int)i).URL, this.fileLocation[i]);
                Future<?> f = this.executor.submit(fetcher);
                try {
                    while (!f.isDone()) {
                        this.fileProgress.set(fetcher.progress.getValue().doubleValue());
                        Thread.sleep(25L);
                    }
                }
                catch (InterruptedException e) {
                    this.countDone.countDown();
                    return;
                }
                this.filesProgress.set((double)(i + 1) / (double)this.fileInfoList.size());
                String expChecksum = this.fileInfoList.get((int)i).Checksum;
                if (expChecksum != null && !(digest = this.calcDigest(this.fileLocation[i])).contentEquals(expChecksum)) {
                    this.logToDefault(Level.WARNING, String.format("Received checksum %s on file %s does not match expected checksum %s", digest, this.fileLocation[i].getAbsolutePath(), expChecksum));
                    return;
                }
                this.downloadOk = true;
            }
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("fetchAndRunInstaller", Level.SEVERE, "Unable to create temporary installer file");
        }
        finally {
            this.countDone.countDown();
        }
    }

    public void executeInstaller() {
        try {
            Runtime run = Runtime.getRuntime();
            Process process = run.exec(this.fileLocation[0].getAbsolutePath());
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("fetchAndRunInstaller", Level.SEVERE, "Unable to execute installer");
        }
    }

    public void cleanup() {
        for (File f : this.fileLocation) {
            if (f.exists()) {
                f.delete();
            }
            if (!this.tempPath.toFile().exists() || !this.tempPath.toFile().isDirectory()) continue;
            this.tempPath.toFile().delete();
        }
    }
}

