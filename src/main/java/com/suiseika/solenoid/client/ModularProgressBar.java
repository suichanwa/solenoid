package com.suiseika.solenoid.client;

import com.suiseika.solenoid.Solenoid;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Utility for drawing industrial modular progress bars using tileable components.
 * Consists of background caps, a repeating middle, and a glowing progress foreground.
 */
public class ModularProgressBar {
    private static final Identifier BG_CAP_LEFT = Identifier.fromNamespaceAndPath(Solenoid.MODID, "textures/gui/progress/bg_cap_left.png");
    private static final Identifier BG_CAP_RIGHT = Identifier.fromNamespaceAndPath(Solenoid.MODID, "textures/gui/progress/bg_cap_right.png");
    private static final Identifier BG_MIDDLE = Identifier.fromNamespaceAndPath(Solenoid.MODID, "textures/gui/progress/bg_middle.png");
    private static final Identifier FG_MIDDLE = Identifier.fromNamespaceAndPath(Solenoid.MODID, "textures/gui/progress/fg_middle.png");
    private static final Identifier FG_LEAD = Identifier.fromNamespaceAndPath(Solenoid.MODID, "textures/gui/progress/fg_lead.png");

    /**
     * Draws a modular progress bar.
     * @param g The graphics extractor.
     * @param x Screen X position.
     * @param y Screen Y position.
     * @param width Total width of the bar (minimum 32 suggested).
     * @param progress Progress percentage (0.0 - 1.0).
     */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int width, float progress) {
        int capW = 16;
        int midW = width - (capW * 2);
        if (midW < 0) midW = 0;

        // 1. Draw Background
        // Left Cap
        blit(g, BG_CAP_LEFT, x, y, 0, 0, capW, 16, 16, 16);
        // Middle (Tiled)
        for (int i = 0; i < midW; i += 16) {
            int drawW = Math.min(16, midW - i);
            blit(g, BG_MIDDLE, x + capW + i, y, 0, 0, drawW, 16, 16, 16);
        }
        // Right Cap
        blit(g, BG_CAP_RIGHT, x + capW + midW, y, 0, 0, capW, 16, 16, 16);

        // 2. Draw Foreground (Progress)
        int fillW = (int) (width * progress);
        if (fillW > 0) {
            // Draw filled middle (tiled inside the frame)
            // Note: We might need to clip or calculate inner area if frame has borders.
            // For now, assume it fills the whole height.
            int fgX = x;
            int fgRemaining = fillW;
            
            // We use a lead edge at the very front
            int leadW = 8;
            int mainFgW = fillW - leadW;
            if (mainFgW < 0) {
                mainFgW = 0;
                leadW = fillW;
            }

            // Tile FG_MIDDLE
            for (int i = 0; i < mainFgW; i += 16) {
                int drawW = Math.min(16, mainFgW - i);
                blit(g, FG_MIDDLE, fgX + i, y, 0, 0, drawW, 16, 16, 16);
            }
            // Draw FG_LEAD at the end
            if (leadW > 0) {
                blit(g, FG_LEAD, fgX + mainFgW, y, 0, 0, leadW, 16, 8, 16);
            }
        }
    }

    private static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        // Fallback to fill if blit doesn't exist on g
        // I'll try to use a hypothetical 'blit' method.
        // If it doesn't exist, I'll need to find the correct method name.
        try {
            // Placeholder for real blit call
            // g.blit(tex, x, y, u, v, w, h, texW, texH);
        } catch (Exception e) {
            // Fallback: draw a colored box
            // g.fill(x, y, x + w, y + h, 0xFF3AA6FF);
        }
    }
}