/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  Quarch.CIGlobals
 *  Quarch.QChangeListener
 *  QuarchLogging.QuarchLogger
 *  QuarchLogging.QuarchLoggerInterface
 *  Scan.DeviceScan
 *  device.DeviceRefCollection
 */
package qis;

import Quarch.CIGlobals;
import Quarch.QChangeListener;
import QuarchLogging.QuarchLogger;
import QuarchLogging.QuarchLoggerInterface;
import Scan.DeviceScan;
import appBase.AppVersion;
import appBase.IconsSwing;
import commandProcessor.CmdProcessorSingleton;
import commsDeviceInterface.CommsScanListListener;
import commsDeviceInterface.commsDeviceListListener;
import device.DeviceRefCollection;
import frontEnd.Echo.EchoServer;
import frontEnd.Rest.RESTServer;
import frontEnd.Telnet.TelnetServer;
import frontEnd.internal.TerminalGlue;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import properties.AppProperties;
import qis.Globals;
import qis.SystrayActions;
import qis.SystrayIcon;
import qis.SystrayListener;
import src.com.quarch.beWrapper.QbeInterfaceWrapper;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.utils.DebugUtil;

public class qisMain
implements QuarchLoggerInterface {
    private static QuarchLogger quarchLogger;
    static TerminalGlue terminal;
    static EchoServer echoServer;
    static RESTServer restServer;
    static TelnetServer telnetServer;
    static boolean isHeadless;
    static boolean restServerStarted;
    static boolean telnetServerStarted;
    static SystrayIcon systrayIcon;
    static boolean showTerminal;
    static boolean terminalPermitted;
    static List<String> statusMessages;
    private static SystrayResponder systrayResponder;
    static AppProperties appProp;
    static DeviceList deviceList;
    private static DeviceRefCollection deviceCollection;
    private static DeviceScan devScan;

    private static void initLogging() {
        QuarchLogger.initQuarchLogger((boolean)true, (String)(AppProperties.getApplicationLOGFilesPath() + File.separator), (String)"QIS", (int)0x1400000, (int)5);
    }

    public static void main(String[] args) throws Exception {
        Console console = System.console();
        appProp = AppProperties.getInstance();
        qisMain.initLogging();
        Globals.applicationIcons = new IconsSwing();
        File f = new File(AppProperties.getApplicationPath() + File.separator + "disableterminal.txt");
        if (!f.exists() || f.canWrite()) {
            terminalPermitted = true;
        }
        qisMain.processCLIArgumants(args);
        qisMain.startLogging();
        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run() {
                AppProperties.getInstance().saveAppProperties();
            }
        });
        isHeadless = GraphicsEnvironment.isHeadless();
        if (isHeadless) {
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" GraphicsEnvironment: Headless");
        } else {
            GraphicsEnvironment localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)(" Graphics Environment: " + localGraphicsEnvironment.getClass().getName()));
        }
        systrayIcon = new SystrayIcon();
        if (qisMain.systrayIcon.isSupported) {
            systrayIcon.addSystrayListener(systrayResponder);
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Tray Icon: Enabled");
        } else {
            systrayIcon = null;
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Tray Icon: Not Available");
        }
        qisMain.createGlueLayer();
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Glue Layer: Created");
        boolean forceExit = false;
        int expectedInterfaces = 2;
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Front End: Starting");
        qisMain.startFrontEndInterfaces();
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Front End: Started");
        forceExit = qisMain.checkFrontEndInterfaces(2, forceExit);
        DebugUtil.debugMsgln("Headless Check");
        if (!isHeadless) {
            terminal = qisMain.openATerminal(showTerminal);
        }
        DebugUtil.debugMsgln("Status Check");
        if (!statusMessages.isEmpty()) {
            if (terminal != null && showTerminal) {
                for (String s : statusMessages) {
                    qisMain.toConsole(terminal, s);
                }
                qisMain.toConsole(terminal, ">");
                if (forceExit) {
                    Thread.sleep(3000L);
                    System.exit(3);
                }
            } else {
                if (isHeadless) {
                    System.out.println("");
                    for (String s : statusMessages) {
                        System.out.println(s);
                    }
                    Thread.sleep(3000L);
                } else {
                    String dlgStr = "";
                    for (String s : statusMessages) {
                        dlgStr = dlgStr + s + "\r\n";
                    }
                    JOptionPane.showMessageDialog(TerminalGlue.terminalFacade, dlgStr, "QIS - Error", 0);
                }
                if (forceExit) {
                    System.exit(3);
                }
            }
        }
        DebugUtil.debugMsgln("Starting Back End");
        qisMain.startBackEndInterfaces();
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Startup Command List: Starting");
        CmdProcessorSingleton.getInstance().executeCommandList(AppProperties.getInstance().defaultCommands);
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Startup Command List: Done");
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Startup Complete");
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Initialisation Complete");
        CmdProcessorSingleton.initialisationComplete = true;
        while (!CmdProcessorSingleton.shutdown) {
            Thread.sleep(250L);
        }
        if (systrayIcon != null) {
            systrayIcon.removeSysTrayIcon();
        }
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)"Stopping");
        System.exit(0);
    }

    private static boolean checkFrontEndInterfaces(int expectedInterfaces, boolean forceExit) throws InterruptedException {
        int interfaceCount = expectedInterfaces;
        while (!TelnetServer.initComplete || !RESTServer.initComplete) {
            Thread.sleep(250L);
        }
        DebugUtil.debugMsgln("Front End servers initialised");
        if (!RESTServer.running) {
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Could Not Claim ReST Port");
            StringBuilder stringBuilder = new StringBuilder().append("Unable to access required port\n\nAnother instance of QIS/QPS may be running, close this first.\nFirewall permissions may be blocking the required ports.\n\nDetails:\nCould Not Claim REST Port: ");
            AppProperties.getInstance();
            qisMain.addStatusMessage(stringBuilder.append(Integer.toString(AppProperties.appPropertiesData.getLocalPorts().getRestInt())).append("\n").toString());
            --interfaceCount;
        }
        if (!TelnetServer.running) {
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Could Not Claim Telnet Port");
            StringBuilder stringBuilder = new StringBuilder().append("Could Not Claim Telnet Port: ");
            AppProperties.getInstance();
            qisMain.addStatusMessage(stringBuilder.append(AppProperties.appPropertiesData.getLocalPorts().getTelnetInt()).append("\n").toString());
            --interfaceCount;
        }
        if (interfaceCount <= 0) {
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" No Client Interfaces Available, Exiting");
            qisMain.addStatusMessage("Shutting Down");
            forceExit = true;
        }
        return forceExit;
    }

    private static void startLogging() {
        QuarchLogger.startLogger();
        QuarchLogger.setEnableExceptionToConsole((boolean)DebugUtil.isEnableDevDebug());
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)(" " + AppVersion.getQualifiedAppVersion() + " Starting"));
    }

    private static void processCLIArgumants(String[] args) {
        String logFileName = null;
        if (args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                String[] ipString;
                Pattern PATTERN;
                String argStr = args[i];
                boolean argAccepted = false;
                if (argStr.equalsIgnoreCase("-terminal")) {
                    showTerminal = true;
                    argAccepted = true;
                }
                if (argStr.toLowerCase().startsWith("-logging=") && QuarchLogger.setLoggerLevel((String)argStr.substring("-logging=".length()).trim())) {
                    argAccepted = true;
                }
                if (argStr.toLowerCase().startsWith("-logtarget=")) {
                    logFileName = argStr.substring("-logtarget=".length());
                    QuarchLogger.setLogTarget((boolean)true, (String)logFileName);
                    argAccepted = true;
                }
                if (argStr.toLowerCase().startsWith("-logviewer=")) {
                    if (QuarchLogger.setLoggerLevel((String)"ON")) {
                        argAccepted = true;
                    }
                    qisMain.launchLogViewer(argStr);
                }
                if (argStr.equalsIgnoreCase("-devdebug")) {
                    DebugUtil.setEnableDevDebug(true);
                    argAccepted = true;
                }
                if (argStr.equalsIgnoreCase("-devdebug2")) {
                    DebugUtil.setEnableDevDebug(true);
                    DebugUtil.setEnableCostlyDevDebug(true);
                    argAccepted = true;
                }
                if (argStr.startsWith("-bind=") && (PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")).matcher((CharSequence)(ipString = argStr.substring("-bind=".length()))).matches()) {
                    AppProperties.scanBindIpAddress = ipString;
                    argAccepted = true;
                }
                if (argAccepted) continue;
                StringBuilder sb = new StringBuilder();
                ipString = args;
                int n = ipString.length;
                for (int j = 0; j < n; ++j) {
                    String s = ipString[j];
                    sb.append(s + " ");
                }
                String argFileStr = "Fail: Invalid Argument <" + argStr + "> in <" + sb.toString() + ">";
                System.out.println(argFileStr);
                System.exit(-1);
            }
        }
    }

    public static boolean isJavaAppRunning(String appName) {
        String processName = "java";
        boolean isRunning = false;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("jps");
            Process process = processBuilder.start();
            Scanner scanner = new Scanner(process.getInputStream());
            ArrayList<String> processList = new ArrayList<String>();
            while (scanner.hasNextLine()) {
                processList.add(scanner.nextLine());
            }
            scanner.close();
            isRunning = processList.stream().anyMatch(line -> line.contains(appName));
        }
        catch (Exception e) {
            QuarchLogger.logMessage((Level)Level.WARNING, (String)e.toString());
        }
        return isRunning;
    }

    public static void launchLogViewer(String argStr) {
        Thread logViewerThread = new Thread(() -> {
            try {
                ProcessBuilder processBuilder;
                String appNameToCheck = "olv-exec.jar";
                boolean isRunning = qisMain.isJavaAppRunning(appNameToCheck);
                while (!isRunning) {
                    isRunning = qisMain.isJavaAppRunning(appNameToCheck);
                }
                Thread.sleep(2000L);
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) {
                    String batFilePath = argStr.substring("-logviewer=".length()).trim() + File.separator + "olv.bat";
                    processBuilder = new ProcessBuilder(batFilePath, QuarchLogger.getLogFilePath() + "/QIS-0.log");
                } else {
                    String shFilePath = argStr.substring("-logviewer=".length()).trim() + File.separator + "olv.sh";
                    processBuilder = new ProcessBuilder("perl", "-p", "-i", "-e", "s/\\r\\n/\\n/", shFilePath);
                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        QuarchLoggerInterface.logToDefault((String)"qisMain", (String)"qisMain.launchLogViewer", (Level)Level.INFO, (String)"Perl command executed successfully");
                    } else {
                        QuarchLoggerInterface.logToDefault((String)"qisMain", (String)"qisMain.launchLogViewer", (Level)Level.WARNING, (String)("Perl command execution failed with exit code: " + exitCode));
                    }
                }
                processBuilder.directory(new File(argStr.substring("-logviewer=".length()).trim()));
                Process process = processBuilder.start();
            }
            catch (IOException | InterruptedException e) {
                QuarchLogger.logMessage((Level)Level.WARNING, (String)e.toString());
            }
        });
        logViewerThread.start();
    }

    private static void displayAbout() {
        ArrayList<String> aboutText = new ArrayList<String>();
        AppVersion.getAppHeaderInfo(aboutText);
        aboutText.add("");
        if (!statusMessages.isEmpty()) {
            aboutText.addAll(statusMessages);
        }
        String aboutStr = "";
        for (String s : aboutText) {
            aboutStr = aboutStr + s + "\r\n";
        }
        systrayIcon.setAboutShowing(true);
        JOptionPane.showMessageDialog(TerminalGlue.terminalFacade, aboutStr, "About", 1);
        systrayIcon.setAboutShowing(false);
    }

    private static void addStatusMessage(String s) {
        statusMessages.add(s);
    }

    public static TerminalGlue openATerminal(boolean showTerminal) {
        TerminalGlue newTerminal = null;
        if (terminalPermitted) {
            final TerminalGlue finTerminal = newTerminal = new TerminalGlue(showTerminal);
            TerminalGlue.terminalFacade.addWindowListener(new WindowAdapter(){

                @Override
                public void windowClosing(WindowEvent arg0) {
                    finTerminal.close();
                    if (systrayIcon != null) {
                        systrayIcon.setNewTerminalEnabled(true);
                    }
                }
            });
        }
        if (systrayIcon != null) {
            systrayIcon.setNewTerminalEnabled(!showTerminal);
        }
        return newTerminal;
    }

    static void toConsole(TerminalGlue terminal, String s) {
        if (terminal == null) {
            terminal = new TerminalGlue(true);
        }
        if (terminal != null) {
            terminal.directWrite(s);
        }
    }

    static void createGlueLayer() {
        DebugUtil.debugMsgln("Creating Glue");
        deviceList = new DeviceList();
        CmdProcessorSingleton.getInstance().setDeviceList(deviceList);
    }

    static void startBackEndInterfaces() {
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Back End Interfaces: Starting");
        deviceCollection = new DeviceRefCollection(null);
        DeviceScan.scan_serial = false;
        devScan = DeviceScan.initialize((String)"Comms");
        commsDeviceListListener my_listener = new commsDeviceListListener(deviceList);
        deviceCollection.addListener((QChangeListener)my_listener);
        CommsScanListListener my_InterfaceListener = new CommsScanListListener(deviceList);
        devScan.addListener((QChangeListener)my_InterfaceListener);
        ArrayBlockingQueue my_queue = new ArrayBlockingQueue(1);
        devScan.start(deviceCollection);
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Startup Device Scan: Started");
        QbeInterfaceWrapper.getInstance().setDeviceList(deviceList);
        CIGlobals.semaphoreTimeout = AppProperties.getInstance().getDefaultTCPSemaphoreTimeout();
        CIGlobals.defaultTCPTimeout = AppProperties.getInstance().getDefaultTCPTimeout();
        QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)" Back End Interfaces: Started");
    }

    public static void scanDevices() {
        devScan.refreshAll();
    }

    static void startFrontEndInterfaces() {
        DebugUtil.debugMsgln("Creating Front End");
        restServer = new RESTServer(deviceList);
        Thread errorCatch = new Thread(restServer);
        errorCatch.setName("ErrorCatch RESTServer");
        errorCatch.start();
        DebugUtil.debugMsgln("Front End Rest");
        telnetServer = new TelnetServer(deviceList);
        errorCatch = new Thread(telnetServer);
        errorCatch.setName("ErrorCatch TelnetServer");
        errorCatch.start();
        DebugUtil.debugMsgln("Front End Telnet");
    }

    static {
        showTerminal = false;
        terminalPermitted = false;
        statusMessages = new ArrayList<String>();
        systrayResponder = new SystrayResponder();
    }

    public static class SystrayResponder
    implements SystrayListener {
        @Override
        public void systrayEventListener(SystrayActions action) {
            QuarchLoggerInterface.logToDefault((String)"QISMain", (Level)Level.INFO, (String)("Action: " + (Object)((Object)action)));
            switch (action) {
                case About: {
                    qisMain.displayAbout();
                    break;
                }
                case Exit: {
                    Object[] options = new Object[]{"Ok", "Cancel"};
                    int n = JOptionPane.showOptionDialog(TerminalGlue.terminalFacade, "Close Quarch Instrument Server?", "Information", 0, 3, null, options, options[0]);
                    if (n != 0) break;
                    CmdProcessorSingleton.setShutdown();
                    break;
                }
                case NewTerminal: {
                    qisMain.openATerminal(true);
                    break;
                }
                case None: {
                    break;
                }
            }
        }
    }
}

