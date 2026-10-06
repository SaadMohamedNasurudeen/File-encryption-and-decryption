package ui;

import crypto.CryptoService;
import crypto.ProgressListener;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Modern, polished Swing/AWT Dashboard for the File Encryption and Decryption System.
 * Fully customized UI components, dark theme, drag-and-drop zones, and real-time statistics.
 */
public class DashboardFrame extends JFrame {

    private final CryptoService cryptoService;

    // --- Header Statistics ---
    private JLabel statEncryptedVal;
    private JLabel statDecryptedVal;
    private JLabel statFailuresVal;
    private int countEncrypted = 0;
    private int countDecrypted = 0;
    private int countFailures = 0;

    // --- Encrypt Tab Components ---
    private DropZonePanel encryptDropZone;
    private JTextField encryptFilePathField;
    private RoundedButton encryptBrowseBtn;
    private JPasswordField encryptPasswordField;
    private JPasswordField encryptConfirmPasswordField;
    private PasswordStrengthMeter passwordStrengthMeter;
    private JCheckBox encryptShowPasswordBox;
    private RoundedButton encryptActionBtn;
    private JLabel encryptFileNameVal;
    private JLabel encryptFileSizeVal;
    private JLabel encryptTargetVal;

    // --- Decrypt Tab Components ---
    private DropZonePanel decryptDropZone;
    private JTextField decryptFilePathField;
    private RoundedButton decryptBrowseBtn;
    private JPasswordField decryptPasswordField;
    private JCheckBox decryptShowPasswordBox;
    private RoundedButton decryptActionBtn;
    private JLabel decryptFileNameVal;
    private JLabel decryptFileSizeVal;
    private JLabel decryptTargetVal;

    // --- Bottom Status & Progress ---
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // --- History Tab Components ---
    private DefaultTableModel historyTableModel;
    private JTable historyTable;
    private RoundedButton clearHistoryBtn;
    private RoundedButton openFolderBtn;

    // State
    private File selectedEncryptFile;
    private File selectedDecryptFile;
    private File lastProcessedFile;
    private boolean isOperationRunning = false;

    public DashboardFrame() {
        super("File Encryption & Decryption System — AES-256 GCM");
        this.cryptoService = new CryptoService();

        initWindow();
        initUI();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 680));
        setPreferredSize(new Dimension(960, 740));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // 1. Top Header with App Branding and Live Stat Badges
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Custom Dark Tabbed Pane
        JTabbedPane tabbedPane = createCustomTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Progress Bar and Color-Coded Status
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    /**
     * Top header banner displaying title, crypto specs, and dynamic stat cards.
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(16, 8));
        headerPanel.setBackground(Theme.SURFACE);
        headerPanel.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Left Branding
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        leftPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("File Encryption & Decryption System");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);

        JLabel subLabel = new JLabel("AES-256 GCM (128-bit tag)  |  PBKDF2-HMAC-SHA256 (600,000 rounds)  |  JDK 17+");
        subLabel.setFont(Theme.FONT_SUBTITLE);
        subLabel.setForeground(Theme.TEXT_MUTED);

        leftPanel.add(titleLabel);
        leftPanel.add(subLabel);
        headerPanel.add(leftPanel, BorderLayout.WEST);

        // Right Stat Cards Panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statsPanel.setOpaque(false);

        statEncryptedVal = new JLabel("0");
        statDecryptedVal = new JLabel("0");
        statFailuresVal = new JLabel("0");

        statsPanel.add(createStatCard("Encrypted", statEncryptedVal, Theme.ACCENT_LIGHT));
        statsPanel.add(createStatCard("Decrypted", statDecryptedVal, Theme.SUCCESS));
        statsPanel.add(createStatCard("Failures", statFailuresVal, Theme.ERROR));

        headerPanel.add(statsPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(Theme.SURFACE_ALT);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));

        valueLabel.setFont(Theme.FONT_BUTTON);
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SMALL);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(valueLabel);
        card.add(titleLabel);
        return card;
    }

    /**
     * Creates the custom tabbed pane with dark theme and no white borders.
     */
    private JTabbedPane createCustomTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new CustomTabbedPaneUI());
        tabbedPane.setBackground(Theme.BG_DARK);
        tabbedPane.setForeground(Theme.TEXT_PRIMARY);

        tabbedPane.addTab("  Encrypt File  ", wrapInScrollableContainer(createEncryptTab()));
        tabbedPane.addTab("  Decrypt File  ", wrapInScrollableContainer(createDecryptTab()));
        tabbedPane.addTab("  Activity History  ", createHistoryTab());

        return tabbedPane;
    }

    /**
     * Wraps a tab's form in a centered container with sensible max width to prevent layout breaking on resize.
     */
    private JComponent wrapInScrollableContainer(JPanel content) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BG_DARK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(16, 24, 16, 24);

        // Constrain preferred width for aesthetics
        content.setMaximumSize(new Dimension(880, Integer.MAX_VALUE));
        wrapper.add(content, gbc);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BG_DARK);
        return scrollPane;
    }

    /**
     * Constructs the Encrypt tab panel with Drag-and-Drop, password strength meter, and file info.
     */
    private JPanel createEncryptTab() {
        JPanel formCard = createFormCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = createGbc();

        // 1. Drag & Drop File Zone
        gbc.gridy = 0;
        encryptDropZone = new DropZonePanel("Drop any file here to encrypt, or click to browse");
        encryptDropZone.setOnFileSelected(this::setEncryptFile);
        encryptDropZone.setOnBrowseAction(this::chooseEncryptFile);
        formCard.add(encryptDropZone, gbc);

        // 2. File Path Text Field + Browse Button
        gbc.gridy = 1;
        formCard.add(createFieldLabel("Selected File Path:"), gbc);

        gbc.gridy = 2;
        JPanel fileChoosePanel = new JPanel(new BorderLayout(8, 0));
        fileChoosePanel.setOpaque(false);

        encryptFilePathField = createStyledTextField("Select a file or drag & drop here...");
        encryptFilePathField.setEditable(false);
        attachFileDropTarget(encryptFilePathField, this::setEncryptFile);
        encryptBrowseBtn = new RoundedButton("Browse...", Theme.SURFACE_ALT, Theme.SURFACE_HOVER);
        encryptBrowseBtn.setPreferredSize(new Dimension(105, 38));
        encryptBrowseBtn.addActionListener(e -> chooseEncryptFile());

        fileChoosePanel.add(encryptFilePathField, BorderLayout.CENTER);
        fileChoosePanel.add(encryptBrowseBtn, BorderLayout.EAST);
        formCard.add(fileChoosePanel, gbc);

        // 3. Info Panel Card (File Name, Size, Target Format)
        gbc.gridy = 3;
        JPanel infoPanel = createFileInfoPanel(true);
        formCard.add(infoPanel, gbc);

        // 4. Password Field
        gbc.gridy = 4;
        formCard.add(createFieldLabel("Encryption Password (min 8 characters):"), gbc);

        gbc.gridy = 5;
        encryptPasswordField = createStyledPasswordField();
        formCard.add(encryptPasswordField, gbc);

        // 5. Password Strength Meter
        gbc.gridy = 6;
        passwordStrengthMeter = new PasswordStrengthMeter();
        formCard.add(passwordStrengthMeter, gbc);

        // DocumentListener to update strength meter on keystrokes
        encryptPasswordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateMeter(); }
            @Override public void removeUpdate(DocumentEvent e) { updateMeter(); }
            @Override public void changedUpdate(DocumentEvent e) { updateMeter(); }
            private void updateMeter() {
                char[] pw = encryptPasswordField.getPassword();
                passwordStrengthMeter.updatePassword(pw);
                CryptoService.zeroArray(pw);
            }
        });

        // 6. Confirm Password Field
        gbc.gridy = 7;
        formCard.add(createFieldLabel("Confirm Encryption Password:"), gbc);

        gbc.gridy = 8;
        encryptConfirmPasswordField = createStyledPasswordField();
        formCard.add(encryptConfirmPasswordField, gbc);

        // 7. Show Passwords Checkbox
        gbc.gridy = 9;
        encryptShowPasswordBox = createStyledCheckBox("Show Passwords");
        encryptShowPasswordBox.addItemListener(e -> {
            char echoChar = (e.getStateChange() == ItemEvent.SELECTED) ? (char) 0 : '\u2022';
            encryptPasswordField.setEchoChar(echoChar);
            encryptConfirmPasswordField.setEchoChar(echoChar);
        });
        formCard.add(encryptShowPasswordBox, gbc);

        // 8. Encrypt Action Button
        gbc.gridy = 10;
        gbc.insets = new Insets(16, 0, 6, 0);
        encryptActionBtn = new RoundedButton("Encrypt and Protect File (.enc)", Theme.ACCENT, Theme.ACCENT_HOVER, Theme.ACCENT_PRESSED);
        encryptActionBtn.setPreferredSize(new Dimension(300, 44));
        encryptActionBtn.addActionListener(e -> executeEncryption());
        formCard.add(encryptActionBtn, gbc);

        return formCard;
    }

    /**
     * Constructs the Decrypt tab panel with Drag-and-Drop and file info.
     */
    private JPanel createDecryptTab() {
        JPanel formCard = createFormCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = createGbc();

        // 1. Drag & Drop File Zone
        gbc.gridy = 0;
        decryptDropZone = new DropZonePanel("Drop an encrypted .enc file here, or click to browse");
        decryptDropZone.setOnFileSelected(this::setDecryptFile);
        decryptDropZone.setOnBrowseAction(this::chooseDecryptFile);
        formCard.add(decryptDropZone, gbc);

        // 2. File Path Text Field + Browse Button
        gbc.gridy = 1;
        formCard.add(createFieldLabel("Selected Encrypted File (.enc):"), gbc);

        gbc.gridy = 2;
        JPanel fileChoosePanel = new JPanel(new BorderLayout(8, 0));
        fileChoosePanel.setOpaque(false);

        decryptFilePathField = createStyledTextField("Select an encrypted (.enc) file or drag & drop here...");
        decryptFilePathField.setEditable(false);
        attachFileDropTarget(decryptFilePathField, this::setDecryptFile);
        decryptBrowseBtn = new RoundedButton("Browse...", Theme.SURFACE_ALT, Theme.SURFACE_HOVER);
        decryptBrowseBtn.setPreferredSize(new Dimension(105, 38));
        decryptBrowseBtn.addActionListener(e -> chooseDecryptFile());

        fileChoosePanel.add(decryptFilePathField, BorderLayout.CENTER);
        fileChoosePanel.add(decryptBrowseBtn, BorderLayout.EAST);
        formCard.add(fileChoosePanel, gbc);

        // 3. Info Panel Card
        gbc.gridy = 3;
        JPanel infoPanel = createFileInfoPanel(false);
        formCard.add(infoPanel, gbc);

        // 4. Password Field
        gbc.gridy = 4;
        formCard.add(createFieldLabel("Decryption Password:"), gbc);

        gbc.gridy = 5;
        decryptPasswordField = createStyledPasswordField();
        formCard.add(decryptPasswordField, gbc);

        // 5. Show Password Checkbox
        gbc.gridy = 6;
        decryptShowPasswordBox = createStyledCheckBox("Show Password");
        decryptShowPasswordBox.addItemListener(e -> {
            char echoChar = (e.getStateChange() == ItemEvent.SELECTED) ? (char) 0 : '\u2022';
            decryptPasswordField.setEchoChar(echoChar);
        });
        formCard.add(decryptShowPasswordBox, gbc);

        // 6. Decrypt Action Button
        gbc.gridy = 7;
        gbc.insets = new Insets(16, 0, 6, 0);
        decryptActionBtn = new RoundedButton("Decrypt and Authenticate File", Theme.SUCCESS, Theme.SUCCESS_HOVER, Theme.SUCCESS_PRESSED);
        decryptActionBtn.setPreferredSize(new Dimension(300, 44));
        decryptActionBtn.addActionListener(e -> executeDecryption());
        formCard.add(decryptActionBtn, gbc);

        return formCard;
    }

    /**
     * File metadata info panel card showing Name, Size, and Format.
     */
    private JPanel createFileInfoPanel(boolean isEncrypt) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.SURFACE_ALT);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 8, 3, 8);

        JLabel nameTitle = new JLabel("File Name:");
        nameTitle.setFont(Theme.FONT_LABEL);
        nameTitle.setForeground(Theme.TEXT_MUTED);

        JLabel sizeTitle = new JLabel("File Size:");
        sizeTitle.setFont(Theme.FONT_LABEL);
        sizeTitle.setForeground(Theme.TEXT_MUTED);

        JLabel extTitle = new JLabel(isEncrypt ? "Target Output:" : "Restored Output:");
        extTitle.setFont(Theme.FONT_LABEL);
        extTitle.setForeground(Theme.TEXT_MUTED);

        JLabel nameVal = new JLabel("No file selected");
        nameVal.setFont(Theme.FONT_INPUT);
        nameVal.setForeground(Theme.TEXT_PRIMARY);

        JLabel sizeVal = new JLabel("—");
        sizeVal.setFont(Theme.FONT_INPUT);
        sizeVal.setForeground(Theme.TEXT_PRIMARY);

        JLabel extVal = new JLabel("—");
        extVal.setFont(Theme.FONT_INPUT);
        extVal.setForeground(Theme.TEXT_PRIMARY);

        if (isEncrypt) {
            encryptFileNameVal = nameVal;
            encryptFileSizeVal = sizeVal;
            encryptTargetVal = extVal;
        } else {
            decryptFileNameVal = nameVal;
            decryptFileSizeVal = sizeVal;
            decryptTargetVal = extVal;
        }

        g.gridx = 0; g.gridy = 0; g.weightx = 0.2; card.add(nameTitle, g);
        g.gridx = 1; g.gridy = 0; g.weightx = 0.8; card.add(nameVal, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0.2; card.add(sizeTitle, g);
        g.gridx = 1; g.gridy = 1; g.weightx = 0.8; card.add(sizeVal, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0.2; card.add(extTitle, g);
        g.gridx = 1; g.gridy = 2; g.weightx = 0.8; card.add(extVal, g);

        return card;
    }

    /**
     * Activity History tab panel with styled JTable and action controls.
     */
    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));

        String[] columns = {"#", "File Name", "Action", "Timestamp", "Result", "Path / Details"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setFont(Theme.FONT_INPUT);
        historyTable.setRowHeight(30);
        historyTable.setBackground(Theme.SURFACE);
        historyTable.setForeground(Theme.TEXT_PRIMARY);
        historyTable.setGridColor(Theme.BORDER);
        historyTable.setSelectionBackground(Theme.ACCENT);
        historyTable.setSelectionForeground(Color.WHITE);
        historyTable.setShowVerticalLines(false);

        // Header styling
        JTableHeader tableHeader = historyTable.getTableHeader();
        tableHeader.setFont(Theme.FONT_LABEL);
        tableHeader.setBackground(Theme.SURFACE_ALT);
        tableHeader.setForeground(Theme.TEXT_PRIMARY);
        tableHeader.setPreferredSize(new Dimension(0, 36));
        tableHeader.setBorder(new LineBorder(Theme.BORDER, 1));

        // Column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(145);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(300);

        // Custom Cell Renderer with alternating row colors and Success/Failed badges
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? Theme.SURFACE : new Color(36, 49, 70));
                }

                String valStr = (value != null) ? value.toString() : "";
                if (col == 4) { // Result column
                    setHorizontalAlignment(CENTER);
                    setFont(Theme.FONT_LABEL);
                    if ("Success".equalsIgnoreCase(valStr)) {
                        setForeground(Theme.SUCCESS);
                    } else if ("Failed".equalsIgnoreCase(valStr)) {
                        setForeground(Theme.ERROR);
                    } else {
                        setForeground(Theme.TEXT_PRIMARY);
                    }
                } else {
                    setHorizontalAlignment(LEFT);
                    setFont(Theme.FONT_INPUT);
                    setForeground(isSelected ? Color.WHITE : Theme.TEXT_PRIMARY);
                }
                return c;
            }
        };

        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        scrollPane.setBorder(new LineBorder(Theme.BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        // History Controls Bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        openFolderBtn = new RoundedButton("Open Selected File Location", Theme.SURFACE_ALT, Theme.SURFACE_HOVER);
        openFolderBtn.setPreferredSize(new Dimension(220, 36));
        openFolderBtn.addActionListener(e -> openSelectedFolder());

        clearHistoryBtn = new RoundedButton("Clear History", Theme.SURFACE_ALT, Theme.ERROR_HOVER);
        clearHistoryBtn.setPreferredSize(new Dimension(130, 36));
        clearHistoryBtn.addActionListener(e -> clearHistory());

        btnBar.add(openFolderBtn);
        btnBar.add(clearHistoryBtn);
        panel.add(btnBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Bottom status and custom styled progress bar area.
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(12, 24, 14, 24)
        ));

        // Custom Styled Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setFont(Theme.FONT_STATUS);
        progressBar.setPreferredSize(new Dimension(0, 24));
        progressBar.setUI(new BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = progressBar.getWidth();
                int h = progressBar.getHeight();

                // Track background
                g2.setColor(Theme.FIELD_BG);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Progress fill
                int amount = getAmountFull(new Insets(0, 0, 0, 0), w, h);
                if (amount > 0) {
                    g2.setColor(Theme.ACCENT_LIGHT);
                    g2.fillRoundRect(0, 0, amount, h, 8, 8);
                }

                // Border
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                // Text
                if (progressBar.isStringPainted()) {
                    paintString(g2, 0, 0, w, h, amount, new Insets(0, 0, 0, 0));
                }
                g2.dispose();
            }

            @Override
            protected Color getSelectionForeground() {
                return Theme.BG_DARK;
            }

            @Override
            protected Color getSelectionBackground() {
                return Theme.TEXT_PRIMARY;
            }
        });

        // Status Label with dynamic success/error color-coding
        statusLabel = new JLabel("System Ready — Select a file to encrypt or decrypt.");
        statusLabel.setFont(Theme.FONT_STATUS);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    // --- File Selection Handlers ---

    private void chooseEncryptFile() {
        JFileChooser chooser = FileChooserHelper.createFileChooser("Select File to Encrypt", true);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            FileChooserHelper.updateLastDirectory(chooser);
            setEncryptFile(chooser.getSelectedFile());
        }
    }

    private void setEncryptFile(File file) {
        if (file == null) return;
        selectedEncryptFile = file;
        encryptFilePathField.setText(file.getAbsolutePath());
        encryptDropZone.setCurrentFile(file);
        updateFileInfo(file, true);
    }

    private void chooseDecryptFile() {
        JFileChooser chooser = FileChooserHelper.createFileChooser("Select Encrypted File (.enc)", false);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            FileChooserHelper.updateLastDirectory(chooser);
            setDecryptFile(chooser.getSelectedFile());
        }
    }

    private void setDecryptFile(File file) {
        if (file == null) return;
        selectedDecryptFile = file;
        decryptFilePathField.setText(file.getAbsolutePath());
        decryptDropZone.setCurrentFile(file);
        updateFileInfo(file, false);
    }

    /**
     * Attaches a java.awt.dnd DropTarget to a text field, allowing users to drag and drop files onto the input field.
     */
    private void attachFileDropTarget(JTextField field, java.util.function.Consumer<File> onFileDropped) {
        new java.awt.dnd.DropTarget(field, java.awt.dnd.DnDConstants.ACTION_COPY, new java.awt.dnd.DropTargetAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                        java.util.List<File> files = (java.util.List<File>) dtde.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            onFileDropped.accept(files.get(0));
                        }
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    private void updateFileInfo(File file, boolean isEncrypt) {
        if (file == null || !file.exists()) return;
        String name = file.getName();
        String sizeFormatted = formatFileSize(file.length());

        if (isEncrypt) {
            encryptFileNameVal.setText(name);
            encryptFileSizeVal.setText(sizeFormatted);
            encryptTargetVal.setText(name + ".enc");
        } else {
            decryptFileNameVal.setText(name);
            decryptFileSizeVal.setText(sizeFormatted);
            String target = name.toLowerCase().endsWith(".enc") ?
                    name.substring(0, name.length() - 4) : name + ".decrypted";
            decryptTargetVal.setText(target);
        }
    }

    // --- Execution Logic ---

    private void executeEncryption() {
        if (isOperationRunning) return;

        if (selectedEncryptFile == null || !selectedEncryptFile.exists() || !selectedEncryptFile.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid existing file to encrypt.",
                    "File Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        char[] password = encryptPasswordField.getPassword();
        char[] confirmPassword = encryptConfirmPasswordField.getPassword();

        if (password.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Encryption password cannot be empty.",
                    "Password Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length < 8) {
            CryptoService.zeroArray(password);
            CryptoService.zeroArray(confirmPassword);
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters long for adequate security.",
                    "Password Too Short",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Arrays.equals(password, confirmPassword)) {
            CryptoService.zeroArray(password);
            CryptoService.zeroArray(confirmPassword);
            JOptionPane.showMessageDialog(this,
                    "The passwords do not match. Please verify and re-type.",
                    "Password Mismatch",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        CryptoService.zeroArray(confirmPassword);

        File sourceFile = selectedEncryptFile;
        setUiLocked(true);
        statusLabel.setForeground(Theme.TEXT_PRIMARY);
        statusLabel.setText("Deriving key (PBKDF2 600,000 iterations) & encrypting " + sourceFile.getName() + "...");
        progressBar.setValue(0);
        progressBar.setString("0%");

        SwingWorker<File, Integer> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                ProgressListener listener = (processed, total) -> {
                    if (total > 0) {
                        int pct = (int) Math.min(100, (processed * 100) / total);
                        publish(pct);
                    }
                };
                return cryptoService.encrypt(sourceFile, null, password, listener);
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                progressBar.setValue(latest);
                progressBar.setString("Encrypting: " + latest + "%");
            }

            @Override
            protected void done() {
                setUiLocked(false);
                try {
                    File resultFile = get();
                    lastProcessedFile = resultFile;
                    progressBar.setValue(100);
                    progressBar.setString("Completed 100%");
                    statusLabel.setForeground(Theme.SUCCESS);
                    statusLabel.setText("Encrypted successfully: " + resultFile.getName());

                    countEncrypted++;
                    updateStatCards();
                    addHistoryRow(sourceFile.getName(), "Encrypt", "Success", resultFile.getAbsolutePath());

                    encryptPasswordField.setText("");
                    encryptConfirmPasswordField.setText("");
                    passwordStrengthMeter.updatePassword(new char[0]);

                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            "File encrypted successfully!\n\nEncrypted file:\n" + resultFile.getAbsolutePath(),
                            "Encryption Succeeded",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    progressBar.setValue(0);
                    progressBar.setString("Failed");
                    statusLabel.setForeground(Theme.ERROR);
                    statusLabel.setText("Encryption failed: " + cause.getMessage());

                    countFailures++;
                    updateStatCards();
                    addHistoryRow(sourceFile.getName(), "Encrypt", "Failed", cause.getMessage());

                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            "Encryption failed: " + cause.getMessage(),
                            "Encryption Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    CryptoService.zeroArray(password);
                }
            }
        };

        worker.execute();
    }

    private void executeDecryption() {
        if (isOperationRunning) return;

        if (selectedDecryptFile == null || !selectedDecryptFile.exists() || !selectedDecryptFile.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid existing encrypted file to decrypt.",
                    "File Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        char[] password = decryptPasswordField.getPassword();

        if (password.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Decryption password cannot be empty.",
                    "Password Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length < 8) {
            CryptoService.zeroArray(password);
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters long.",
                    "Invalid Password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        File sourceFile = selectedDecryptFile;
        setUiLocked(true);
        statusLabel.setForeground(Theme.TEXT_PRIMARY);
        statusLabel.setText("Deriving key & verifying GCM authentication tag for " + sourceFile.getName() + "...");
        progressBar.setValue(0);
        progressBar.setString("0%");

        SwingWorker<File, Integer> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                ProgressListener listener = (processed, total) -> {
                    if (total > 0) {
                        int pct = (int) Math.min(100, (processed * 100) / total);
                        publish(pct);
                    }
                };
                return cryptoService.decrypt(sourceFile, null, password, listener);
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                progressBar.setValue(latest);
                progressBar.setString("Decrypting: " + latest + "%");
            }

            @Override
            protected void done() {
                setUiLocked(false);
                try {
                    File resultFile = get();
                    lastProcessedFile = resultFile;
                    progressBar.setValue(100);
                    progressBar.setString("Completed 100%");
                    statusLabel.setForeground(Theme.SUCCESS);
                    statusLabel.setText("Decrypted successfully: " + resultFile.getName());

                    countDecrypted++;
                    updateStatCards();
                    addHistoryRow(sourceFile.getName(), "Decrypt", "Success", resultFile.getAbsolutePath());

                    decryptPasswordField.setText("");

                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            "File decrypted & authenticated successfully!\n\nRestored file:\n" + resultFile.getAbsolutePath(),
                            "Decryption Succeeded",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String message = cause.getMessage();
                    if (cause instanceof SecurityException && message != null) {
                        message = "Wrong password or file was tampered with";
                    }

                    progressBar.setValue(0);
                    progressBar.setString("Failed");
                    statusLabel.setForeground(Theme.ERROR);
                    statusLabel.setText("Decryption failed: " + message);

                    countFailures++;
                    updateStatCards();
                    addHistoryRow(sourceFile.getName(), "Decrypt", "Failed", message);

                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            message,
                            "Decryption Failed",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    CryptoService.zeroArray(password);
                }
            }
        };

        worker.execute();
    }

    private void updateStatCards() {
        statEncryptedVal.setText(String.valueOf(countEncrypted));
        statDecryptedVal.setText(String.valueOf(countDecrypted));
        statFailuresVal.setText(String.valueOf(countFailures));
    }

    private void addHistoryRow(String fileName, String action, String result, String details) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        int index = historyTableModel.getRowCount() + 1;
        historyTableModel.addRow(new Object[]{index, fileName, action, timestamp, result, details});
    }

    private void clearHistory() {
        historyTableModel.setRowCount(0);
        countEncrypted = 0;
        countDecrypted = 0;
        countFailures = 0;
        updateStatCards();
    }

    private void openSelectedFolder() {
        int selectedRow = historyTable.getSelectedRow();
        File target = null;
        if (selectedRow >= 0) {
            String path = (String) historyTableModel.getValueAt(selectedRow, 5);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) {
                    target = f.isDirectory() ? f : f.getParentFile();
                }
            }
        }
        if (target == null && lastProcessedFile != null && lastProcessedFile.exists()) {
            target = lastProcessedFile.getParentFile();
        }
        if (target != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(target);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open directory: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a valid successful history entry to open its location.");
        }
    }

    private void setUiLocked(boolean locked) {
        this.isOperationRunning = locked;
        encryptActionBtn.setEnabled(!locked);
        decryptActionBtn.setEnabled(!locked);
        encryptBrowseBtn.setEnabled(!locked);
        decryptBrowseBtn.setEnabled(!locked);
        encryptPasswordField.setEnabled(!locked);
        encryptConfirmPasswordField.setEnabled(!locked);
        decryptPasswordField.setEnabled(!locked);

        setCursor(locked ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    // --- UI Factory & Styling Helpers ---

    private JPanel createFormCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(Theme.SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1),
                new EmptyBorder(20, 24, 20, 24)
        ));
        return card;
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 0);
        return gbc;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXT_SECONDARY);
        return label;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(Theme.FONT_INPUT);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBackground(Theme.FIELD_BG);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setBorder(new FocusAwareBorder(8, new Insets(8, 12, 8, 12), Theme.BORDER, Theme.BORDER_FOCUS));
        field.setToolTipText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e) { field.repaint(); }
        });
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.FONT_INPUT);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBackground(Theme.FIELD_BG);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setEchoChar('\u2022');
        field.setBorder(new FocusAwareBorder(8, new Insets(8, 12, 8, 12), Theme.BORDER, Theme.BORDER_FOCUS));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e) { field.repaint(); }
        });
        return field;
    }

    private JCheckBox createStyledCheckBox(String text) {
        JCheckBox box = new JCheckBox(text);
        box.setFont(Theme.FONT_STATUS);
        box.setForeground(Theme.TEXT_MUTED);
        box.setBackground(Theme.SURFACE);
        box.setFocusPainted(false);
        box.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return box;
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        DecimalFormat df = new DecimalFormat("#,##0.##");
        DecimalFormat commaFormat = new DecimalFormat("#,###");
        return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups] + " (" + commaFormat.format(bytes) + " bytes)";
    }
}
