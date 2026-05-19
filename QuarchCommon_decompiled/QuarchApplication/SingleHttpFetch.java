/*
 * Decompiled with CFR 0.152.
 */
package QuarchApplication;

import QuarchLogging.QuarchLoggerInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

public class SingleHttpFetch
implements QuarchLoggerInterface {
    public static String fetch(String url) {
        String rv = null;
        URL obj = null;
        try {
            obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)obj.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String inputLine;
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                }
                in.close();
                rv = sb.toString();
            }
        }
        catch (MalformedURLException e) {
            QuarchLoggerInterface.logToDefault("SingleHttpFetch", Level.SEVERE, "Incorrect URL " + url);
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("SingleHttpFetch", Level.WARNING, "Unable to connect to URL " + url);
        }
        return rv;
    }

    public static boolean fetchFile(String url, File f) {
        URL obj = null;
        byte[] buffer = new byte[4096];
        try {
            obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)obj.openConnection();
            FileOutputStream outFile = new FileOutputStream(f.getAbsoluteFile());
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream netStream = conn.getInputStream();
                int bytesRead = -1;
                while ((bytesRead = netStream.read(buffer)) != -1) {
                    outFile.write(buffer, 0, bytesRead);
                }
                outFile.close();
                netStream.close();
            }
            return true;
        }
        catch (MalformedURLException e) {
            QuarchLoggerInterface.logToDefault("SingleHttpFetch", Level.SEVERE, "Incorrect URL " + url);
            return false;
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("SingleHttpFetch", Level.WARNING, "Unable to connect to URL " + url);
            return false;
        }
    }
}

