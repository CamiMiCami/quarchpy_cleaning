/*
 * Decompiled with CFR 0.152.
 */
package appBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class AppVersion {
    private final String appVersion;
    public static final String appName = "Quarch Instrument Server";
    private static final String copyrightStr = "(C)Quarch Technology Ltd 2023";
    private static AppVersion instance;

    private AppVersion() {
        Properties properties = new Properties();
        try (InputStream inputStream = AppVersion.class.getResourceAsStream("/appBase/appversion.properties");){
            properties.load(inputStream);
            this.appVersion = properties.getProperty("app.version").trim();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized AppVersion getInstance() {
        if (instance == null) {
            instance = new AppVersion();
        }
        return instance;
    }

    public static String getAppVersion() {
        return AppVersion.getInstance().appVersion;
    }

    public static String getQualifiedAppVersion() {
        return "v" + AppVersion.getAppVersion();
    }

    public static String getAppnameAsFileName(String replaceSpaceWith) {
        return appName.replace(" ", replaceSpaceWith);
    }

    public static void getAppHeaderInfo(List<String> sl) {
        sl.add("Quarch Instrument Server " + AppVersion.getQualifiedAppVersion() + " " + copyrightStr);
        sl.add("");
        sl.add("This program uses:");
        sl.add("usb4java V1.3 distributed under LGPL v3.0");
        sl.add("Netty v4.1.43 distributed under Apache License 2.0");
    }
}

