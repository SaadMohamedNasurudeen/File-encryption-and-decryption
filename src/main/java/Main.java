import ui.DashboardFrame;
import ui.Theme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application Entry Point.
 * Sets Cross-Platform (Metal) Look & Feel to avoid native Windows UXTheme color overrides,
 * then launches the Swing/AWT File Encryption & Decryption System dashboard.
 */
public class Main {
    public static void main(String[] args) {
        // Disable bold metal fonts so file names and labels render cleanly
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        // Set cross-platform Look and Feel before creating any components
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            // Global Dark Theme overrides for standard Swing components
            UIManager.put("Panel.background", Theme.BG_DARK);
            UIManager.put("Viewport.background", Theme.BG_DARK);
            UIManager.put("ScrollPane.background", Theme.BG_DARK);
            UIManager.put("TabbedPane.background", Theme.BG_DARK);
            UIManager.put("TabbedPane.foreground", Theme.TEXT_PRIMARY);
            UIManager.put("TabbedPane.contentBorderInsets", new java.awt.Insets(0, 0, 0, 0));
            UIManager.put("TabbedPane.tabsOverlapBorder", true);
            UIManager.put("Label.foreground", Theme.TEXT_PRIMARY);
            UIManager.put("OptionPane.background", Theme.SURFACE);
            UIManager.put("OptionPane.messageForeground", Theme.TEXT_PRIMARY);
        } catch (Exception ignored) {
            // Graceful fallback
        }

        // Launch GUI safely on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame();
            frame.setVisible(true);
        });
    }
}
