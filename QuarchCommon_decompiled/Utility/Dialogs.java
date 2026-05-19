/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.scene.control.Alert
 *  javafx.scene.control.Alert$AlertType
 *  javafx.scene.control.ButtonType
 */
package Utility;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class Dialogs {
    public static ButtonType alert(Alert.AlertType at, String title, String headerLine, String contentLine) {
        Alert alert = new Alert(at);
        alert.setTitle(title);
        alert.setHeaderText(headerLine);
        alert.setContentText(contentLine);
        Optional result = alert.showAndWait();
        if (result.isPresent()) {
            return (ButtonType)result.get();
        }
        return null;
    }
}

