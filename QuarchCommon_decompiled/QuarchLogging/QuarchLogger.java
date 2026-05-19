/*
 * Decompiled with CFR 0.152.
 */
package QuarchLogging;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuarchLogger {
    private static final Logger baseLogger = Logger.getLogger("global");
    private static FileHandler fileTxt = null;
    private static String logFilePath;
    private static String logFilePrefix;
    private static int logFileMaxSize;
    private static int logFileCount;
    private static Level origionalConsoleHandlerLevel;
    private static PrintStream origionalStdErrStream;
    private static boolean enableExceptionToConsole;
    private static boolean initialized;
    private static boolean DisableConsole;
    static Level selectedLogLevel;
    static String selectedlogFilePath;

    public static boolean isInitialized() {
        return initialized;
    }

    public static void initQuarchLogger(String logFilePath, String logFilePrefix, int logFileMaxSize, int logFileCount) {
        if (!initialized) {
            initialized = true;
            QuarchLogger.logFilePath = logFilePath;
            QuarchLogger.logFilePrefix = logFilePrefix;
            QuarchLogger.logFileMaxSize = logFileMaxSize;
            QuarchLogger.logFileCount = logFileCount;
            QuarchLogger.setDefaults(QuarchLogger.getBaselogger());
            Logger rootLogger = Logger.getLogger("");
            origionalConsoleHandlerLevel = rootLogger.getLevel();
            origionalStdErrStream = System.err;
            QuarchLogger.suppressConsole(rootLogger);
        }
    }

    public static void initQuarchLogger(boolean disableConsole, String logFilePath, String logFilePrefix, int logFileMaxSize, int logFileCount) {
        if (!initialized) {
            initialized = true;
            QuarchLogger.logFilePath = logFilePath;
            QuarchLogger.logFilePrefix = logFilePrefix;
            QuarchLogger.logFileMaxSize = logFileMaxSize;
            QuarchLogger.logFileCount = logFileCount;
            QuarchLogger.setLevelDefaults(QuarchLogger.getBaselogger());
            Logger rootLogger = Logger.getLogger("");
            origionalConsoleHandlerLevel = rootLogger.getLevel();
            origionalStdErrStream = System.err;
            DisableConsole = disableConsole;
        }
    }

    private static void redirectStdErr() {
        System.setErr(new PrintStream(new LoggerStream(QuarchLogger.getBaselogger(), Level.SEVERE, System.err), true));
    }

    private static void restoreStdErr() {
        System.setErr(origionalStdErrStream);
    }

    public static Logger getLoggerForClass(Object classObj) {
        return Logger.getLogger(classObj.getClass().getName());
    }

    private static void setDefaults(Logger logger) {
        logger.setLevel(Level.OFF);
        logger.severe("Severe");
        logger.warning("Warning");
        logger.info("Info");
        logger.finest("Finest");
    }

    private static void setLevelDefaults(Logger logger) {
        logger.setLevel(Level.OFF);
        logger.severe("Severe");
        logger.warning("Warning");
        logger.info("Info");
        logger.finest("Finest");
    }

    public static void suppressConsole(Logger logger) {
        Handler[] handlers;
        for (Handler handler : handlers = logger.getHandlers()) {
            if (!(handler instanceof ConsoleHandler)) continue;
            handler.setLevel(Level.OFF);
        }
    }

    public static void restoreConsoleHandller(Logger logger) {
        QuarchLogger.enableConsoleHandller(logger, origionalConsoleHandlerLevel);
    }

    public static void enableConsoleHandller(Logger logger, Level level) {
        Handler[] handlers;
        for (Handler handler : handlers = logger.getHandlers()) {
            if (!(handler instanceof ConsoleHandler)) continue;
            handler.setLevel(level);
        }
    }

    public static Level getLoggerLevel() {
        return QuarchLogger.getBaselogger().getLevel();
    }

    public static void setLoggerLevel(Level level) {
        selectedLogLevel = level;
    }

    public static void startLogger() {
        if (selectedLogLevel != Level.OFF) {
            QuarchLogger.setFileHandler();
            Logger rootLogger = Logger.getLogger("");
            if (DisableConsole) {
                QuarchLogger.suppressConsole(rootLogger);
            }
            QuarchLogger.redirectStdErr();
        } else {
            Logger rootLogger = Logger.getLogger("");
            QuarchLogger.restoreConsoleHandller(rootLogger);
            QuarchLogger.restoreStdErr();
        }
        QuarchLogger.getBaselogger().setLevel(selectedLogLevel);
    }

    public static boolean setLoggerLevel(String levelStr) {
        try {
            if (levelStr.equalsIgnoreCase("ON")) {
                Level level = Level.parse("INFO");
                QuarchLogger.setLoggerLevel(level);
                DisableConsole = false;
            } else {
                Level level = Level.parse(levelStr.toUpperCase());
                QuarchLogger.setLoggerLevel(level);
                DisableConsole = false;
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean isOff() {
        return QuarchLogger.getLoggerLevel() == Level.OFF;
    }

    public static void setLoggingOn() {
        QuarchLogger.setLoggerLevel(Level.INFO);
    }

    private static void setFileHandler() {
        if (fileTxt == null) {
            try {
                fileTxt = new FileHandler(logFilePath + File.separator + logFilePrefix + "-%g.log", logFileMaxSize, logFileCount, true);
                CustomXMLLogFormatter customFormatter = new CustomXMLLogFormatter();
                fileTxt.setFormatter(customFormatter);
                Logger rootLogger = Logger.getLogger("");
                rootLogger.addHandler(fileTxt);
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logMessage(Logger logger, Level logLevel, String msg) {
        if (!QuarchLogger.isOff() && logger != null && logLevel.intValue() >= QuarchLogger.getBaselogger().getLevel().intValue()) {
            logger.log(logLevel, msg);
        }
    }

    public static Logger getBaselogger() {
        return baseLogger;
    }

    public static void logMessage(Level level, String string) {
        if (QuarchLogger.getBaselogger() != null) {
            QuarchLogger.getBaselogger().log(level, string);
        }
    }

    private static boolean isEnableExceptionToConsole() {
        return enableExceptionToConsole;
    }

    public static void setEnableExceptionToConsole(boolean value) {
        enableExceptionToConsole = value;
    }

    public static String getLogFilePath() {
        return logFilePath;
    }

    public static void setLogFilePath(String newLogFilePath) {
        logFilePath = newLogFilePath.endsWith(File.separator) ? newLogFilePath : newLogFilePath + File.separator;
    }

    public static void setLogTarget(boolean disableConsole, String newLogFilePath) {
        QuarchLogger.setLogFilePath(newLogFilePath);
        DisableConsole = disableConsole;
    }

    static {
        enableExceptionToConsole = true;
        initialized = false;
        selectedLogLevel = Level.OFF;
        selectedlogFilePath = logFilePath + File.separator;
    }

    private static class CustomXMLLogFormatter
    extends Formatter {
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        private boolean headerWritten = false;

        private CustomXMLLogFormatter() {
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            String threadName = "";
            String threadId = "";
            String className = "";
            String methodName = "";
            String message = "";
            Pattern pattern = Pattern.compile("\\[(?<threadName>.*?)\\]\\s*\\[(?<threadId>.*?)\\]\\s*\\[(?<className>.*?)\\]\\s*\\[(?<methodName>.*?)\\]\\s*(?<message>.*)");
            Matcher matcher = pattern.matcher(record.getMessage());
            if (matcher.matches()) {
                threadName = matcher.group("threadName");
                threadId = matcher.group("threadId");
                className = matcher.group("className");
                methodName = matcher.group("methodName");
                if (methodName.equals("ME:")) {
                    methodName = record.getSourceMethodName();
                }
                message = matcher.group("message");
            } else {
                threadId = "" + record.getThreadID();
                className = record.getSourceClassName();
                message = record.getMessage();
                methodName = record.getSourceMethodName();
            }
            if (!this.headerWritten) {
                builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
                builder.append("<!DOCTYPE log SYSTEM \"logger.dtd\">\n");
                this.headerWritten = true;
            }
            builder.append("<record>\n");
            builder.append("  <date>").append(this.dateFormat.format(new Date(record.getMillis()))).append("</date>\n");
            builder.append("  <millis>").append(record.getMillis()).append("</millis>\n");
            builder.append("  <sequence>").append(record.getSequenceNumber()).append("</sequence>\n");
            builder.append("  <logger>").append(record.getLoggerName()).append("</logger>\n");
            builder.append("  <level>").append(record.getLevel()).append("</level>\n");
            builder.append("  <class>").append(className).append("</class>\n");
            builder.append("  <method>").append(methodName).append("</method>\n");
            builder.append("  <thread>").append(threadName).append(" ").append(threadId).append("</thread>\n");
            builder.append("  <message>").append(message).append("</message>\n");
            builder.append("</record>\n");
            return builder.toString();
        }

        @Override
        public String getHead(Handler h) {
            return "<log>\n";
        }

        @Override
        public String getTail(Handler h) {
            return "</log>\n";
        }
    }

    private static class LoggerStream
    extends OutputStream {
        private final Logger logger;
        private final Level logLevel;
        private final OutputStream outputStream;
        private StringBuilder sbBuffer;

        public LoggerStream(Logger logger, Level logLevel, OutputStream outputStream) {
            this.logger = logger;
            this.logLevel = logLevel;
            this.outputStream = outputStream;
            this.sbBuffer = new StringBuilder();
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.doWrite(new String(b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.doWrite(new String(b, off, len));
        }

        @Override
        public void write(int b) throws IOException {
            this.doWrite(String.valueOf((char)b));
        }

        private void doWrite(String str) throws IOException {
            this.sbBuffer.append(str);
            if (this.sbBuffer.charAt(this.sbBuffer.length() - 1) == '\n') {
                this.sbBuffer.setLength(this.sbBuffer.length() - 1);
                if (this.sbBuffer.charAt(this.sbBuffer.length() - 1) == '\r') {
                    this.sbBuffer.setLength(this.sbBuffer.length() - 1);
                }
                String buf = this.sbBuffer.toString();
                this.sbBuffer.setLength(0);
                if (QuarchLogger.isEnableExceptionToConsole()) {
                    this.outputStream.write(buf.getBytes());
                    this.outputStream.write(10);
                }
                this.logger.log(this.logLevel, buf);
            }
        }
    }
}

