package com.suiseika.solenoid.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Code-drawn primitives for the Solenoid console theme.
 *
 * <p>Everything here paints with {@link GuiGraphicsExtractor#fill} only — no texture sheets, per the
 * mod's "screens must be code-drawn" rule. Coordinates are screen-absolute; callers add
 * {@code leftPos}/{@code topPos} themselves.
 */
public final class Painter {
    private Painter() {}

    // ---- Frames --------------------------------------------------------------------------------

    /** The outer chassis: solid body with a bevel that reads as raised. */
    public static void chassis(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.CHASSIS);
        g.fill(x, y, x + w, y + 1, Theme.CHASSIS_LIGHT);
        g.fill(x, y, x + 1, y + h, Theme.CHASSIS_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, Theme.CHASSIS_DARK);
        g.fill(x + w - 1, y, x + w, y + h, Theme.CHASSIS_DARK);
    }

    /** Title strip across the top of the panel, closed by a hairline divider. */
    public static void header(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x + 1, y + 1, x + w - 1, y + Theme.HEADER_H, Theme.HEADER);
        g.fill(x + 1, y + Theme.HEADER_H - 1, x + w - 1, y + Theme.HEADER_H, Theme.DIVIDER);
    }

    /** A recessed well: same tone as the chassis, with an inverted bevel so it reads as sunken. */
    public static void well(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.WELL);
        g.fill(x, y, x + w, y + 1, Theme.WELL_SHADOW);
        g.fill(x, y, x + 1, y + h, Theme.WELL_SHADOW);
        g.fill(x, y + h - 1, x + w, y + h, Theme.WELL_HIGHLIGHT);
        g.fill(x + w - 1, y, x + w, y + h, Theme.WELL_HIGHLIGHT);
    }

    /**
     * An item slot recess. {@code slotX}/{@code slotY} are the slot's own 16x16 origin; the 18x18
     * recess is drawn around it, matching vanilla's 1px margin.
     */
    public static void slot(GuiGraphicsExtractor g, int slotX, int slotY) {
        int x = slotX - 1, y = slotY - 1;
        g.fill(x, y, x + 18, y + 18, Theme.SLOT);
        g.fill(x, y, x + 18, y + 1, Theme.SLOT_SHADOW);
        g.fill(x, y, x + 1, y + 18, Theme.SLOT_SHADOW);
        g.fill(x, y + 17, x + 18, y + 18, Theme.SLOT_HIGHLIGHT);
        g.fill(x + 17, y, x + 18, y + 18, Theme.SLOT_HIGHLIGHT);
    }

    /** Draws a 1px accent ring just outside a slot, marking it as a machine output. */
    public static void outputRing(GuiGraphicsExtractor g, int slotX, int slotY) {
        int x = slotX - 2, y = slotY - 2;
        int w = 20, h = 20;
        g.fill(x, y, x + w, y + 1, Theme.SLOT_OUTPUT_RING);
        g.fill(x, y + h - 1, x + w, y + h, Theme.SLOT_OUTPUT_RING);
        g.fill(x, y, x + 1, y + h, Theme.SLOT_OUTPUT_RING);
        g.fill(x + w - 1, y, x + w, y + h, Theme.SLOT_OUTPUT_RING);
    }

    // ---- Bars ----------------------------------------------------------------------------------

    /**
     * Vertical gauge filling bottom-to-top, with a framed track, a bright cap on the fill and
     * evenly spaced tick marks so partial levels stay readable at a glance.
     */
    public static void barVertical(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                   int value, int max, int fill, int bright, int track) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Theme.BAR_FRAME);
        g.fill(x, y, x + w, y + h, track);

        int filled = scale(value, max, h);
        if (filled > 0) {
            int top = y + h - filled;
            g.fill(x, top, x + w, y + h, fill);
            g.fill(x, top, x + w, top + 1, bright);
        }
        // Quarter ticks, drawn over the fill so they read on both halves.
        for (int i = 1; i < 4; i++) {
            int ty = y + h - (h * i / 4);
            g.fill(x, ty, x + 2, ty + 1, Theme.WELL_SHADOW);
            g.fill(x + w - 2, ty, x + w, ty + 1, Theme.WELL_SHADOW);
        }
    }

    /** Vertical EMF gauge in the standard cyan with animated bright cap. */
    public static void energyBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int stored, int max) {
        int animatedBright = pulseColor(Theme.EMF_BRIGHT, 1500);
        barVertical(g, x, y, w, h, stored, max, Theme.EMF, animatedBright, Theme.EMF_TRACK);
    }

    /** Horizontal gauge filling left-to-right, with a bright leading edge. */
    public static void barHorizontal(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                     int value, int max, int fill, int bright, int track) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Theme.BAR_FRAME);
        g.fill(x, y, x + w, y + h, track);

        int filled = scale(value, max, w);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + h, fill);
            g.fill(x + filled - 1, y, x + filled, y + h, bright);
        }
        for (int i = 1; i < 4; i++) {
            int tx = x + (w * i / 4);
            g.fill(tx, y, tx + 1, y + 2, Theme.WELL_SHADOW);
            g.fill(tx, y + h - 2, tx + 1, y + h, Theme.WELL_SHADOW);
        }
    }

    /**
     * Progress arrow: a chevron-tipped bar that fills left-to-right. Unlike a plain rectangle the
     * silhouette itself shows direction, so the flow of the recipe is readable while it is empty.
     */
    public static void progressArrow(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int max) {
        int shaftH = Math.max(2, h / 2);
        int shaftY = y + (h - shaftH) / 2;
        int headW = Math.min(6, w / 3);
        int shaftW = w - headW;

        // Empty track: shaft plus a hollow chevron head.
        g.fill(x, shaftY, x + shaftW, shaftY + shaftH, Theme.WELL_SHADOW);
        chevron(g, x + shaftW, y, headW, h, Theme.WELL_SHADOW);

        int filled = scale(value, max, w);
        if (filled <= 0) {
            return;
        }
        int animatedBright = pulseColor(Theme.PROGRESS_BRIGHT, 1000);
        int shaftFill = Math.min(filled, shaftW);
        if (shaftFill > 0) {
            g.fill(x, shaftY, x + shaftFill, shaftY + shaftH, Theme.PROGRESS);
            g.fill(x + shaftFill - 1, shaftY, x + shaftFill, shaftY + shaftH, animatedBright);
        }
        if (filled > shaftW) {
            // Head fills in proportion once the shaft is full.
            int headFill = filled - shaftW;
            chevronPartial(g, x + shaftW, y, headW, h, headFill, Theme.PROGRESS, animatedBright);
        }
    }

    /** Solid right-pointing chevron head, drawn as a stack of centred rows. */
    private static void chevron(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        chevronPartial(g, x, y, w, h, w, color, color);
    }

    /** Chevron head clipped to its first {@code columns} columns. */
    private static void chevronPartial(GuiGraphicsExtractor g, int x, int y, int w, int h, int columns, int color, int brightColor) {
        int limit = Math.min(columns, w);
        for (int c = 0; c < limit; c++) {
            // Taper symmetrically toward the tip.
            int inset = (h / 2) * c / Math.max(1, w);
            int top = y + inset;
            int bottom = y + h - inset;
            if (bottom <= top) {
                continue;
            }
            int colColor = (c == limit - 1) ? brightColor : color;
            g.fill(x + c, top, x + c + 1, bottom, colColor);
        }
    }

    /**
     * Modulates the RGB components of an ARGB color with a sine wave pulse.
     *
     * @param color original ARGB color
     * @param periodMs pulse cycle duration in milliseconds
     * @return animated ARGB color
     */
    public static int pulseColor(int color, long periodMs) {
        double phase = (System.currentTimeMillis() % periodMs) / (double) periodMs;
        double factor = 0.70 + 0.30 * Math.sin(phase * 2.0 * Math.PI);
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ---- Indicators ----------------------------------------------------------------------------

    /** A small framed status lamp. */
    public static void lamp(GuiGraphicsExtractor g, int x, int y, int size, int color, boolean lit) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, Theme.BAR_FRAME);
        g.fill(x, y, x + size, y + size, lit ? color : Theme.TEXT_FAINT);
        if (lit) {
            // Specular pip, sells it as a lit LED rather than a flat square.
            g.fill(x + 1, y + 1, x + 1 + Math.max(1, size / 3), y + 1 + Math.max(1, size / 3), Theme.LAMP_GLINT);
        }
    }

    /** Status lamp plus its caption, laid out on one line. */
    public static void status(GuiGraphicsExtractor g, Font font, int x, int y, int color, boolean lit, Component label) {
        lamp(g, x, y, 5, color, lit);
        g.text(font, label, x + 9, y - 1, lit ? Theme.TEXT : Theme.TEXT_DIM, false);
    }

    // ---- Text ----------------------------------------------------------------------------------

    /** Right-aligned text, for numeric readouts that should not jitter as digits change. */
    public static void textRight(GuiGraphicsExtractor g, Font font, String text, int rightX, int y, int color) {
        g.text(font, text, rightX - font.width(text), y, color, false);
    }

    /** Thousands-separated number, so "50000 EMF" reads as "50,000 EMF". */
    public static String number(int value) {
        return String.format("%,d", value);
    }

    /** Integer percentage, guarding a zero maximum. */
    public static int percent(int value, int max) {
        return max > 0 ? (int) ((long) value * 100L / max) : 0;
    }

    // ---- Helpers -------------------------------------------------------------------------------

    /** Scales {@code value/max} onto {@code span} pixels, clamped and overflow-safe. */
    public static int scale(int value, int max, int span) {
        if (max <= 0 || value <= 0) {
            return 0;
        }
        long scaled = (long) span * value / max;
        return (int) Math.min(span, scaled);
    }

    /** Point-in-rectangle test for hover regions. */
    public static boolean hovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
