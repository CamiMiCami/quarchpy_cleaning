/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class GraphLED
extends JPanel {
    private static final long serialVersionUID = 1L;
    private Style style = Style.OVAL;
    private int xOffset = 0;
    private int yOffset = 0;
    private int width;
    private int height;
    private int widthLED;
    private int heightLED;
    private Color offColor = Color.RED;
    private Color onColor = new Color(0.0f, 0.9f, 0.0f);
    private boolean state;

    public int getWidthLED() {
        return this.widthLED;
    }

    public void setWidthLED(int widthLED) {
        this.widthLED = widthLED;
        this.xOffset = (this.width - this.widthLED) / 2;
    }

    public int getHeightLED() {
        return this.heightLED;
    }

    public void setHeightLED(int heightLED) {
        this.heightLED = heightLED;
        this.yOffset = (this.height - this.heightLED) / 2;
    }

    public GraphLED() {
        this.setIgnoreRepaint(true);
    }

    @Override
    public void setPreferredSize(Dimension d) {
        super.setPreferredSize(d);
        this.width = this.widthLED = d.width;
        this.height = this.heightLED = d.height;
        this.xOffset = 0;
        this.yOffset = 0;
    }

    public boolean isState() {
        return this.state;
    }

    public void setState(boolean state) {
        this.state = state;
        this.repaint();
    }

    public void setColors(Color off, Color on) {
        this.offColor = off;
        this.onColor = on;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        Color color = this.state ? this.onColor : this.offColor;
        g2d.setColor(color);
        switch (this.style) {
            case OVAL: {
                g2d.fillOval(this.xOffset, this.yOffset, this.widthLED, this.heightLED);
                g2d.setColor(color.darker());
                g2d.drawOval(this.xOffset, this.yOffset, this.widthLED, this.heightLED);
                g2d.setColor(Color.WHITE);
                g2d.fillOval(this.width / 3, this.height / 3, 3, 4);
                break;
            }
            case RECTANGLE: {
                g2d.fillRect(this.xOffset, this.yOffset, this.widthLED, this.heightLED);
                g2d.setColor(color.darker());
                g2d.drawRect(this.xOffset, this.yOffset, this.widthLED, this.heightLED);
            }
        }
    }

    public static enum Style {
        OVAL,
        RECTANGLE;

    }
}

