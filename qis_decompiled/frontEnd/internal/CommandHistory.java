/*
 * Decompiled with CFR 0.152.
 */
package frontEnd.internal;

import java.util.ArrayList;
import java.util.List;

public class CommandHistory {
    private List<String> commands = new ArrayList<String>();
    private int commandPointer = 0;
    private boolean hasMaxNumberCommands;
    private int maxNumberCommands;

    public CommandHistory() {
        this.hasMaxNumberCommands = false;
        this.maxNumberCommands = -1;
    }

    public CommandHistory(int maxNumberCommands) {
        this.hasMaxNumberCommands = true;
        this.maxNumberCommands = maxNumberCommands;
    }

    public void clearCommandHistory() {
        this.commands.clear();
    }

    public int numberCommands() {
        return this.commands.size();
    }

    public boolean hasCommandHistory() {
        return !this.commands.isEmpty();
    }

    public String firstCommand() {
        if (this.commands.isEmpty()) {
            return null;
        }
        return this.commands.get(0);
    }

    public String lastCommand() {
        if (this.commands.isEmpty()) {
            return null;
        }
        return this.commands.get(this.commands.size() - 1);
    }

    public void moveUp() {
        --this.commandPointer;
        if (this.commandPointer < 0) {
            this.commandPointer = 0;
        }
    }

    public void moveDown() {
        ++this.commandPointer;
        if (this.commandPointer > this.commands.size() - 1) {
            this.commandPointer = this.commands.size() - 1;
        }
    }

    public String getPointerCommand() {
        if (this.commands.isEmpty()) {
            return null;
        }
        return this.commands.get(this.commandPointer);
    }

    public void addCommand(String command) {
        this.commands.add(command);
        if (this.hasMaxNumberCommands && this.commands.size() > this.maxNumberCommands) {
            this.commands.remove(0);
        }
        this.commandPointer = this.commands.size() - 1;
    }
}

