package com.bobroflza.tierednetworks.client;

import net.minecraft.client.gui.GuiGraphics;

/** Vanilla-style panels and slots drawn with plain fills, so the GUIs need no texture files. */
public final class GuiDraw {
    public static final int LABEL = 0xFF404040;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;

    private GuiDraw() {}

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, DARK);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, DARK);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    /** Draws a slot whose item sits at (x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BG);
        g.fill(x - 1, y - 1, x + 16, y, SLOT_DARK);
        g.fill(x - 1, y - 1, x, y + 16, SLOT_DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
    }

    public static void playerInventory(GuiGraphics g, int left, int top, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, left + x + col * 18, top + y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(g, left + x + col * 18, top + y + 58);
        }
    }

    public static String shortCount(long count) {
        if (count < 1_000) return Long.toString(count);
        if (count < 1_000_000) return trim(count / 1_000.0) + "k";
        if (count < 1_000_000_000) return trim(count / 1_000_000.0) + "M";
        return trim(count / 1_000_000_000.0) + "G";
    }

    private static String trim(double v) {
        return v >= 100 ? Long.toString((long) v) : String.format("%.1f", v);
    }
}
