package ui;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Custom TabbedPaneUI delivering dark tabs, an accent highlight underline for the active tab,
 * readable light text, and elimination of native/beveled white borders around the content area.
 */
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

    private final Color activeTabBg = Theme.SURFACE;
    private final Color inactiveTabBg = Theme.BG_DARK;
    private final Color activeIndicatorColor = Theme.ACCENT;

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(10, 20, 10, 20);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(4, 10, 0, 10);
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // Suppress default white/beveled borders around content area completely
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.BORDER);
        g2.drawLine(0, 0, tabPane.getWidth(), 0);
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        // Suppress default beveled borders
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        // Suppress default dotted focus rectangle for clean modern UI
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill tab background
        if (isSelected) {
            g2.setColor(activeTabBg);
            g2.fillRoundRect(x, y + 2, w, h - 2, 8, 8);

            // Draw colored underline / indicator at the bottom of the selected tab
            g2.setColor(activeIndicatorColor);
            g2.fillRect(x + 6, y + h - 3, w - 12, 3);
        } else {
            g2.setColor(inactiveTabBg);
            g2.fillRoundRect(x, y + 4, w, h - 4, 6, 6);
        }

        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                             int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(Theme.FONT_LABEL);
        g2.setColor(isSelected ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);

        int textX = textRect.x;
        int textY = textRect.y + metrics.getAscent();

        g2.drawString(title, textX, textY);
        g2.dispose();
    }
}
