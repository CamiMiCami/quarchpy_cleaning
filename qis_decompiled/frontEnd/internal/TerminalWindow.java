/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package frontEnd.internal;

import appBase.AppVersion;
import frontEnd.internal.CommandHistory;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import org.apache.commons.lang3.StringUtils;
import qis.Globals;
import src.com.quarch.utils.DebugUtil;

public class TerminalWindow
extends JFrame
implements ActionListener,
Runnable {
    private static final long serialVersionUID = 2414175872579577827L;
    private JMenuItem mntmEditPaste;
    private JMenuItem mntmEditCopy;
    private JTextArea terminalWindow;
    private JMenuBar menuBar_1;
    private TextTransfer textTransfer = new TextTransfer();
    private int keyActioned = -1;
    private CommandHistory commandHistory = new CommandHistory();
    private JCheckBoxMenuItem chckbxmntmDebug;
    private volatile boolean cmdInFlight = false;
    public BlockingQueue<String> cmdReplyQ = new LinkedBlockingDeque<String>();
    public BlockingQueue<String> cmdSendQ = new LinkedBlockingDeque<String>();
    private Thread TprocessLineThread;
    private JPopupMenu popupMenu;
    private JMenuItem menuPopCopy;
    private JMenuItem menuPopPaste;
    protected final Lock swingSyncMutex = new ReentrantLock(true);
    private ActionListener fTerminalUpdater;
    private Timer fTimer;
    private ArrayList<String> internalTextArea = new ArrayList();
    private boolean internalTextAreaUpdated = false;
    public String terminalTitle = "Quarch Instrument Server " + AppVersion.getQualifiedAppVersion();
    private final int terminalUpdatePeriod = 25;
    private final int caretBlinkPeriod = 20;
    private boolean caretShouldBeVisible = false;
    private int caretBlinkCount = 0;
    private boolean replyThreadRunning;
    private static final int terminalWindowRows = 1024;
    private boolean replyStrLineAdded;
    private boolean replyStrNeedCursor;

    public TerminalWindow(String lookAndFeel) {
        this.addWindowFocusListener(new WindowFocusListener(){

            @Override
            public void windowGainedFocus(WindowEvent arg0) {
            }

            @Override
            public void windowLostFocus(WindowEvent arg0) {
            }
        });
        ArrayList<BufferedImage> icons = new ArrayList<BufferedImage>();
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            BufferedImage image = Globals.applicationIcons.getAsBufferedImage(0);
            icons.add(image);
            image = Globals.applicationIcons.getAsBufferedImage(1);
            icons.add(image);
            image = Globals.applicationIcons.getAsBufferedImage(2);
            icons.add(image);
            image = Globals.applicationIcons.getAsBufferedImage(3);
            icons.add(image);
            image = Globals.applicationIcons.getAsBufferedImage(4);
            icons.add(image);
        }
        catch (Exception toolkit) {
            // empty catch block
        }
        double scaleFactor = (double)Toolkit.getDefaultToolkit().getScreenResolution() / 98.0;
        Font scrollPaneFont = new Font("Monospaced", 0, (int)(11.0 * scaleFactor));
        Font terminalWindowFont = new Font("Monospaced", 0, (int)(12.0 * scaleFactor));
        this.setIconImages(icons);
        this.setPreferredSize(new Dimension(360, 240));
        this.setFocusTraversalKeysEnabled(false);
        this.getContentPane().setFocusTraversalKeysEnabled(false);
        this.setMinimumSize(new Dimension(624, 451));
        this.setSize((int)((double)this.getWidth() * scaleFactor), (int)((double)this.getHeight() * scaleFactor));
        this.setDefaultCloseOperation(0);
        this.setTitle(this.terminalTitle);
        this.getContentPane().setBackground(Color.WHITE);
        this.getContentPane().setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{624, 0};
        gridBagLayout.rowHeights = new int[]{450, 0};
        gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0};
        this.getContentPane().setLayout(gridBagLayout);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setFocusTraversalKeysEnabled(false);
        scrollPane.setFont(scrollPaneFont);
        scrollPane.setAutoscrolls(true);
        scrollPane.setAlignmentY(0.0f);
        scrollPane.setAlignmentX(0.0f);
        scrollPane.setViewportBorder(new TitledBorder(null, "", 4, 2, null, null));
        scrollPane.setVerticalScrollBarPolicy(22);
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.insets = new Insets(3, 0, 5, 0);
        gbc_scrollPane.weighty = 1.0;
        gbc_scrollPane.weightx = 1.0;
        gbc_scrollPane.anchor = 18;
        gbc_scrollPane.fill = 1;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 0;
        this.getContentPane().add((Component)scrollPane, gbc_scrollPane);
        this.terminalWindow = new JTextArea();
        this.terminalWindow.setFont(terminalWindowFont);
        this.terminalWindow.addFocusListener(new FocusAdapter(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void focusGained(FocusEvent arg0) {
                boolean syncLock = false;
                try {
                    syncLock = TerminalWindow.this.swingSyncMutex.tryLock(50L, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    if (syncLock) {
                        TerminalWindow.this.internalTextAreaUpdated = true;
                        TerminalWindow.this.swingSyncMutex.unlock();
                    }
                }
            }
        });
        this.terminalWindow.setDoubleBuffered(true);
        this.terminalWindow.setFocusTraversalKeysEnabled(false);
        this.terminalWindow.setAlignmentY(0.0f);
        this.terminalWindow.setAlignmentX(0.0f);
        Action action = this.terminalWindow.getActionMap().get("beep");
        action.setEnabled(false);
        this.terminalWindow.addKeyListener(new KeyAdapter(){

            @Override
            public void keyTyped(KeyEvent arg0) {
                if (!arg0.isControlDown()) {
                    if (!TerminalWindow.this.cmdInFlight) {
                        char c = arg0.getKeyChar();
                        TerminalWindow.this.processChar(c);
                    }
                    arg0.consume();
                } else {
                    TerminalWindow.this.processMenuKey(arg0.getKeyChar(), arg0.getKeyLocation(), arg0.getModifiers());
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (TerminalWindow.this.keyActioned == -1 && e.getModifiersEx() == 128) {
                    TerminalWindow.this.keyActioned = TerminalWindow.this.processMenuKey(e.getKeyChar(), e.getKeyCode(), e.getModifiersEx());
                }
                if (e.getKeyChar() == '\b' || e.getKeyChar() == '\t') {
                    e.consume();
                }
                int keyCode = e.getKeyCode();
                switch (keyCode) {
                    case 112: {
                        TerminalWindow.this.processString("write 0xa005 0x0001\n");
                        e.consume();
                        break;
                    }
                    case 113: {
                        TerminalWindow.this.processString("write 0xa005 0x0000\n");
                        e.consume();
                        break;
                    }
                    case 114: {
                        try {
                            TerminalWindow.this.processString("write 0xa005 0x0001\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0000\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0001\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0000\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0001\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0000\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0001\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0000\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0001\n");
                            Thread.sleep(1000L);
                            TerminalWindow.this.processString("write 0xa005 0x0000\n");
                        }
                        catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        e.consume();
                        break;
                    }
                    case 38: {
                        TerminalWindow.this.processUpArrow();
                        e.consume();
                        break;
                    }
                    case 40: {
                        TerminalWindow.this.processDownArrow();
                        e.consume();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == TerminalWindow.this.keyActioned) {
                    TerminalWindow.this.keyActioned = -1;
                }
            }
        });
        this.terminalWindow.setRows(1024);
        this.terminalWindow.append(">");
        this.terminalWindow.setPreferredSize(new Dimension(50, 50));
        this.terminalWindow.setEditable(false);
        this.terminalWindow.setColumns(80);
        scrollPane.setViewportView(this.terminalWindow);
        this.popupMenu = new JPopupMenu();
        TerminalWindow.addPopup(this.terminalWindow, this.popupMenu);
        this.menuPopCopy = new JMenuItem("Copy");
        this.menuPopCopy.addActionListener(this);
        this.menuPopCopy.setMnemonic(65485);
        this.popupMenu.add(this.menuPopCopy);
        this.menuPopPaste = new JMenuItem("Paste");
        this.menuPopPaste.addActionListener(this);
        this.menuPopPaste.setMnemonic(65487);
        this.popupMenu.add(this.menuPopPaste);
        this.internalTextArea.add(">");
        this.menuBar_1 = new JMenuBar();
        this.setJMenuBar(this.menuBar_1);
        JMenu mnEdit = new JMenu("Edit");
        this.menuBar_1.add(mnEdit);
        this.mntmEditCopy = new JMenuItem("Copy");
        this.mntmEditCopy.setAccelerator(KeyStroke.getKeyStroke(67, 2));
        this.mntmEditCopy.setMnemonic(65485);
        mnEdit.add(this.mntmEditCopy);
        this.mntmEditPaste = new JMenuItem("Paste");
        this.mntmEditPaste.setAccelerator(KeyStroke.getKeyStroke(86, 2));
        this.mntmEditPaste.setMnemonic(65487);
        mnEdit.add(this.mntmEditPaste);
        this.chckbxmntmDebug = new JCheckBoxMenuItem("debug");
        this.chckbxmntmDebug.setSelected(DebugUtil.isEnableDebug());
        this.chckbxmntmDebug.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                DebugUtil.setEnableDebug(TerminalWindow.this.chckbxmntmDebug.isSelected());
            }
        });
        mnEdit.add(this.chckbxmntmDebug);
        this.mntmEditCopy.addActionListener(this);
        this.mntmEditPaste.addActionListener(this);
        this.setEnables(true);
        new Thread(this).start();
        this.startTerminalUpdaterTimer();
        this.TprocessLineThread = new processLineThread();
        this.TprocessLineThread.start();
    }

    private void processString(String str) {
        for (char c : str.toCharArray()) {
            this.processChar(c);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processChar(char c) {
        boolean syncLock = false;
        this.swingSyncMutex.lock();
        syncLock = true;
        if (syncLock) {
            try {
                if (c == '\b') {
                    int lastLine = this.internalTextArea.size() - 1;
                    String lastStr = this.internalTextArea.get(lastLine);
                    if (lastStr.length() > 1) {
                        lastStr = lastStr.substring(0, lastStr.length() - 1);
                        this.internalTextArea.set(lastLine, lastStr);
                    }
                } else if (c == '\t') {
                    if (this.commandHistory.hasCommandHistory()) {
                        int lastLine = this.internalTextArea.size() - 1;
                        String command = this.commandHistory.lastCommand();
                        this.internalTextArea.set(lastLine, ">" + command);
                    }
                } else {
                    int lastLine = this.internalTextArea.size() - 1;
                    if (c == '\n') {
                        lastLine = this.internalTextArea.size() - 1;
                        String cmd = this.internalTextArea.get(lastLine);
                        this.swingSyncMutex.unlock();
                        syncLock = false;
                        cmd = cmd.replaceAll("(\\r|\\n)", "");
                        if (cmd.startsWith(">")) {
                            cmd = cmd.substring(1, cmd.length());
                        }
                        if (!cmd.isEmpty()) {
                            this.commandHistory.moveDown();
                        }
                        this.sendCommand(new String(cmd + "\r\n"));
                    } else {
                        this.internalTextArea.set(lastLine, this.internalTextArea.get(lastLine) + c);
                    }
                }
                this.internalTextAreaUpdated = true;
            }
            catch (Exception exception) {
            }
            finally {
                if (syncLock) {
                    this.swingSyncMutex.unlock();
                }
            }
        }
    }

    private void processUpArrow() {
        this.swingSyncMutex.lock();
        if (this.commandHistory.hasCommandHistory()) {
            int lastLine = this.internalTextArea.size() - 1;
            this.commandHistory.moveUp();
            String command = this.commandHistory.getPointerCommand();
            this.internalTextArea.set(lastLine, ">" + command);
            this.internalTextAreaUpdated = true;
        }
        this.swingSyncMutex.unlock();
    }

    private void processDownArrow() {
        this.swingSyncMutex.lock();
        if (this.commandHistory.hasCommandHistory()) {
            int lastLine = this.internalTextArea.size() - 1;
            this.commandHistory.moveDown();
            String command = this.commandHistory.getPointerCommand();
            this.internalTextArea.set(lastLine, ">" + command);
            this.internalTextAreaUpdated = true;
        }
        this.swingSyncMutex.unlock();
    }

    /*
     * Loose catch block
     */
    private void updateTerminalDisplay() {
        block18: {
            boolean syncLock;
            block19: {
                syncLock = false;
                syncLock = this.swingSyncMutex.tryLock(2L, TimeUnit.MILLISECONDS);
                if (!syncLock) break block18;
                if (!this.internalTextAreaUpdated) break block19;
                try {
                    this.terminalWindow.setText("");
                    for (int i = 0; i <= this.internalTextArea.size() - 2; ++i) {
                        this.terminalWindow.append(this.internalTextArea.get(i) + "\n");
                    }
                    this.terminalWindow.append(this.internalTextArea.get(this.internalTextArea.size() - 1));
                    this.terminalWindow.setCaretPosition(this.terminalWindow.getText().length());
                }
                catch (Exception i) {
                    // empty catch block
                }
            }
            if (++this.caretBlinkCount > 20) {
                this.caretBlinkCount = 0;
                this.terminalWindow.getCaret().setVisible(this.caretShouldBeVisible);
                this.caretShouldBeVisible = !this.caretShouldBeVisible;
            }
            this.internalTextAreaUpdated = false;
            this.swingSyncMutex.unlock();
            break block18;
            catch (InterruptedException e) {
                block20: {
                    e.printStackTrace();
                    if (!syncLock) break block18;
                    if (!this.internalTextAreaUpdated) break block20;
                    try {
                        this.terminalWindow.setText("");
                        for (int i = 0; i <= this.internalTextArea.size() - 2; ++i) {
                            this.terminalWindow.append(this.internalTextArea.get(i) + "\n");
                        }
                        this.terminalWindow.append(this.internalTextArea.get(this.internalTextArea.size() - 1));
                        this.terminalWindow.setCaretPosition(this.terminalWindow.getText().length());
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (++this.caretBlinkCount > 20) {
                    this.caretBlinkCount = 0;
                    this.terminalWindow.getCaret().setVisible(this.caretShouldBeVisible);
                    this.caretShouldBeVisible = !this.caretShouldBeVisible;
                }
                this.internalTextAreaUpdated = false;
                this.swingSyncMutex.unlock();
                catch (Throwable throwable) {
                    if (syncLock) {
                        if (this.internalTextAreaUpdated) {
                            try {
                                this.terminalWindow.setText("");
                                for (int i = 0; i <= this.internalTextArea.size() - 2; ++i) {
                                    this.terminalWindow.append(this.internalTextArea.get(i) + "\n");
                                }
                                this.terminalWindow.append(this.internalTextArea.get(this.internalTextArea.size() - 1));
                                this.terminalWindow.setCaretPosition(this.terminalWindow.getText().length());
                            }
                            catch (Exception exception) {
                                // empty catch block
                            }
                        }
                        if (++this.caretBlinkCount > 20) {
                            this.caretBlinkCount = 0;
                            this.terminalWindow.getCaret().setVisible(this.caretShouldBeVisible);
                            this.caretShouldBeVisible = !this.caretShouldBeVisible;
                        }
                        this.internalTextAreaUpdated = false;
                        this.swingSyncMutex.unlock();
                    }
                    throw throwable;
                }
            }
        }
    }

    private void startTerminalUpdaterTimer() {
        this.fTerminalUpdater = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt) {
                TerminalWindow.this.updateTerminalDisplay();
            }
        };
        this.fTimer = new Timer(25, this.fTerminalUpdater);
        this.fTimer.start();
    }

    private void stopTerminalUpdaterTimer() {
        this.fTimer.stop();
        this.fTimer.removeActionListener(this.fTerminalUpdater);
        this.fTerminalUpdater = null;
        this.fTimer = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "Paste") {
            this.processMenuKey('\u0000', this.mntmEditPaste.getAccelerator().getKeyCode(), 128);
        }
        if (e.getActionCommand() == "Copy") {
            this.processMenuKey('\u0000', this.mntmEditCopy.getAccelerator().getKeyCode(), 128);
        }
    }

    public void itemStateChanged(ItemEvent e) {
    }

    public int processMenuKey(char keyChar, int keyCode, int modifier) {
        if (modifier == 128 && keyCode == this.mntmEditCopy.getAccelerator().getKeyCode()) {
            this.textTransfer.setClipboardContents(this.terminalWindow.getSelectedText());
            return keyCode;
        }
        if (modifier == 128 && keyCode == this.mntmEditPaste.getAccelerator().getKeyCode()) {
            String[] lines;
            String clipStr = this.textTransfer.getClipboardContents();
            boolean lineStart = false;
            boolean done = false;
            int cr = StringUtils.countMatches((CharSequence)clipStr, (CharSequence)"\r");
            int ln = StringUtils.countMatches((CharSequence)clipStr, (CharSequence)"\n");
            int terminatorCount = Math.max(cr, ln);
            for (String s : lines = clipStr.split("\\r?\\n")) {
                if (terminatorCount-- > 0) {
                    this.pasteCommand(s + "\n");
                } else {
                    this.pasteCommand(s);
                }
                try {
                    Thread.sleep(20L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return keyCode;
        }
        return -1;
    }

    private void pasteCommand(String clipLine) {
        for (int i = 0; i < clipLine.length(); ++i) {
            this.processChar(clipLine.charAt(i));
        }
    }

    private void sendCommand(String clipLine) {
        try {
            this.cmdSendQ.put(new String(clipLine));
            if (!clipLine.startsWith("\r\n")) {
                this.commandHistory.addCommand(new String(clipLine.replaceAll("(\\r|\\n)", "")));
            }
        }
        catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private boolean isSpecialCmd(String cmd) {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        boolean done = false;
        String s = "";
        this.replyStrNeedCursor = false;
        this.replyStrLineAdded = false;
        this.replyThreadRunning = true;
        try {
            while (!done) {
                s = this.cmdReplyQ.take();
                if (s != null) {
                    boolean syncLock = false;
                    this.swingSyncMutex.lock();
                    syncLock = true;
                    if (syncLock) {
                        this.printReplyStr(s.replaceAll("(\\r|\\n)", ""));
                        if (this.replyStrLineAdded) {
                            this.replyStrLineAdded = false;
                            while (this.internalTextArea.size() >= 1024) {
                                this.internalTextArea.remove(0);
                            }
                        }
                    }
                    this.internalTextAreaUpdated = true;
                    this.swingSyncMutex.unlock();
                    continue;
                }
                Thread.sleep(200L);
            }
        }
        catch (Exception e) {
            done = true;
        }
        finally {
            this.replyThreadRunning = false;
        }
    }

    private void printReplyStr(String s) {
        if (this.replyStrNeedCursor) {
            this.replyStrNeedCursor = false;
            this.internalTextArea.set(this.internalTextArea.size() - 1, ">" + s);
        } else {
            this.internalTextArea.add(s);
        }
        this.replyStrLineAdded = true;
    }

    public void printStr(String s) {
        this.printReplyStr(s);
        this.internalTextAreaUpdated = true;
    }

    private static void addPopup(Component component, final JPopupMenu popup) {
        component.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    this.showMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    this.showMenu(e);
                }
            }

            private void showMenu(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void setEnables(boolean enable) {
        this.terminalWindow.setEnabled(enable);
        this.menuBar_1.setEnabled(enable);
        this.popupMenu.setEnabled(enable);
        this.mntmEditCopy.setEnabled(enable);
        this.mntmEditPaste.setEnabled(enable);
        this.menuPopCopy.setEnabled(enable);
        this.menuPopPaste.setEnabled(enable);
    }

    public class processLineThread
    extends Thread {
        @Override
        public void run() {
        }
    }

    public final class TextTransfer
    implements ClipboardOwner {
        @Override
        public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        }

        public void setClipboardContents(String aString) {
            StringSelection stringSelection = new StringSelection(aString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }

        public String getClipboardContents() {
            boolean hasTransferableText;
            String result = "";
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            boolean bl = hasTransferableText = contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    result = (String)contents.getTransferData(DataFlavor.stringFlavor);
                }
                catch (UnsupportedFlavorException | IOException ex) {
                    ex.printStackTrace();
                }
            }
            return result;
        }
    }
}

