/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.JAXBContext
 *  jakarta.xml.bind.JAXBException
 *  jakarta.xml.bind.Marshaller
 *  jakarta.xml.bind.Unmarshaller
 */
package properties;

import appBase.AppVersion;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import properties.AppPropertiesData;
import src.com.quarch.utils.DebugUtil;

public final class AppProperties {
    private static volatile AppProperties instance;
    static final String propsFileName;
    static final String deviceChar = "*";
    public static volatile AppPropertiesData appPropertiesData;
    public List<String> defaultCommands;
    private static String applicationPath;
    private static String userRWDirectoryPath;
    private static String applicationRWFilesPath;
    private static String applicationLOGFilesPath;
    private static String applicationDir;
    public static String scanBindIpAddress;

    private AppProperties() {
        this.setup();
        this.loadAppProperties();
        this.loadStartupCommandFile();
    }

    private void setup() {
        appPropertiesData = new AppPropertiesData();
        AppProperties.setDirectoriesAndFiles(true);
    }

    private static void setDirectoriesAndFiles(boolean movePropertyFile) {
        boolean weHaveaRWProblem = false;
        applicationPath = Paths.get(".", new String[0]).toAbsolutePath().normalize().toString();
        userRWDirectoryPath = System.getProperty("user.home");
        try {
            File testItDir = new File(userRWDirectoryPath);
            if (!testItDir.exists()) {
                weHaveaRWProblem = true;
            } else {
                applicationRWFilesPath = System.getProperty("os.name").contains("Windows") ? Paths.get(userRWDirectoryPath, "AppData", "Local", "Quarch", AppVersion.getAppnameAsFileName("_"), AppVersion.getAppVersion()).toString() : Paths.get(userRWDirectoryPath, ".quarch", AppVersion.getAppnameAsFileName("_"), AppVersion.getAppVersion()).toString();
                testItDir = new File(applicationRWFilesPath);
                if (!testItDir.exists() && !testItDir.mkdirs()) {
                    weHaveaRWProblem = true;
                }
            }
        }
        catch (Exception e) {
            weHaveaRWProblem = true;
        }
        if (weHaveaRWProblem) {
            applicationRWFilesPath = applicationPath;
        }
        if (movePropertyFile) {
            try {
                AppProperties.movePropertyFiles();
            }
            catch (Exception e) {
                applicationRWFilesPath = applicationPath;
            }
        }
        if (applicationLOGFilesPath == null) {
            applicationLOGFilesPath = applicationRWFilesPath;
        }
    }

    private static void movePropertyFiles() throws Exception {
        if (applicationRWFilesPath == applicationPath) {
            return;
        }
        String dstFile = AppProperties.propertyXMLFileFullPath();
        String srcFile = AppProperties.propertyXMLFileFullPathFor(applicationPath);
        File testSrc = new File(srcFile);
        File testDst = new File(dstFile);
        if (testSrc.exists() && !testDst.exists()) {
            Path srcPath = Paths.get(srcFile, "");
            Path dstPath = Paths.get(dstFile, "");
            Files.copy(srcPath, dstPath, new CopyOption[0]);
        }
    }

    public static String getApplicationLOGFilesPath() {
        return applicationLOGFilesPath;
    }

    private static String propertyXMLFileFullPathFor(String path) {
        return path + File.separator + propsFileName + ".properties.xml";
    }

    private static String propertyXMLFileFullPath() {
        String s = AppProperties.propertyXMLFileFullPathFor(applicationRWFilesPath);
        return s;
    }

    private void loadStartupCommandFile() {
        try {
            File inFile = new File(AppProperties.getApplicationPath() + File.separator + "startupCommands.txt");
            if (inFile.exists()) {
                this.defaultCommands = new ArrayList<String>();
                Scanner s = new Scanner(inFile);
                while (s.hasNextLine()) {
                    this.defaultCommands.add(s.nextLine());
                }
                s.close();
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static String getApplicationPath() {
        return applicationPath;
    }

    public static String getApplicationRWFilesPath() {
        return applicationRWFilesPath;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static AppProperties getInstance() {
        if (instance != null) return instance;
        Class<AppProperties> clazz = AppProperties.class;
        synchronized (AppProperties.class) {
            if (instance != null) return instance;
            instance = new AppProperties();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    public void setMyApplicationPath() {
        applicationDir = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (applicationDir.endsWith(".jar")) {
            applicationDir = new File(applicationDir).getParent();
        }
    }

    private void debugPrint() {
        try {
            DebugUtil.debugMsgln("lastModified " + AppProperties.appPropertiesData.lastModified);
            DebugUtil.debugMsgln("getTelnet()  " + appPropertiesData.getLocalPorts().getTelnet());
            DebugUtil.debugMsgln("getRest()  " + appPropertiesData.getLocalPorts().getRest());
            DebugUtil.debugMsgln("getTcp() " + appPropertiesData.getLocalPorts().getTcp());
        }
        catch (Exception e) {
            StackTraceElement[] st = e.getStackTrace();
            DebugUtil.debugMsgln("appPropertiesData Exception:");
            for (StackTraceElement ste : st) {
                DebugUtil.debugMsgln("\t" + ste.toString());
            }
        }
    }

    public void loadFromXMLPropFile() {
        try {
            DebugUtil.debugMsgln("Load Properties:" + AppProperties.propertyXMLFileFullPath());
            File file = new File(AppProperties.propertyXMLFileFullPath());
            if (file.exists()) {
                JAXBContext jaxbContext = JAXBContext.newInstance((Class[])new Class[]{AppPropertiesData.class});
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                appPropertiesData = (AppPropertiesData)jaxbUnmarshaller.unmarshal(file);
            } else {
                DebugUtil.debugMsgln("File [" + file.getAbsolutePath() + "] not found");
            }
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void saveToXMLPropFile() {
        try {
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            AppProperties.appPropertiesData.lastModified = sdf.format(date);
            JAXBContext jaxbContext = JAXBContext.newInstance((Class[])new Class[]{AppPropertiesData.class});
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty("jaxb.formatted.output", (Object)true);
            jaxbMarshaller.marshal((Object)appPropertiesData, new File(AppProperties.propertyXMLFileFullPath()));
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private void loadAppProperties() {
        this.loadFromXMLPropFile();
    }

    private String getDeviceId(String str, int offset) {
        return str.substring(offset);
    }

    public void saveAppProperties() {
        this.saveToXMLPropFile();
    }

    public int getDefaultTCPTimeout() {
        return appPropertiesData.getDefaultTCPTimeout();
    }

    public int getDefaultTCPSemaphoreTimeout() {
        return appPropertiesData.getDefaultTCPSemaphoreTimeout();
    }

    static {
        propsFileName = AppVersion.getAppnameAsFileName("");
        applicationPath = null;
        userRWDirectoryPath = null;
        applicationRWFilesPath = null;
        applicationLOGFilesPath = null;
        scanBindIpAddress = "";
    }
}

