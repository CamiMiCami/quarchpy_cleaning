/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceXML;

public class Identity {
    public String Description;
    public String Class;
    public String Part;
    public String FwVersion;

    public boolean isPPM() {
        return this.Class.contains("PPM") && !this.Class.contains("PAM");
    }

    public boolean isPAM() {
        return this.Class.contains("PAM") && !this.Class.contains("PPM");
    }

    public boolean isPPM_PAM() {
        return this.Class.contains("PPM") && this.Class.contains("PAM");
    }

    public boolean isPAM_AC() {
        return this.Part.equals("QTL2582") || this.Part.equals("QTL2751") || this.Part.equals("QTL2789");
    }
}

