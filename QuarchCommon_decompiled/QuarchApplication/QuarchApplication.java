/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.JAXBContext
 *  jakarta.xml.bind.JAXBException
 *  jakarta.xml.bind.Unmarshaller
 */
package QuarchApplication;

import QuarchApplication.QuarchUpdateInfo;
import QuarchApplication.SingleHttpFetch;
import QuarchApplication.VersionNumber;
import QuarchLogging.QuarchLogger;
import QuarchLogging.QuarchLoggerInterface;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.logging.Level;

public interface QuarchApplication {
    public static void initQuarchApp(String appName, int logSize, int numLogs) {
        QuarchLogger.initQuarchLogger(QuarchApplication.getQuarchAppDataDirectory(appName).toString(), appName, logSize, numLogs);
    }

    public static Path getQuarchAppDataDirectory(String appName) {
        Path appdir = System.getProperty("os.name").contains("Windows") ? Paths.get(System.getProperty("user.home"), "AppData", "Local", "Quarch", appName) : Paths.get(System.getProperty("user.home"), ".quarch", appName);
        File appdirFile = appdir.toFile();
        if (!appdirFile.isDirectory()) {
            appdirFile.mkdirs();
        }
        return appdir;
    }

    public static String checkIsLatestVersion(String appVersion, String url) {
        try {
            String textWebVersion = SingleHttpFetch.fetch(url);
            VersionNumber webVersion = new VersionNumber(textWebVersion);
            VersionNumber curVersion = new VersionNumber(appVersion);
            if (curVersion.compareTo(webVersion) == -1) {
                return textWebVersion;
            }
            return "latest";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "latest";
        }
    }

    public static QuarchUpdateInfo getUpdateInfo(String url) {
        QuarchUpdateInfo rv = null;
        String updateString = SingleHttpFetch.fetch(url);
        if (updateString != null) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance((Class[])new Class[]{QuarchUpdateInfo.class});
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                StringReader reader = new StringReader(updateString);
                rv = (QuarchUpdateInfo)jaxbUnmarshaller.unmarshal((Reader)reader);
            }
            catch (JAXBException e) {
                rv = new QuarchUpdateInfo();
                QuarchLoggerInterface.logToDefault("getUpdateInfo", Level.WARNING, e.toString());
            }
        }
        return rv;
    }

    public static void fetchAndRunInstaller(String[] installerUrl) {
        File[] localInstaller = new File[installerUrl.length];
        String[] args = new String[localInstaller.length];
        try {
            Path tempPath = Files.createTempDirectory("QuarchInstall", new FileAttribute[0]);
            for (int i = 0; i < installerUrl.length; ++i) {
                String[] pathSplit = installerUrl[i].split("/");
                String fileName = pathSplit[pathSplit.length - 1];
                localInstaller[i] = new File(tempPath.toFile(), fileName);
                pathSplit[pathSplit.length - 1] = fileName.replaceAll(" ", "%20");
                String encodedUrl = String.join((CharSequence)"/", Arrays.asList(pathSplit));
                if (!SingleHttpFetch.fetchFile(encodedUrl, localInstaller[i])) {
                    return;
                }
                args[i] = localInstaller[i].getAbsolutePath();
            }
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("fetchAndRunInstaller", Level.SEVERE, "Unable to create temporary installer file");
            return;
        }
        Runtime run = Runtime.getRuntime();
        try {
            Process i = run.exec(args[0]);
        }
        catch (IOException e) {
            QuarchLoggerInterface.logToDefault("fetchAndRunInstaller", Level.SEVERE, "Unable execute installer " + localInstaller[0].getAbsolutePath());
        }
    }
}

