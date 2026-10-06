package ui;

import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Custom rounded border that dynamically changes to an accent highlight color when the component is focused.
 */
public class FocusAwareBorder implements Border {

    private final int radius;
    private final Insets insets;
    private final Color normalColor;
    private final Color focusColor;

    public FocusAwareBorder(int radius, Insets insets, Color normalColor, Color focusColor) {
        this.radius = radius;
        this.insets = insets != null ? insets : new Insets(8, 12, 8, 12);
        this.normalColor = normalColor != null ? normalColor : Theme.BORDER;
        this.focusColor = focusColor != null ? focusColor : Theme.BORDER_FOCUS;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean focused = c.hasFocus();
        g2.setColor(focused ? focusColor : normalColor);
        g2.setStroke(new BasicStroke(focused ? 1.8f : 1.0f));

        g2.drawRoundRect(x + 1, y + 1, width - 2, height - 2, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return (Insets) insets.clone();
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
