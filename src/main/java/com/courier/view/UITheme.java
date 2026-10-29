package com.courier.view;

import com.courier.model.ShipmentStatus;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Modern design system and UI constants for Java Swing Views in MVC architecture.
 */
public final class UITheme {

    // ── Primary Palette ─────────────────────────────────────────────────────────
    public static final Color COLOR_PRIMARY        = new Color(30, 58, 138);   // Deep Indigo Blue
    public static final Color COLOR_PRIMARY_LIGHT  = new Color(239, 246, 255); // Soft blue tint
    public static final Color COLOR_ACCENT         = new Color(37, 99, 235);   // Vibrant Blue
    public static final Color COLOR_ACCENT_HOVER   = new Color(29, 78, 216);

    // ── Status Badges ───────────────────────────────────────────────────────────
    public static final Color COLOR_DELIVERED_BG   = new Color(220, 252, 231);
    public static final Color COLOR_DELIVERED_FG   = new Color(22, 101, 52);

    public static final Color COLOR_IN_TRANSIT_BG  = new Color(254, 243, 199);
    public static final Color COLOR_IN_TRANSIT_FG  = new Color(146, 64, 14);

    public static final Color COLOR_OUT_FOR_DEL_BG = new Color(224, 242, 254);
    public static final Color COLOR_OUT_FOR_DEL_FG = new Color(7, 89, 133);

    public static final Color COLOR_DISPATCHED_BG  = new Color(243, 232, 255);
    public static final Color COLOR_DISPATCHED_FG  = new Color(107, 33, 168);

    public static final Color COLOR_RETURNED_BG    = new Color(254, 226, 226);
    public static final Color COLOR_RETURNED_FG    = new Color(153, 27, 27);

    public static final Color COLOR_ON_HOLD_BG     = new Color(254, 249, 195);
    public static final Color COLOR_ON_HOLD_FG     = new Color(133, 77, 14);

    // ── Neutral Surface & Borders ───────────────────────────────────────────────
    public static final Color COLOR_BG_APP         = new Color(248, 250, 252); // Slate 50
    public static final Color COLOR_CARD_BG        = Color.WHITE;
    public static final Color COLOR_BORDER         = new Color(226, 232, 240); // Slate 200
    public static final Color COLOR_TEXT_MAIN      = new Color(15, 23, 42);    // Slate 900
    public static final Color COLOR_TEXT_MUTED     = new Color(100, 116, 139); // Slate 500
    public static final Color COLOR_TABLE_HEADER   = new Color(241, 245, 249);

    // ── Typography ──────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE            = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE         = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_HEADER           = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BODY             = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD             = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_STAT_NUM         = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_STAT_LABEL       = new Font("Segoe UI", Font.PLAIN, 11);

    private UITheme() {}

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(COLOR_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(COLOR_TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(7, 15, 7, 15)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(new Color(239, 68, 68));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new CompoundBorder(
            new LineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        private final JLabel badge = new JLabel();

        public StatusBadgeRenderer() {
            badge.setOpaque(true);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setFont(FONT_BOLD);
            badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (value == null) {
                badge.setText("N/A");
                badge.setBackground(COLOR_BORDER);
                badge.setForeground(COLOR_TEXT_MUTED);
                return badge;
            }

            String str = value.toString();
            ShipmentStatus status = ShipmentStatus.fromString(str);
            badge.setText(status.getDisplayName());

            switch (status) {
                case DELIVERED -> {
                    badge.setBackground(COLOR_DELIVERED_BG);
                    badge.setForeground(COLOR_DELIVERED_FG);
                }
                case IN_TRANSIT -> {
                    badge.setBackground(COLOR_IN_TRANSIT_BG);
                    badge.setForeground(COLOR_IN_TRANSIT_FG);
                }
                case OUT_FOR_DELIVERY -> {
                    badge.setBackground(COLOR_OUT_FOR_DEL_BG);
                    badge.setForeground(COLOR_OUT_FOR_DEL_FG);
                }
                case DISPATCHED -> {
                    badge.setBackground(COLOR_DISPATCHED_BG);
                    badge.setForeground(COLOR_DISPATCHED_FG);
                }
                case FAILED_DELIVERY, RETURNED -> {
                    badge.setBackground(COLOR_RETURNED_BG);
                    badge.setForeground(COLOR_RETURNED_FG);
                }
                case ON_HOLD -> {
                    badge.setBackground(COLOR_ON_HOLD_BG);
                    badge.setForeground(COLOR_ON_HOLD_FG);
                }
                default -> {
                    badge.setBackground(COLOR_ON_HOLD_BG);
                    badge.setForeground(COLOR_ON_HOLD_FG);
                }
            }

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            wrapper.add(badge);
            return wrapper;
        }
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(FONT_BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(COLOR_BORDER);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(COLOR_TEXT_MAIN);

        table.getTableHeader().setFont(FONT_HEADER);
        table.getTableHeader().setBackground(COLOR_TABLE_HEADER);
        table.getTableHeader().setForeground(COLOR_TEXT_MAIN);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setReorderingAllowed(false);
    }
}
