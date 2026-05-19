/*
 * Decompiled with CFR 0.152.
 */
package QuarchApplication;

import java.util.ArrayList;

public class VersionNumber
implements Comparable<VersionNumber> {
    public final int[] version;

    public VersionNumber(int[] version) {
        this.version = new int[version.length];
        for (int i = 0; i < version.length; ++i) {
            this.version[i] = version[i];
        }
    }

    public VersionNumber(String ver) throws NumberFormatException {
        String[] subver = ver.split("\\.");
        this.version = new int[subver.length];
        for (int i = 0; i < this.version.length; ++i) {
            this.version[i] = Integer.parseInt(subver[i]);
        }
    }

    @Override
    public int compareTo(VersionNumber o) {
        int depth = o.version.length > this.version.length ? o.version.length : this.version.length;
        for (int i = 0; i < depth; ++i) {
            int ourV;
            int otherV = i >= o.version.length ? 0 : o.version[i];
            if (otherV > (ourV = i >= this.version.length ? 0 : this.version[i])) {
                return -1;
            }
            if (otherV >= ourV) continue;
            return 1;
        }
        return 0;
    }

    public String toString() {
        ArrayList<String> nums = new ArrayList<String>(this.version.length);
        for (int n = 0; n < this.version.length; ++n) {
            nums.add(Integer.toString(this.version[n]));
        }
        return String.join((CharSequence)".", nums);
    }
}

