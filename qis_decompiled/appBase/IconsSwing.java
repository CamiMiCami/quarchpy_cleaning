/*
 * Decompiled with CFR 0.152.
 */
package appBase;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public class IconsSwing {
    private static List<ImageIcon> appIcons = null;

    public IconsSwing() {
        this.loadAppIcons();
    }

    protected List<ImageIcon> loadAppIcons() {
        if (appIcons == null) {
            appIcons = new ArrayList<ImageIcon>();
            appIcons.add(this.createImageIcon("appBase/icons/QIS_16x16.png", ""));
            appIcons.add(this.createImageIcon("appBase/icons/QIS_32x32.png", ""));
            appIcons.add(this.createImageIcon("appBase/icons/QIS_64x64.png", ""));
            appIcons.add(this.createImageIcon("appBase/icons/QIS_128x128.png", ""));
            appIcons.add(this.createImageIcon("appBase/icons/QIS_256x256.png", ""));
        }
        return appIcons;
    }

    protected ImageIcon createImageIcon(String path, String description) {
        URL imgURL = this.getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        }
        System.err.println("Couldn't find file: " + path);
        return null;
    }

    public BufferedImage getAsBufferedImage(int idx) {
        ImageIcon icon = appIcons.get(idx);
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), 6);
        Graphics2D g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bi;
    }
}

