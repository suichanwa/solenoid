package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.JeiBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Fully code-drawn Induction Furnace GUI, following the {@link CrusherScreen} pattern exactly: panel,
 * slot recesses, energy bar and progress arrow are all painted with {@link GuiGraphicsExtractor#fill}
 * (no texture sheet, no magenta). The energy bar shows correct values above 32767 because the menu
 * reassembles the two synced short halves as unsigned.
 */
public class InductionFurnaceScreen extends AbstractContainerScreen<InductionFurnaceMenu> {
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BAR_X = 12, BAR_Y = 16, BAR_W = 12, BAR_H = 52;
    private static final int BAR_FRAME = 0xFF373737;
    private static final int BAR_EMPTY = 0xFF555555;
    private static final int BAR_FILL = 0xFFFF7A1A;
    /** Public so the JEI plugin can hang its recipe click area on the exact same rectangle. */
    public static final int ARROW_X = 79, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 16;
    // "Show recipes" button, tucked into the free top-right corner of the panel.
    private static final int JEI_BUTTON_X = 152, JEI_BUTTON_Y = 4;

    public InductionFurnaceScreen(InductionFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        JeiBridge.recipeButton(this.leftPos + JEI_BUTTON_X, this.topPos + JEI_BUTTON_Y, getClass())
                .ifPresent(this::addRenderableWidget);
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

        // Slot recesses (auto-aligned to every real slot)
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_INNER);
            g.fill(sx, sy, sx + 18, sy + 1, SLOT_DARK);
            g.fill(sx, sy, sx + 1, sy + 18, SLOT_DARK);
            g.fill(sx, sy + 17, sx + 18, sy + 18, SLOT_LIGHT);
            g.fill(sx + 17, sy, sx + 18, sy + 18, SLOT_LIGHT);
        }

        // Energy bar (fills bottom -> top)
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, BAR_FRAME);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, BAR_EMPTY);
        int stored = this.menu.getEnergyStored();
        int max = this.menu.getEnergyMax();
        if (max > 0 && stored > 0) {
            int fillH = (int) ((long) BAR_H * stored / max);
            g.fill(bx, by + BAR_H - fillH, bx + BAR_W, by + BAR_H, BAR_FILL);
        }

        // Progress arrow (fills left -> right)
        int ax = x + ARROW_X, ay = y + ARROW_Y;
        g.fill(ax, ay, ax + ARROW_W, ay + ARROW_H, PANEL_DARK);
        int fillW = ARROW_W * this.menu.getProgress() / this.menu.getMaxProgress();
        if (fillW > 0) {
            g.fill(ax, ay, ax + fillW, ay + ARROW_H, PANEL_LIGHT);
        }

        // Energy bar hover tooltip
        if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H) {
            g.setTooltipForNextFrame(Component.literal(stored + " / " + max + " EMF"), mouseX, mouseY);
        }
    }
}
