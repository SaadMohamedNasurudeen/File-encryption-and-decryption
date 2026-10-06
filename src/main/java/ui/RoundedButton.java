package ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom modern rounded button with smooth hover, pressed, and disabled states.
 * Uses Graphics2D antialiasing and bypasses native Look & Feel rendering issues.
 */
public class RoundedButton extends JButton {

    private Color normalColor;
    private Color hoverColor;
    private Color pressedColor;
    private Color disabledColor;
    private Color borderColor;
    private int cornerRadius = 10;

    private boolean isHovered = false;
    private boolean isPressed = false;

    public RoundedButton(String text) {
        this(text, Theme.ACCENT, Theme.ACCENT_HOVER, Theme.ACCENT_PRESSED);
    }

    public RoundedButton(String text, Color normalColor, Color hoverColor) {
        this(text, normalColor, hoverColor, normalColor.darker());
    }

    public RoundedButton(String text, Color normalColor, Color hoverColor, Color pressedColor) {
        super(text);
        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;
        this.disabledColor = Theme.SURFACE_ALT;
        this.borderColor = null;

        initButton();
    }

    private void initButton() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(Theme.FONT_BUTTON);
        setForeground(Theme.TEXT_PRIMARY);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setColors(Color normal, Color hover, Color pressed) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.pressedColor = pressed;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Determine background fill color
        Color bgColor;
        if (!isEnabled()) {
            bgColor = disabledColor;
        } else if (isPressed) {
            bgColor = pressedColor;
        } else if (isHovered) {
            bgColor = hoverColor;
        } else {
            bgColor = normalColor;
        }

        // 2. Draw rounded background
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // 3. Draw border if configured
        if (borderColor != null && isEnabled()) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        // 4. Draw text centered
        Color textColor = isEnabled() ? getForeground() : Theme.TEXT_DISABLED;
        g2.setColor(textColor);
        g2.setFont(getFont());

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getAscent();
        int x = (width - textWidth) / 2;
        int y = (height + textHeight) / 2 - 2;

        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}
