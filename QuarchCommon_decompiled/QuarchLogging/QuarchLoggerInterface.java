/*
 * Decompiled with CFR 0.152.
 */
package QuarchLogging;

import QuarchLogging.QuarchLogger;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface QuarchLoggerInterface {
    default public Logger getLoggerForClass(Object classObj) {
        return Logger.getLogger(classObj.getClass().getName());
    }

    default public boolean isLoggingOff() {
        return QuarchLogger.isOff();
    }

    public static void logToDefault(String staticClassName, Level level, String msg) {
        Thread thisThread = Thread.currentThread();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String methodName = stackTrace[2].getMethodName();
        StringBuilder sb = new StringBuilder();
        QuarchLoggerInterface.buildMessageToStringBuilder(sb, thisThread, staticClassName, "", msg);
        QuarchLogger.logMessage(level, sb.toString());
    }

    public static void logToDefault(String staticClassName, String staticMethodName, Level level, String msg) {
        Thread thisThread = Thread.currentThread();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String methodName = stackTrace[2].getMethodName();
        StringBuilder sb = new StringBuilder();
        QuarchLoggerInterface.buildMessageToStringBuilder(sb, thisThread, staticClassName, staticMethodName, msg);
        QuarchLogger.logMessage(level, sb.toString());
    }

    default public void logToDefault(Object obj, Level level, String msg) {
        QuarchLoggerInterface.logToDefault(obj.getClass().getName(), level, msg);
    }

    default public void logToDefault(Object obj, String methodName, Level level, String msg) {
        QuarchLoggerInterface.logToDefault(obj.getClass().getName(), methodName, level, msg);
    }

    default public void logToDefault(Level level, String msg) {
        Class<?> thisClass = this.getClass();
        Thread thisThread = Thread.currentThread();
        StringBuilder sb = new StringBuilder();
        QuarchLoggerInterface.buildMessageToStringBuilder(sb, thisThread, thisClass.getName(), "", msg);
        QuarchLogger.logMessage(level, sb.toString());
    }

    default public void logToDefault(Level level, String msg, List<String> msgList) {
        Class<?> thisClass = this.getClass();
        Thread thisThread = Thread.currentThread();
        StringBuilder sb = new StringBuilder();
        QuarchLoggerInterface.buildMessageToStringBuilder(sb, thisThread, thisClass.getName(), msg, msgList);
        QuarchLogger.logMessage(level, sb.toString());
    }

    public static void buildMessageToStringBuilder(StringBuilder sb, Thread thisThread, String classsName, String msg, List<String> msgList) {
        QuarchLoggerInterface.buildMessagePrefix(sb, thisThread, classsName, "");
        sb.append(msg);
        for (String str : msgList) {
            sb.append("[");
            sb.append(str);
            sb.append("]");
        }
    }

    public static void buildMessagePrefix(StringBuilder sb, Thread thisThread, String classsName, String methodName) {
        QuarchLoggerInterface.idStringToSB(sb, "TN", thisThread.getName());
        QuarchLoggerInterface.idStringToSB(sb, "TI", Long.toString(thisThread.getId()));
        QuarchLoggerInterface.idStringToSB(sb, "CL", classsName);
        QuarchLoggerInterface.idStringToSB(sb, "ME", methodName);
    }

    public static void buildMessageToStringBuilder(StringBuilder sb, Thread thisThread, String classsName, String methodName, String msg) {
        QuarchLoggerInterface.idStringToSB(sb, "TN", thisThread.getName());
        QuarchLoggerInterface.idStringToSB(sb, "TI", Long.toString(thisThread.getId()));
        QuarchLoggerInterface.idStringToSB(sb, "CL", classsName);
        QuarchLoggerInterface.idStringToSB(sb, "ME", methodName);
        sb.append(msg);
    }

    public static void idStringToSB(StringBuilder sb, String id, String fieldStr) {
        sb.append("[");
        sb.append(id);
        sb.append(":");
        sb.append(fieldStr);
        sb.append("]");
    }
}

