package ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Drag-and-Drop file landing zone supporting java.awt.dnd and click-to-browse.
 * Displays a styled dashed border, hover feedback, and file status.
 */
public class DropZonePanel extends JPanel implements DropTargetListener {

    private final String defaultSubtitle;
    private boolean isDragOver = false;
    private boolean isHovered = false;
    private File currentFile = null;

    private Consumer<File> fileSelectedListener;
    private Runnable browseActionListener;

    public DropZonePanel(String defaultSubtitle) {
        this.defaultSubtitle = defaultSubtitle != null ? defaultSubtitle : "Drag a file here or click Browse";
        setOpaque(false);
        setPreferredSize(new Dimension(0, 85));
        setMinimumSize(new Dimension(0, 80));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Enable Drag & Drop
        new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

        // Click to browse
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (browseActionListener != null) {
                    browseActionListener.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setOnFileSelected(Consumer<File> listener) {
        this.fileSelectedListener = listener;
    }

    public void setOnBrowseAction(Runnable runnable) {
        this.browseActionListener = runnable;
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        repaint();
    }

    // --- DropTargetListener Implementation ---

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            isDragOver = true;
            repaint();
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {}

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}

    @Override
    public void dragExit(DropTargetEvent dte) {
        isDragOver = false;
        repaint();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        try {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (files != null && !files.isEmpty()) {
                    File selected = files.get(0);
                    setCurrentFile(selected);
                    if (fileSelectedListener != null) {
                        fileSelectedListener.accept(selected);
                    }
                }
                dtde.dropComplete(true);
            } else {
                dtde.rejectDrop();
            }
        } catch (Exception ex) {
            dtde.dropComplete(false);
        } finally {
            isDragOver = false;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Background Fill
        Color bg;
        if (isDragOver) {
            bg = new Color(30, 58, 138, 120); // semi-transparent blue highlight
        } else if (isHovered) {
            bg = Theme.SURFACE_ALT;
        } else {
            bg = Theme.SURFACE;
        }
        g2.setColor(bg);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 12, 12);

        // 2. Dashed Border
        Color strokeColor = isDragOver ? Theme.ACCENT_LIGHT : (isHovered ? Theme.ACCENT : Theme.BORDER);
        float[] dashPattern = {8.0f, 6.0f};
        g2.setColor(strokeColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashPattern, 0.0f));
        g2.drawRoundRect(2, 2, w - 5, h - 5, 12, 12);

        // 3. Text Message
        FontMetrics fmMain = g2.getFontMetrics(Theme.FONT_BUTTON);
        FontMetrics fmSub = g2.getFontMetrics(Theme.FONT_SMALL);

        String mainText;
        String subText;
        Color mainColor;

        if (currentFile != null) {
            mainText = "Selected: " + currentFile.getName();
            subText = "Drop another file or click to choose a different one";
            mainColor = Theme.SUCCESS;
        } else {
            mainText = "Drag a file here or click Browse";
            subText = defaultSubtitle;
            mainColor = Theme.TEXT_PRIMARY;
        }

        int mainY = (h / 2) - 4;
        g2.setFont(Theme.FONT_BUTTON);
        g2.setColor(mainColor);
        int mainX = (w - fmMain.stringWidth(mainText)) / 2;
        g2.drawString(mainText, Math.max(10, mainX), mainY);

        int subY = mainY + fmSub.getHeight() + 2;
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        int subX = (w - fmSub.stringWidth(subText)) / 2;
        g2.drawString(subText, Math.max(10, subX), subY);

        g2.dispose();
    }
}
