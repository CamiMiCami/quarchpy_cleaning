/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class DebugUtil {
    private static boolean enableDebug = false;
    private static boolean enableDevDebug = false;
    private static boolean enableCostlyDevDebug = false;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final int historyMaxSize = 1024;
    public static List<String> history = new LinkedList<String>();

    public static String dateStr() {
        LocalDateTime date = LocalDateTime.now();
        return "[" + date.format(formatter) + "] ";
    }

    private static void addToHistory(String s) {
        String str = new String(s);
        while (history.size() > 1024) {
            history.remove(0);
        }
        history.add(str);
    }

    public static void debugMsg(String s) {
        if (DebugUtil.isEnableDebug()) {
            System.out.print(s);
            DebugUtil.addToHistory(s);
        }
    }

    public static void debugMsgln(String s) {
        if (DebugUtil.isEnableDebug()) {
            String str = DebugUtil.dateStr() + s;
            System.out.println(str);
            DebugUtil.addToHistory(str);
        }
    }

    public static void debugError(String s) {
        if (DebugUtil.isEnableDebug()) {
            System.err.print(s);
            DebugUtil.addToHistory(s);
        }
    }

    public static void debugErrorln(String s) {
        if (DebugUtil.isEnableDebug()) {
            String str = DebugUtil.dateStr() + s;
            System.err.println(str);
            DebugUtil.addToHistory(str);
        }
    }

    public static boolean isEnableDebug() {
        return enableDebug;
    }

    public static void setEnableDebug(boolean enableDebug) {
        DebugUtil.enableDebug = enableDebug;
    }

    public static void devDebugMsgln(String s) {
        if (enableDevDebug) {
            System.out.println(s);
        }
    }

    public static boolean isEnableDevDebug() {
        return enableDevDebug;
    }

    public static void setEnableDevDebug(boolean b) {
        enableDevDebug = b;
    }

    public static boolean isEnableCostlyDevDebug() {
        return enableCostlyDevDebug;
    }

    public static void setEnableCostlyDevDebug(boolean enableCostlyDevDebug) {
        DebugUtil.enableCostlyDevDebug = enableCostlyDevDebug;
    }
}

