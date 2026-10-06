package ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Reusable helper that configures modern, styled JFileChooser dialogs
 * with Details view, dark theme styling, persistent directories, and a file preview accessory.
 */
public final class FileChooserHelper {

    // Remembers the last used directory across invocations
    private static File lastDirectory = new File(System.getProperty("user.home"));

    static {
        // Prevent bold metal fonts on file names
        UIManager.put("swing.boldMetal", Boolean.FALSE);
    }

    private FileChooserHelper() {}

    /**
     * Creates and configures a JFileChooser with:
     * - Preferred size: 850x550
     * - Details view action activated
     * - Remembers last directory
     * - Encrypt/Decrypt file filters
     * - Styled accessory preview panel
     * - Dark theme matching Theme.java
     *
     * @param dialogTitle Title for the chooser dialog
     * @param isEncrypt   True for Encrypt tab (filters out .enc), False for Decrypt tab (defaults to .enc)
     * @return Configured JFileChooser
     */
    public static JFileChooser createFileChooser(String dialogTitle, boolean isEncrypt) {
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle(dialogTitle);
        chooser.setPreferredSize(new Dimension(850, 550));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);

        // 1. Configure File Filters
        if (isEncrypt) {
            FileFilter nonEncFilter = new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    return !f.getName().toLowerCase().endsWith(".enc");
                }

                @Override
                public String getDescription() {
                    return "Files to Encrypt (excludes .enc)";
                }
            };
            chooser.addChoosableFileFilter(nonEncFilter);
            chooser.setFileFilter(nonEncFilter);
            chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
        } else {
            FileNameExtensionFilter encFilter = new FileNameExtensionFilter("Encrypted Files (*.enc)", "enc");
            chooser.addChoosableFileFilter(encFilter);
            chooser.setFileFilter(encFilter);
            chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
        }

        // 2. Add File Preview Accessory Panel
        FilePreviewAccessory accessory = new FilePreviewAccessory();
        chooser.setAccessory(accessory);
        chooser.addPropertyChangeListener(accessory);

        // 3. Enable Details View via "viewTypeDetails" action
        Action detailsAction = chooser.getActionMap().get("viewTypeDetails");
        if (detailsAction != null) {
            detailsAction.actionPerformed(new ActionEvent(chooser, ActionEvent.ACTION_PERFORMED, "viewTypeDetails"));
        }

        // 4. Apply Dark Theme styling across chooser component hierarchy
        styleComponentTree(chooser);

        return chooser;
    }

    /**
     * Updates the persistent last directory from the chooser's selected file or current directory.
     */
    public static void updateLastDirectory(JFileChooser chooser) {
        if (chooser != null) {
            File current = chooser.getCurrentDirectory();
            if (current != null && current.exists()) {
                lastDirectory = current;
            }
        }
    }

    /**
     * Recursively traverses and styles components inside the JFileChooser to match Theme.java.
     */
    public static void styleComponentTree(Component comp) {
        if (comp == null) return;

        if (comp instanceof JTable table) {
            table.setBackground(Theme.FIELD_BG);
            table.setForeground(Theme.TEXT_PRIMARY);
            table.setSelectionBackground(Theme.ACCENT);
            table.setSelectionForeground(Color.WHITE);
            table.setGridColor(Theme.BORDER);
            table.setFont(Theme.FONT_INPUT);
            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(Theme.SURFACE_ALT);
                table.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
                table.getTableHeader().setFont(Theme.FONT_LABEL);
            }
        } else if (comp instanceof JList<?> list) {
            list.setBackground(Theme.FIELD_BG);
            list.setForeground(Theme.TEXT_PRIMARY);
            list.setSelectionBackground(Theme.ACCENT);
            list.setSelectionForeground(Color.WHITE);
            list.setFont(Theme.FONT_INPUT);
        } else if (comp instanceof JTree tree) {
            tree.setBackground(Theme.FIELD_BG);
            tree.setForeground(Theme.TEXT_PRIMARY);
            tree.setFont(Theme.FONT_INPUT);
        } else if (comp instanceof JTextField textField) {
            textField.setBackground(Theme.FIELD_BG);
            textField.setForeground(Theme.TEXT_PRIMARY);
            textField.setCaretColor(Theme.TEXT_PRIMARY);
            textField.setFont(Theme.FONT_INPUT);
            textField.setBorder(new CompoundBorder(
                    new LineBorder(Theme.BORDER, 1),
                    new EmptyBorder(4, 8, 4, 8)
            ));
        } else if (comp instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(Theme.SURFACE_ALT);
            comboBox.setForeground(Theme.TEXT_PRIMARY);
            comboBox.setFont(Theme.FONT_INPUT);
        } else if (comp instanceof JButton button) {
            button.setBackground(Theme.SURFACE_ALT);
            button.setForeground(Theme.TEXT_PRIMARY);
            button.setFont(Theme.FONT_BUTTON);
            button.setFocusPainted(false);
            button.setBorder(new CompoundBorder(
                    new LineBorder(Theme.BORDER, 1),
                    new EmptyBorder(5, 12, 5, 12)
            ));
        } else if (comp instanceof JLabel label) {
            label.setForeground(Theme.TEXT_PRIMARY);
            label.setFont(Theme.FONT_LABEL);
        } else if (comp instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(Theme.SURFACE);
            scrollPane.getViewport().setBackground(Theme.FIELD_BG);
            scrollPane.setBorder(new LineBorder(Theme.BORDER, 1));
        } else if (comp instanceof JPanel panel) {
            panel.setBackground(Theme.SURFACE);
        }

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleComponentTree(child);
            }
        }
    }

    /**
     * Accessory panel that displays a rich preview of the currently selected file:
     * File Name, File Size, Last-Modified Date, and Format.
     */
    public static class FilePreviewAccessory extends JPanel implements PropertyChangeListener {

        private final JLabel nameValueLabel;
        private final JLabel sizeValueLabel;
        private final JLabel dateValueLabel;
        private final JLabel typeValueLabel;

        public FilePreviewAccessory() {
            setLayout(new BorderLayout(8, 8));
            setPreferredSize(new Dimension(230, 0));
            setBackground(Theme.SURFACE_ALT);
            setBorder(new CompoundBorder(
                    new LineBorder(Theme.BORDER, 1),
                    new EmptyBorder(14, 14, 14, 14)
            ));

            // Title
            JLabel headerLabel = new JLabel("File Preview");
            headerLabel.setFont(Theme.FONT_SECTION);
            headerLabel.setForeground(Theme.TEXT_PRIMARY);
            headerLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
            add(headerLabel, BorderLayout.NORTH);

            // Details panel
            JPanel detailsPanel = new JPanel(new GridLayout(8, 1, 0, 4));
            detailsPanel.setOpaque(false);

            JLabel nameTitle = createMetaTitle("File Name:");
            nameValueLabel = createMetaValue("No selection");

            JLabel sizeTitle = createMetaTitle("Size:");
            sizeValueLabel = createMetaValue("—");

            JLabel dateTitle = createMetaTitle("Last Modified:");
            dateValueLabel = createMetaValue("—");

            JLabel typeTitle = createMetaTitle("File Type:");
            typeValueLabel = createMetaValue("—");

            detailsPanel.add(nameTitle);
            detailsPanel.add(nameValueLabel);
            detailsPanel.add(sizeTitle);
            detailsPanel.add(sizeValueLabel);
            detailsPanel.add(dateTitle);
            detailsPanel.add(dateValueLabel);
            detailsPanel.add(typeTitle);
            detailsPanel.add(typeValueLabel);

            add(detailsPanel, BorderLayout.CENTER);
        }

        private JLabel createMetaTitle(String text) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.FONT_LABEL);
            label.setForeground(Theme.TEXT_MUTED);
            return label;
        }

        private JLabel createMetaValue(String text) {
            JLabel label = new JLabel(text);
            label.setFont(Theme.FONT_SMALL);
            label.setForeground(Theme.TEXT_PRIMARY);
            return label;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
                File file = (File) evt.getNewValue();
                updatePreview(file);
            }
        }

        private void updatePreview(File file) {
            if (file != null && file.exists()) {
                nameValueLabel.setText(truncate(file.getName(), 24));
                nameValueLabel.setToolTipText(file.getName());

                if (file.isDirectory()) {
                    sizeValueLabel.setText("Directory");
                    typeValueLabel.setText("Folder");
                } else {
                    sizeValueLabel.setText(formatSize(file.length()));
                    String name = file.getName();
                    int dot = name.lastIndexOf('.');
                    String ext = (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1).toUpperCase() : "Unknown";
                    typeValueLabel.setText(ext + " File");
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateValueLabel.setText(sdf.format(new Date(file.lastModified())));
            } else {
                nameValueLabel.setText("No selection");
                nameValueLabel.setToolTipText(null);
                sizeValueLabel.setText("—");
                dateValueLabel.setText("—");
                typeValueLabel.setText("—");
            }
        }

        private String truncate(String str, int maxLen) {
            if (str == null || str.length() <= maxLen) return str;
            return str.substring(0, maxLen - 3) + "...";
        }

        private String formatSize(long bytes) {
            if (bytes <= 0) return "0 B";
            final String[] units = {"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            DecimalFormat df = new DecimalFormat("#,##0.##");
            return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        }
    }
}
