/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.xml.bind.annotation.XmlAccessType
 *  jakarta.xml.bind.annotation.XmlAccessorType
 *  jakarta.xml.bind.annotation.XmlElement
 *  jakarta.xml.bind.annotation.XmlRootElement
 */
package QuarchApplication;

import QuarchApplication.FileInformation;
import QuarchApplication.VersionNumber;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="QuarchUpdateInfo")
@XmlAccessorType(value=XmlAccessType.PUBLIC_MEMBER)
public class QuarchUpdateInfo {
    public String LatestVersion;
    @XmlElement(name="FileList")
    public List<FileInformation> FileList;

    public String isLatest(String appVersion) {
        VersionNumber curVersion = new VersionNumber(appVersion);
        VersionNumber webVersion = new VersionNumber(this.LatestVersion);
        if (curVersion.compareTo(webVersion) == -1) {
            return this.LatestVersion;
        }
        return "latest";
    }
}

