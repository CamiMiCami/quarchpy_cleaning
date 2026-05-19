/*
 * Decompiled with CFR 0.152.
 */
package commandProcessor;

import appBase.AppVersion;
import java.util.ArrayList;
import java.util.List;

public class CmdHelp {
    List<String> helpStrings = new ArrayList<String>();

    public CmdHelp() {
        AppVersion.getAppHeaderInfo(this.helpStrings);
        this.helpStrings.add("");
        this.helpStrings.add("Application Start Directory: " + System.getProperty("user.dir"));
        this.helpStrings.add("");
        this.helpStrings.add("Help:");
        this.helpStrings.add("");
        this.helpStrings.add("$list : list attached devices");
        this.helpStrings.add("$list details : list attached devices with additional information");
        this.helpStrings.add("$scan : perform a scan for connected devices");
        this.helpStrings.add("");
        this.helpStrings.add("$default device : set device as default for all device commands");
        this.helpStrings.add("$default? : displays the current default device");
        this.helpStrings.add("");
        this.helpStrings.add("$sysinfo : Displays memory usage and stream task information, eg");
        this.helpStrings.add("");
        this.helpStrings.add("$shutdown : close this application");
        this.helpStrings.add("");
        this.helpStrings.add("stream? : returns running status and buffer information for the current stream operation");
        this.helpStrings.add("stream text header : returns header information for the current stream operation");
        this.helpStrings.add("stream text [all|number of stripes] : returns N stripes in formatted ASCII text from the stream buffer");
        this.helpStrings.add("stream bin [all|number of stripes] : returns N stripes in binary format from the stream buffer");
        this.helpStrings.add("");
        this.helpStrings.add(">");
    }
}

