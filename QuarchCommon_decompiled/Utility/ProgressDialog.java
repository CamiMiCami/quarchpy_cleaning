/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.beans.property.SimpleDoubleProperty
 *  javafx.beans.value.ObservableValue
 *  javafx.fxml.FXML
 *  javafx.scene.Node
 *  javafx.scene.control.Alert
 *  javafx.scene.control.Alert$AlertType
 *  javafx.scene.control.ButtonType
 *  javafx.scene.control.DialogPane
 *  javafx.scene.control.Label
 *  javafx.scene.control.ProgressBar
 *  javafx.scene.layout.VBox
 */
package Utility;

import java.util.Optional;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressDialog
implements Runnable {
    @FXML
    private ProgressBar numFileProgress;
    @FXML
    private ProgressBar fileProgress;
    private SimpleDoubleProperty propFileProgress;
    private SimpleDoubleProperty propNumFileProgress;

    public ProgressDialog(SimpleDoubleProperty numFileProg, SimpleDoubleProperty fileProg) {
        this.propFileProgress = fileProg;
        this.propNumFileProgress = numFileProg;
    }

    @Override
    public void run() {
        String titleText = "Download Progress";
        String headerText = "Downloading Files For Update";
        String contentText = "Downloading files ...";
        Alert alert = new Alert(Alert.AlertType.INFORMATION, contentText, new ButtonType[]{ButtonType.CANCEL});
        alert.setTitle(titleText);
        alert.setHeaderText(headerText);
        VBox vbox = new VBox();
        ProgressBar numFileProgress = new ProgressBar();
        ProgressBar fileProgress = new ProgressBar();
        Label label1 = new Label("Number of files to download");
        numFileProgress.progressProperty().bind((ObservableValue)this.propNumFileProgress);
        Label label2 = new Label("File download progress");
        fileProgress.progressProperty().bind((ObservableValue)this.propFileProgress);
        vbox.getChildren().addAll((Object[])new Node[]{label1, numFileProgress, label2, fileProgress});
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setExpandableContent((Node)vbox);
        dialogPane.setExpanded(true);
        Optional selection = alert.showAndWait();
        if (selection.get() == ButtonType.CANCEL) {
            return;
        }
    }
}

