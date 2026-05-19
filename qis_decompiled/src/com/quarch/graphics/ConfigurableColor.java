/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.graphics;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JColorChooser;

public class ConfigurableColor {
    private Color color;
    private String name;
    private Component parent;

    public ConfigurableColor(Component parent, String name, Color color) {
        this.parent = parent;
        this.setName(name);
        this.setColor(color);
    }

    public void useSelector() {
        Color color = JColorChooser.showDialog(this.parent, "Color of " + this.name, this.getColor());
        if (color != null) {
            this.setColor(color);
        }
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

