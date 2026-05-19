/*
 * Decompiled with CFR 0.152.
 */
package qis;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import qis.Globals;
import qis.SystrayActions;
import qis.SystrayListener;
import qis.qisMain;

public class SystrayIcon {
    public boolean isSupported = false;
    public int errorCode = 0;
    private SystrayActions requestedAction = SystrayActions.None;
    private SystemTray tray;
    private PopupMenu popup;
    private TrayIcon trayIcon;
    private MenuItem aboutItem;
    private MenuItem newTerminal;
    private MenuItem exitItem;
    private List<SystrayListener> listeners = new ArrayList<SystrayListener>();

    protected static Image createImage(String path, String description) {
        qisMain.class.getResource("").getPath();
        qisMain.class.getResource(path);
        return new ImageIcon(path, description).getImage();
    }

    SystrayIcon() {
        this.isSupported = SystemTray.isSupported();
        if (!this.isSupported) {
            this.errorCode = -1;
            return;
        }
        BufferedImage image = Globals.applicationIcons.getAsBufferedImage(0);
        this.trayIcon = new TrayIcon(image);
        this.trayIcon.setToolTip("QIS");
        this.tray = SystemTray.getSystemTray();
        this.addPopupItems();
        this.trayIcon.setPopupMenu(this.popup);
        try {
            this.tray.add(this.trayIcon);
        }
        catch (AWTException | UnsupportedOperationException e) {
            this.errorCode = -2;
            this.isSupported = false;
            System.out.println("TrayIcon could not be added.");
        }
    }

    private void addPopupItems() {
        this.aboutItem = new MenuItem("About");
        this.newTerminal = new MenuItem("Show Terminal");
        this.exitItem = new MenuItem("Exit");
        this.popup = new PopupMenu();
        this.popup.add(this.aboutItem);
        this.popup.addSeparator();
        this.popup.add(this.newTerminal);
        this.popup.addSeparator();
        this.popup.add(this.exitItem);
        this.trayIcon.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        this.aboutItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                SystrayIcon.this.fireSystrayEvent(SystrayActions.About);
            }
        });
        this.newTerminal.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                SystrayIcon.this.fireSystrayEvent(SystrayActions.NewTerminal);
            }
        });
        this.exitItem.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                SystrayIcon.this.fireSystrayEvent(SystrayActions.Exit);
            }
        });
    }

    public synchronized void setSystrayAction(SystrayActions act) {
        this.requestedAction = act;
    }

    public synchronized void clearSystrayAction() {
        this.requestedAction = SystrayActions.None;
    }

    public synchronized SystrayActions getSystrayAction() {
        return this.requestedAction;
    }

    public synchronized void removeSysTrayIcon() {
        this.tray.remove(this.trayIcon);
    }

    public void setAboutShowing(boolean showing) {
        this.aboutItem.setEnabled(!showing);
    }

    public void setNewTerminalEnabled(boolean enabled) {
        this.newTerminal.setEnabled(enabled);
    }

    public void addSystrayListener(SystrayListener toAdd) {
        this.listeners.add(toAdd);
    }

    private void fireSystrayEvent(SystrayActions action) {
        for (SystrayListener hl : this.listeners) {
            hl.systrayEventListener(action);
        }
    }
}

