package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralized theme providing colors, typography, and styling tokens for the UI.
 */
public final class Theme {

    private Theme() {}

    // --- Color Palette ---
    public static final Color BG_DARK          = new Color(15, 23, 42);     // #0F172A Slate 900
    public static final Color SURFACE          = new Color(30, 41, 59);     // #1E293B Slate 800
    public static final Color SURFACE_ALT      = new Color(51, 65, 85);     // #334155 Slate 700
    public static final Color SURFACE_HOVER    = new Color(71, 85, 105);    // #475569 Slate 600
    public static final Color FIELD_BG         = new Color(15, 23, 42);     // #0F172A Deep input bg

    public static final Color BORDER           = new Color(71, 85, 105);    // #475569 Slate 600
    public static final Color BORDER_LIGHT     = new Color(100, 116, 139);  // #64748B Slate 500
    public static final Color BORDER_FOCUS     = new Color(96, 165, 250);   // #60A5FA Focus blue

    public static final Color ACCENT           = new Color(37, 99, 235);    // #2563EB Electric Blue
    public static final Color ACCENT_HOVER     = new Color(29, 78, 216);    // #1D4ED8
    public static final Color ACCENT_PRESSED   = new Color(30, 64, 175);    // #1E40AF
    public static final Color ACCENT_LIGHT     = new Color(56, 189, 248);   // #38BDF8 Cyan/Sky

    public static final Color SUCCESS          = new Color(16, 185, 129);   // #10B981 Emerald 500
    public static final Color SUCCESS_HOVER    = new Color(5, 150, 105);    // #059669
    public static final Color SUCCESS_PRESSED  = new Color(4, 120, 87);     // #047857

    public static final Color ERROR            = new Color(239, 68, 68);    // #EF4444 Rose 500
    public static final Color ERROR_HOVER      = new Color(220, 38, 38);    // #DC2626

    public static final Color WARNING          = new Color(245, 158, 11);   // #F59E0B Amber 500

    public static final Color TEXT_PRIMARY     = new Color(248, 250, 252);  // #F8FAFC
    public static final Color TEXT_SECONDARY   = new Color(203, 213, 225);  // #CBD5E1
    public static final Color TEXT_MUTED       = new Color(148, 163, 184);  // #94A3B8
    public static final Color TEXT_DISABLED    = new Color(100, 116, 139);  // #64748B

    // --- Typography with Safe Font Fallback ---
    private static final String FONT_FAMILY;
    static {
        Set<String> fonts = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
        ));
        if (fonts.contains("Segoe UI")) {
            FONT_FAMILY = "Segoe UI";
        } else if (fonts.contains("Roboto")) {
            FONT_FAMILY = "Roboto";
        } else if (fonts.contains("Inter")) {
            FONT_FAMILY = "Inter";
        } else {
            FONT_FAMILY = Font.SANS_SERIF;
        }
    }

    public static final Font FONT_TITLE     = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_SUBTITLE  = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SECTION   = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_LABEL     = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_INPUT     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BUTTON    = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_STATUS    = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_BADGE     = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_SMALL     = new Font(FONT_FAMILY, Font.PLAIN, 11);
}
