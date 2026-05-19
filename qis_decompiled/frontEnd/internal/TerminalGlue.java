/*
 * Decompiled with CFR 0.152.
 */
package frontEnd.internal;

import commandProcessor.CmdProcessorSingleton;
import frontEnd.internal.TerminalWindow;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class TerminalGlue
implements Runnable {
    public static TerminalWindow terminalFacade;
    private static CmdProcessorSingleton cmdProcessor;
    private DeviceListEntry defaultDevice = null;

    private static void openTerminal(boolean visible) {
        String lookAndFeel = "Nimbus";
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (!lookAndFeel.equals(info.getName())) continue;
                UIManager.setLookAndFeel(info.getClassName());
                UIDefaults defaults = UIManager.getLookAndFeelDefaults();
                defaults.put("Table.gridColor", new Color(214, 217, 223));
                defaults.put("Table.disabled", (Object)false);
                defaults.put("Table.showGrid", (Object)true);
                defaults.put("Table.intercellSpacing", new Dimension(1, 1));
                defaults.put("Table.intercellSpacing", new Dimension(1, 1));
                break;
            }
        }
        catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        terminalFacade = new TerminalWindow(lookAndFeel);
        terminalFacade.setVisible(visible);
    }

    public TerminalGlue(boolean visible) {
        cmdProcessor = CmdProcessorSingleton.getInstance();
        TerminalGlue.openTerminal(visible);
        new Thread(this).start();
    }

    public void close() {
        terminalFacade.setVisible(false);
    }

    public void directWrite(String s) {
        terminalFacade.printStr(s);
    }

    @Override
    public void run() {
        boolean done = false;
        while (!done) {
            try {
                String cmd = TerminalGlue.terminalFacade.cmdSendQ.take();
                CmdStruct cmdStruct = new CmdStruct();
                cmdStruct.command = cmd;
                cmdStruct.setDefaultDevice(this.defaultDevice);
                cmdProcessor.cmdDecode(cmdStruct);
                if (cmdStruct.getDefaultDevice() != null) {
                    this.defaultDevice = cmdStruct.getDefaultDevice();
                } else if (cmdStruct.getDefaultDevice() == null && !cmdStruct.defaultDeviceIsValid) {
                    this.defaultDevice = null;
                }
                if (cmdStruct.getStringBuilder() != null) {
                    String[] strs = cmdStruct.getStringBuilder().toString().split("\r\n");
                    if (strs.length == 0) {
                        TerminalGlue.terminalFacade.cmdReplyQ.put(cmdStruct.getStringBuilder().toString());
                        continue;
                    }
                    for (String s : strs) {
                        if (s.startsWith(">")) {
                            TerminalGlue.terminalFacade.cmdReplyQ.put(s);
                            continue;
                        }
                        TerminalGlue.terminalFacade.cmdReplyQ.put(s);
                    }
                    continue;
                }
                if (cmdStruct.response.size() >= 1) {
                    if (((String)cmdStruct.response.get(0)).equals("\\")) {
                        TerminalGlue.terminalFacade.cmdReplyQ.put(">");
                        continue;
                    }
                    for (String s : cmdStruct.response) {
                        if (s.startsWith(">")) {
                            TerminalGlue.terminalFacade.cmdReplyQ.put(s);
                            continue;
                        }
                        TerminalGlue.terminalFacade.cmdReplyQ.put(s);
                    }
                    continue;
                }
                if (cmdStruct.bArray != null) {
                    TerminalGlue.terminalFacade.cmdReplyQ.put("Terminal cannot display results from BIN commands");
                    TerminalGlue.terminalFacade.cmdReplyQ.put(">");
                    continue;
                }
                TerminalGlue.terminalFacade.cmdReplyQ.put("Command did not return a response");
                TerminalGlue.terminalFacade.cmdReplyQ.put(">");
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

