/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleDoubleProperty
 */
package QuarchApplication;

import QuarchLogging.QuarchLoggerInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import javafx.beans.property.SimpleDoubleProperty;

public class HttpFetchProgress
implements Runnable,
QuarchLoggerInterface {
    private final String url;
    private final File f;
    public SimpleDoubleProperty progress;

    public HttpFetchProgress(String url, File f) {
        this.url = url;
        this.f = f;
        this.progress = new SimpleDoubleProperty();
        this.progress.set(0.0);
    }

    @Override
    public void run() {
        URL obj = null;
        byte[] buffer = new byte[4096];
        try {
            int bytesFetched = 0;
            obj = new URL(this.url);
            long size = 0L;
            HttpURLConnection conn = (HttpURLConnection)obj.openConnection();
            FileOutputStream outFile = new FileOutputStream(this.f.getAbsoluteFile());
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream netStream = conn.getInputStream();
                size = conn.getContentLengthLong();
                int bytesRead = -1;
                while ((bytesRead = netStream.read(buffer)) != -1) {
                    outFile.write(buffer, 0, bytesRead);
                    this.progress.set((double)(bytesFetched += bytesRead) / (double)size);
                }
                outFile.close();
                netStream.close();
            }
        }
        catch (MalformedURLException e) {
            this.logToDefault(Level.SEVERE, "Incorrect URL " + this.url);
        }
        catch (IOException e) {
            this.logToDefault(Level.WARNING, "Unable to connect to URL " + this.url);
        }
    }
}

