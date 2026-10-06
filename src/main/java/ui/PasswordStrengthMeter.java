package ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Visual Password Strength Meter with animated color-coded feedback (Weak, Medium, Strong).
 */
public class PasswordStrengthMeter extends JPanel {

    public enum Strength {
        NONE("No password", Theme.TEXT_MUTED, 0),
        WEAK("Weak (min 8 chars required)", Theme.ERROR, 1),
        MEDIUM("Medium (good mix)", Theme.WARNING, 2),
        STRONG("Strong (highly secure)", Theme.SUCCESS, 3);

        final String label;
        final Color color;
        final int level;

        Strength(String label, Color color, int level) {
            this.label = label;
            this.color = color;
            this.level = level;
        }
    }

    private Strength currentStrength = Strength.NONE;

    public PasswordStrengthMeter() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, 22));
        setMinimumSize(new Dimension(0, 20));
    }

    public void updatePassword(char[] password) {
        this.currentStrength = calculateStrength(password);
        repaint();
    }

    public static Strength calculateStrength(char[] password) {
        if (password == null || password.length == 0) {
            return Strength.NONE;
        }
        if (password.length < 8) {
            return Strength.WEAK;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        int score = 0;
        if (password.length >= 8) score++;
        if (password.length >= 12) score++;
        if (hasUpper && hasLower) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;

        if (score >= 4) {
            return Strength.STRONG;
        } else if (score >= 2) {
            return Strength.MEDIUM;
        } else {
            return Strength.WEAK;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int barTotalWidth = Math.min(220, width / 2);
        int barHeight = 5;
        int barY = (height - barHeight) / 2;

        int numSegments = 3;
        int gap = 4;
        int segWidth = (barTotalWidth - (gap * (numSegments - 1))) / numSegments;

        int activeLevel = currentStrength.level;

        // Draw 3 bar segments
        for (int i = 0; i < numSegments; i++) {
            int segX = i * (segWidth + gap);
            if (i < activeLevel) {
                g2.setColor(currentStrength.color);
            } else {
                g2.setColor(Theme.SURFACE_ALT);
            }
            g2.fillRoundRect(segX, barY, segWidth, barHeight, 4, 4);
        }

        // Draw label text beside the bars
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(currentStrength.color);
        FontMetrics fm = g2.getFontMetrics();

        int textX = barTotalWidth + 12;
        int textY = (height + fm.getAscent()) / 2 - 2;
        g2.drawString("Security: " + currentStrength.label, textX, textY);

        g2.dispose();
    }
}
