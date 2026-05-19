/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  QuarchLogging.QuarchLoggerInterface
 *  Scan.DeviceScan
 *  commsInterface.DeviceInterfaceFeatures
 *  commsInterfaceEthernet.PooledSocket
 *  commsInterfaceEthernet.SocketPool
 */
package commandProcessor;

import QuarchLogging.QuarchLoggerInterface;
import Scan.DeviceScan;
import appBase.AppVersion;
import commandProcessor.CmdHelp;
import commandProcessor.MemoryInfo;
import commsInterface.DeviceInterfaceFeatures;
import commsInterfaceEthernet.PooledSocket;
import commsInterfaceEthernet.SocketPool;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import properties.AppProperties;
import properties.EthernetPorts;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beStream.StreamBase;
import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.beWrapper.QbeInterfaceWrapper;
import src.com.quarch.deviceInterface.DeviceList;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceVirtual.VirtualDeiceList;
import src.com.quarch.deviceVirtual.VirtualDevice;
import src.com.quarch.parser.DeviceParser;
import src.com.quarch.parser.Token;
import src.com.quarch.parser.Tokens;
import src.com.quarch.utils.DebugUtil;

public final class CmdProcessorSingleton
implements QuarchLoggerInterface {
    private static final String FAIL_BAD_COMMAND = "FAIL: Bad command";
    private static final String FAIL_INSUFFICIENT_PARAMETERS = "FAIL: Insufficient parameters";
    private static final String FAIL_INVALID_DESCRIPTION = "FAIL: Invalid description";
    private static final String $CREATED_DEVICES_Q = "$created devices?";
    private static final String $CREATED_DEVICES_DELETE = "$created devices delete";
    private static final String $CREATED_DEVICES_CLEAR = "$created devices clear";
    private static final String $CREATE_DEVICE = "$create device";
    private static final String STR_OK = "OK";
    private static final Level defaultCmdLogLevel = Level.INFO;
    private static volatile CmdProcessorSingleton instance;
    public static boolean initialisationComplete;
    public static boolean shutdown;
    private CmdHelp cmdHelp = new CmdHelp();
    private static DeviceList deviceList;
    ByteBuffer bbTest = ByteBuffer.allocateDirect(65536);
    ByteBuffer bbTest2;
    byte[] testArray;
    boolean hasArray;
    private final VirtualDeiceList virtualDeiceList = new VirtualDeiceList();
    private List<String> cmdListResults = new ArrayList<String>();

    private CmdProcessorSingleton() {
    }

    public DeviceList getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(DeviceList deviceList) {
        CmdProcessorSingleton.deviceList = deviceList;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static CmdProcessorSingleton getInstance() {
        if (instance != null) return instance;
        Class<CmdProcessorSingleton> clazz = CmdProcessorSingleton.class;
        synchronized (CmdProcessorSingleton.class) {
            if (instance != null) return instance;
            instance = new CmdProcessorSingleton();
            // ** MonitorExit[var0] (shouldn't be in output)
            return instance;
        }
    }

    public static void setShutdown() {
        shutdown = true;
    }

    public boolean cmdDecode(CmdStruct cmd) {
        if (!initialisationComplete) {
            String command = cmd.command.trim();
            this.logToDefault(defaultCmdLogLevel, "Issuing Command: " + command);
            cmd.response.add("Fail: Startup in Progress\r\n");
            cmd.response.add(">");
            cmd.action = 333;
            return true;
        }
        return this.internalCmdDecode(cmd);
    }

    public boolean internalCmdDecode(CmdStruct cmd) {
        String command = cmd.command.trim();
        this.logToDefault(defaultCmdLogLevel, "Issuing Command: " + command);
        cmd.action = 0;
        cmd.setDebugFilterLongReply(false);
        if (command.isEmpty()) {
            cmd.response.add(">");
            cmd.action = 333;
            return true;
        }
        String lowerCaseCmd = command.toLowerCase();
        if ("$help".equals(lowerCaseCmd)) {
            for (String s : this.cmdHelp.helpStrings) {
                cmd.response.add(s);
            }
            cmd.action = 333;
        } else if ("$sysinfo".equals(lowerCaseCmd)) {
            this.getSysInfo(cmd);
            cmd.action = 333;
        } else if ("$shutdown".equals(lowerCaseCmd)) {
            cmd.response.add("Have a good day!\r\n");
            cmd.response.add(">");
            cmd.action = -1;
            CmdProcessorSingleton.setShutdown();
        } else if (lowerCaseCmd.startsWith("$list")) {
            this.listDevices(cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith("$version")) {
            cmd.response.add(AppVersion.getQualifiedAppVersion());
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith("$sleep")) {
            this.doSleepCmd(cmd, lowerCaseCmd);
        } else if (lowerCaseCmd.startsWith("$sockets?")) {
            this.doSocketsCmd(cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith("$debug")) {
            this.doDebugCmd(cmd, lowerCaseCmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith("$scan")) {
            this.doScan(command, cmd);
        } else if (lowerCaseCmd.startsWith($CREATE_DEVICE)) {
            this.doCreateDevice(command, cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith($CREATED_DEVICES_Q)) {
            this.doCreatedDevicesQuery(command, cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith($CREATED_DEVICES_DELETE)) {
            this.doCreatedDevicesDelete(command, cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (lowerCaseCmd.startsWith($CREATED_DEVICES_CLEAR)) {
            this.doCreatedDevicesClear(command, cmd);
            cmd.response.add(">");
            cmd.action = 333;
        } else if (QbeInterfaceWrapper.getInstance().isDefaultCommand(cmd)) {
            cmd.action = 333;
        } else {
            QbeInterfaceWrapper.getInstance().forwardCommand(cmd);
            if (cmd.response.isEmpty() && cmd.bArray == null && cmd.action == 666) {
                cmd.response.add("Unknown Command '" + cmd.command.replaceAll("(\\r|\\n)", "") + "'\r\n");
                cmd.response.add(">");
                cmd.action = 333;
            }
            if (cmd.action == 999) {
                cmd.action = 333;
                cmd.response.add("Attached Device Not Found\r\n");
                cmd.response.add(">");
            }
        }
        this.loggResponse(cmd);
        return true;
    }

    private void doCreatedDevicesClear(String command, CmdStruct cmd) {
        String retStr = this.virtualDeiceList.deleteAll(deviceList);
        if (retStr.isEmpty()) {
            cmd.response.add(STR_OK);
        } else {
            cmd.response.add(retStr);
        }
    }

    private void doCreatedDevicesDelete(String command, CmdStruct cmd) {
        String devName = command.substring($CREATED_DEVICES_DELETE.length() + 1);
        VirtualDevice deleteMe = null;
        for (VirtualDevice vDev : this.virtualDeiceList.getVirtualDevices()) {
            if (!devName.equalsIgnoreCase(vDev.qdi.idnName)) continue;
            deleteMe = vDev;
        }
        if (deleteMe != null) {
            String retStr = this.virtualDeiceList.deleteDevice(deviceList, deleteMe);
            if (retStr.isEmpty()) {
                cmd.response.add(STR_OK);
            } else {
                cmd.response.add(retStr);
            }
        } else {
            cmd.response.add("FAIL: Device not found");
        }
    }

    private void doCreatedDevicesQuery(String command, CmdStruct cmd) {
        boolean noneFound = true;
        for (VirtualDevice vDev : this.virtualDeiceList.getVirtualDevices()) {
            noneFound = false;
            cmd.response.add(vDev.getOrigCmdText());
        }
        if (noneFound) {
            cmd.response.add("None");
        }
    }

    private void doCreateDevice(String command, CmdStruct cmd) {
        String vdError;
        List<Token> tkz;
        try {
            tkz = Tokens.tokenize(new DeviceParser(new StringReader(command)));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ArrayList<String> paramList = new ArrayList<String>();
        if (tkz.size() < 12) {
            cmd.response.add(FAIL_BAD_COMMAND);
            return;
        }
        String cmdStr = tkz.get((int)0).image + tkz.get((int)1).image;
        if (!$CREATE_DEVICE.equalsIgnoreCase(cmdStr)) {
            cmd.response.add(FAIL_BAD_COMMAND);
            return;
        }
        String deviceName = tkz.get((int)2).image;
        paramList.add(deviceName);
        if (!tkz.get((int)3).image.equalsIgnoreCase("newDevice(")) {
            cmd.response.add(FAIL_BAD_COMMAND);
            return;
        }
        String description = tkz.get((int)4).image.replace("\"", "");
        paramList.add(description);
        int i = 5;
        while (i < tkz.size() && !tkz.get((int)i).image.equals(")")) {
            if (!tkz.get((int)i++).image.equalsIgnoreCase("device(")) {
                cmd.response.add(FAIL_BAD_COMMAND);
                return;
            }
            String deviceId = tkz.get((int)i++).image.replace(",", "").trim();
            String prefix = tkz.get((int)i++).image.replace(")", "");
            paramList.add("device");
            paramList.add(deviceId);
            paramList.add(prefix);
        }
        String devSpec = command.substring($CREATE_DEVICE.length() + 1);
        Object vdreturn = this.virtualDeiceList.createDevice(deviceList, paramList, devSpec);
        if (vdreturn instanceof String && !(vdError = (String)vdreturn).isEmpty()) {
            cmd.response.add(vdError);
            return;
        }
        if (vdreturn instanceof VirtualDevice) {
            VirtualDevice vd = (VirtualDevice)vdreturn;
            deviceList.addDLE(vd.dle);
        }
        cmd.response.add(STR_OK);
    }

    private void doSocketsCmd(CmdStruct cmd) {
        List sList = SocketPool.getSocketlist();
        if (sList.isEmpty()) {
            cmd.response.add("No Connections");
        } else {
            for (PooledSocket socket : sList) {
                cmd.response.add(socket.getInetAddress().toString());
            }
        }
    }

    public void doDebugCmd(CmdStruct cmd, String lowerCaseCmd) {
        if (lowerCaseCmd.startsWith("$debug?")) {
            if (DebugUtil.isEnableDebug()) {
                cmd.response.add("ON");
            } else {
                cmd.response.add("OFF");
            }
        } else if (lowerCaseCmd.startsWith("$debug on")) {
            DebugUtil.setEnableDebug(true);
            cmd.response.add(STR_OK);
        } else if (lowerCaseCmd.startsWith("$debug off")) {
            DebugUtil.setEnableDebug(false);
            cmd.response.add(STR_OK);
        } else if (lowerCaseCmd.startsWith("$debug history")) {
            cmd.response.addAll(DebugUtil.history);
            cmd.response.add(STR_OK);
        }
    }

    private void doSleepCmd(CmdStruct cmd, String lowerCaseCmd) {
        String[] parts = lowerCaseCmd.split(" ");
        if (parts.length == 2) {
            try {
                int sleepMs = Integer.parseInt(parts[1]);
                try {
                    Thread.sleep(sleepMs);
                    cmd.response.add(STR_OK);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (NumberFormatException e) {
                cmd.response.add("FAIL: [" + parts[1] + "] is not a number");
            }
        }
        cmd.response.add(">");
        cmd.action = 333;
    }

    public void loggResponse(CmdStruct cmd) {
        if (this.isLoggingOff()) {
            return;
        }
        if (cmd.response == null || cmd.response.isEmpty() && cmd.bArray == null && cmd.getCmBuffer() == null) {
            this.logToDefault(defaultCmdLogLevel, "Command Response: No Response");
        } else {
            if (cmd.bArray != null || cmd.getCmBuffer() != null) {
                this.logToDefault(defaultCmdLogLevel, "Command Response (Trip Time(uS) " + cmd.getRoundTrip_uS() + "): Binary data returned");
            }
            if (cmd.isDebugFilterLongReply()) {
                if (cmd.response.size() > 4) {
                    ArrayList<String> lines = new ArrayList<String>();
                    lines.add("Command Response: (Trip Time(uS): " + cmd.getRoundTrip_uS() + ")");
                    for (int i = 0; i < 2; ++i) {
                        lines.add((String)cmd.response.get(i));
                    }
                    lines.add(".... skipping lines ....");
                    lines.add((String)cmd.response.get(cmd.response.size() - 2));
                    lines.add((String)cmd.response.get(cmd.response.size() - 1));
                    this.logToDefault(defaultCmdLogLevel, "Command Response: ", lines);
                } else {
                    this.logToDefault(defaultCmdLogLevel, "Command Response (Trip Time(uS) " + cmd.getRoundTrip_uS() + "): ", cmd.response);
                }
            } else {
                this.logToDefault(defaultCmdLogLevel, "Command Response (Trip Time(uS) " + cmd.getRoundTrip_uS() + "): ", cmd.response);
            }
        }
    }

    private void doScan(String command, CmdStruct cmd) {
        if ("$scan?".equals(command.toLowerCase())) {
            DeviceScan.getInstance().getScanState(cmd.response);
        } else if ("$scan".equals(command.toLowerCase())) {
            DeviceScan.getInstance().forceScan();
            cmd.response.add("Started network scan");
        } else if (command.toLowerCase().startsWith("$scan -t") && command.toLowerCase().matches(".*\\d.*")) {
            String[] parts = command.split(" ");
            if (parts.length == 2 && parts[1].length() > 2) {
                try {
                    int scanTime = Integer.parseInt(parts[1].substring(2));
                    DeviceScan.getInstance().setScanTime(scanTime);
                    cmd.response.add(STR_OK);
                }
                catch (NumberFormatException e) {
                    cmd.response.add("Invalid command format. Expecing and integer");
                }
            }
        } else {
            DeviceSpecifier devSpec = new DeviceSpecifier(cmd.command);
            if (!devSpec.specifiedByManualIP) {
                devSpec.deviceAddressLookup();
            }
            if (devSpec.specifiedByManualIP) {
                DeviceListEntry dle = QbeInterfaceWrapper.deviceList.findMatchingDevice(devSpec);
                if (dle == null) {
                    if (DeviceScan.getInstance().identify(devSpec.deviceAddress)) {
                        cmd.response.add("Located Device: " + devSpec.getDeviceName());
                    } else {
                        cmd.response.add("No Quarch Device Found at: " + devSpec.deviceAddress);
                    }
                } else {
                    cmd.response.add("Located Device: " + dle.getDeviceIdStr());
                }
            } else {
                cmd.response.add("Invalid command format. Try $scan tcp::IPAddress");
            }
        }
        cmd.response.add(">");
        cmd.action = 333;
    }

    private void getSysInfo(CmdStruct cmd) {
        List<String> info = MemoryInfo.getInfo();
        cmd.response.add("Memory");
        for (String s : info) {
            cmd.response.add(s);
        }
        cmd.response.add("");
        cmd.response.add("Stream Tasks");
        info = StreamBase.getStreamList();
        if (info.size() == 0) {
            cmd.response.add("None");
        } else {
            for (String s : info) {
                cmd.response.add(s);
            }
        }
        cmd.response.add("");
        for (String str : this.cmdListResults) {
            cmd.response.add(str);
        }
        cmd.response.add("");
        cmd.response.add("Working Directory: " + System.getProperty("user.dir"));
        List list = cmd.response;
        StringBuilder stringBuilder = new StringBuilder().append("Application Path: ");
        AppProperties.getInstance();
        list.add(stringBuilder.append(AppProperties.getApplicationPath()).toString());
        List list2 = cmd.response;
        StringBuilder stringBuilder2 = new StringBuilder().append("Application RW Path: ");
        AppProperties.getInstance();
        list2.add(stringBuilder2.append(AppProperties.getApplicationRWFilesPath()).toString());
        cmd.response.add("");
        cmd.response.add("TCP Timeout: " + AppProperties.getInstance().getDefaultTCPTimeout());
        cmd.response.add("TCP Semaphore Timeout: " + AppProperties.getInstance().getDefaultTCPSemaphoreTimeout());
        AppProperties.getInstance();
        EthernetPorts ports = AppProperties.appPropertiesData.getLocalPorts();
        cmd.response.add("");
        if (ports != null) {
            cmd.response.add("Telnet Port: " + ports.getTelnet());
            cmd.response.add("Rest Port: " + ports.getRest());
        } else {
            cmd.response.add("No Port configuration found. Please ensure config file is correct");
        }
        cmd.response.add("");
        cmd.response.add("Comms properties path: " + DeviceInterfaceFeatures.propertiesFilePath);
        cmd.response.add("");
        cmd.response.add(">");
    }

    private void listDevices(CmdStruct cmd) {
        if (deviceList != null) {
            if (cmd.command.toLowerCase().contains("details")) {
                deviceList.getNumberedtDeviceDetails(cmd.response);
            } else {
                deviceList.getNumberedtDeviceSerialNumbers(cmd.response);
            }
        }
        if (cmd.response.isEmpty()) {
            cmd.response.add("No Devices Found\r\n");
        }
    }

    public void executeCommandList(List<String> commandList) {
        this.cmdListResults.add("Processing Start Commands");
        if (commandList == null) {
            this.cmdListResults.add("Start command file is empty");
            return;
        }
        for (String str : commandList) {
            if ((str = str.trim()).isEmpty() || str.startsWith("#")) continue;
            try {
                Thread.sleep(20L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.cmdListResults.add("Executing: " + str);
            CmdStruct cmd = new CmdStruct();
            cmd.command = str;
            this.internalCmdDecode(cmd);
            if (cmd.response.size() == 0) {
                this.cmdListResults.add("\tNo Response from device");
                continue;
            }
            for (String response : cmd.response) {
                this.cmdListResults.add("\t" + response);
            }
        }
    }

    static {
        initialisationComplete = false;
        shutdown = false;
    }

    class TokenString {
        final String origStr;
        final char[] cArray;
        int idx = 0;
        private char lastDelimiter = '\u0000';

        public TokenString(String str) {
            this.origStr = str;
            this.cArray = str.toCharArray();
        }

        public String getNextFunctionToken() {
            String functStr = this.getNextToken(new char[]{'('});
            return functStr;
        }

        public String getNextToken(char[] delimiters) {
            String retVal = null;
            try {
                this.skipWhiteSpace(null);
                int startIdx = this.idx;
                do {
                    ++this.idx;
                } while (this.idx < this.cArray.length && !Character.isWhitespace(this.cArray[this.idx]) && !this.isTerminator(this.cArray[this.idx], delimiters));
                retVal = this.idx >= this.cArray.length ? this.origStr.substring(startIdx) : this.origStr.substring(startIdx, this.idx);
            }
            catch (Exception e) {
                return null;
            }
            return retVal;
        }

        public boolean isTerminator(char ch, char[] delimiters) {
            this.lastDelimiter = '\u0000';
            if (Character.isWhitespace(ch)) {
                this.lastDelimiter = ch;
                return true;
            }
            if (delimiters == null) {
                return false;
            }
            for (char c : delimiters) {
                if (ch != c) continue;
                this.lastDelimiter = ch;
                return true;
            }
            return false;
        }

        public void skipWhiteSpace(char[] delimiters) {
            while (this.isTerminator(this.cArray[this.idx], delimiters)) {
                ++this.idx;
            }
        }

        public boolean skipOver(String str) {
            int pos = this.origStr.indexOf(str, this.idx);
            if (pos == -1) {
                return false;
            }
            this.idx += str.length();
            return true;
        }

        public char getLastDelimiter() {
            return this.lastDelimiter;
        }
    }
}

