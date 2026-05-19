/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.scene.control.Alert$AlertType
 *  javafx.scene.control.ButtonType
 */
package QuarchApplication;

import QuarchApplication.FileFetcher;
import QuarchApplication.QuarchApplication;
import QuarchApplication.QuarchUpdateInfo;
import Utility.Dialogs;
import Utility.JavaFXUtilities;
import Utility.ProgressDialog;
import java.util.concurrent.ArrayBlockingQueue;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class CheckLatestVersion
implements Runnable {
    private Thread thread;
    private String appName;
    private Runnable appShutdown;
    private final String currentVersion;
    private final String latestVersionUrl;
    private ArrayBlockingQueue<Boolean> installAnswer;

    public CheckLatestVersion(String appName, Runnable appShutdown, String currentVersion, String latestVersionUrl) {
        this.appName = appName;
        this.appShutdown = appShutdown;
        this.currentVersion = currentVersion;
        this.latestVersionUrl = latestVersionUrl;
        this.installAnswer = new ArrayBlockingQueue(1);
        this.thread = new Thread(this);
    }

    public void start() {
        this.thread.start();
    }

    @Override
    public void run() {
        final QuarchUpdateInfo updateInfo = QuarchApplication.getUpdateInfo(this.latestVersionUrl);
        if (updateInfo == null) {
            return;
        }
        if (!updateInfo.isLatest(this.currentVersion).contentEquals("latest")) {
            Platform.runLater((Runnable)new Runnable(){

                @Override
                public void run() {
                    String title = CheckLatestVersion.this.appName;
                    String header = "New version of " + CheckLatestVersion.this.appName + " available";
                    String msg = "A newer version of " + CheckLatestVersion.this.appName + " (" + updateInfo.LatestVersion + ") is available. You are currently on version " + CheckLatestVersion.this.currentVersion + ".  Would you like to download and install the new version?";
                    ButtonType result = Dialogs.alert(Alert.AlertType.CONFIRMATION, title, header, msg);
                    try {
                        CheckLatestVersion.this.installAnswer.put(result == ButtonType.OK);
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                }
            });
        }
        try {
            if (this.installAnswer.take().booleanValue()) {
                FileFetcher ff = new FileFetcher(updateInfo.FileList);
                ProgressDialog pd = new ProgressDialog(ff.filesProgress, ff.fileProgress);
                Platform.runLater((Runnable)pd);
                ff.run();
                ff.executeInstaller();
                JavaFXUtilities.runOnFxThread(this.appShutdown);
            }
        }
        catch (InterruptedException e) {
            return;
        }
    }
}

