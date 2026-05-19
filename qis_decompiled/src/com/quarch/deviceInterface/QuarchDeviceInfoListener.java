/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceInterface;

import java.util.TreeMap;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;

public interface QuarchDeviceInfoListener {
    public void QuarchDeviceInfoEvent(TreeMap<String, QuarchDeviceInfo> var1, TreeMap<String, QuarchDeviceInfo> var2);
}

