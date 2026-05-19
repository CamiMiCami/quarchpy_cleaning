/*
 * Decompiled with CFR 0.152.
 */
package commandProcessor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MemoryInfo {
    public static List<String> getInfo() {
        float mb = 1048576.0f;
        ArrayList<String> retVal = new ArrayList<String>();
        DecimalFormat df = new DecimalFormat("#.000000");
        Runtime runtime = Runtime.getRuntime();
        retVal.add("Used Memory: " + df.format((float)(runtime.totalMemory() - runtime.freeMemory()) / mb) + " MB");
        retVal.add("Free Memory: " + df.format((float)runtime.freeMemory() / mb) + " MB");
        retVal.add("Total Memory: " + df.format((float)runtime.totalMemory() / mb) + " MB");
        retVal.add("Max Memory: " + df.format((float)runtime.maxMemory() / mb) + " MB");
        return retVal;
    }
}

