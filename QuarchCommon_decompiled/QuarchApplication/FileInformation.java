/*
 * Decompiled with CFR 0.152.
 */
package QuarchApplication;

public class FileInformation {
    public String FileName;
    public String URL;
    public String Checksum;

    public FileInformation() {
    }

    public FileInformation(String name, String url) {
        this.FileName = name;
        this.URL = url;
    }

    public void checkFileName() {
        if (this.FileName == null && this.URL != null) {
            String[] pathSplit = this.URL.split("/");
            String fileName = pathSplit[pathSplit.length - 1];
            this.FileName = fileName.replaceAll("%20", " ");
        }
    }
}

