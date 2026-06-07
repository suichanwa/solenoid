package com.suiseika.solenoid.energy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Fully code-drawn Capacitor GUI, following the {@link CrusherScreen} pattern. No machine slots: a
 * single wide horizontal EMF bar shows the stored charge, with a hover tooltip giving exact numbers.
 * Panel and player-inventory slot recesses are painted with {@link GuiGraphicsExtractor#fill}; the
 * title and "Inventory" label are drawn by {@link AbstractContainerScreen}'s vanilla label pass.
 */
public class CapacitorScreen extends AbstractContainerScreen<CapacitorMenu> {
    // Panel bevel
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    // Slot recess
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    // Energy bar (wide, horizontal, fills left -> right)
    private static final int BAR_X = 16, BAR_Y = 22, BAR_W = 144, BAR_H = 18;
    private static final int BAR_FRAME = 0xFF373737;
    private static final int BAR_EMPTY = 0xFF555555;
    private static final int BAR_FILL = 0xFF3AA6FF;

    public CapacitorScreen(CapacitorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos, y = this.topPos, w = this.imageWidth, h = this.imageHeight;

        // Beveled panel
        g.fill(x, y, x + w, y + h, PANEL_FILL);
        g.fill(x, y, x + w, y + 1, PANEL_LIGHT);
        g.fill(x, y, x + 1, y + h, PANEL_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_DARK);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_DARK);

        // Slot recesses (auto-aligned to every real slot -- player inventory only)
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_INNER);
            g.fill(sx, sy, sx + 18, sy + 1, SLOT_DARK);
            g.fill(sx, sy, sx + 1, sy + 18, SLOT_DARK);
            g.fill(sx, sy + 17, sx + 18, sy + 18, SLOT_LIGHT);
            g.fill(sx + 17, sy, sx + 18, sy + 18, SLOT_LIGHT);
        }

        // Energy bar (fills left -> right)
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, BAR_FRAME);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, BAR_EMPTY);
        int stored = this.menu.getEnergyStored();
        int max = this.menu.getEnergyMax();
        if (max > 0 && stored > 0) {
            int fillW = (int) ((long) BAR_W * stored / max);
            g.fill(bx, by, bx + fillW, by + BAR_H, BAR_FILL);
        }

        // Energy bar hover tooltip
        if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H) {
            g.setTooltipForNextFrame(Component.literal(stored + " / " + max + " EMF"), mouseX, mouseY);
        }
    }
}
