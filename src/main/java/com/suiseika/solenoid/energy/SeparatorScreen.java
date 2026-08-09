package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.JeiBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Code-drawn Separator GUI (no texture sheet). */
public class SeparatorScreen extends AbstractContainerScreen<SeparatorMenu> {
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BAR_X = 12, BAR_Y = 16, BAR_W = 12, BAR_H = 52;
    private static final int BAR_FRAME = 0xFF373737;
    private static final int BAR_EMPTY = 0xFF555555;
    private static final int BAR_FILL = 0xFF3AA6FF;
    /** Public so the JEI plugin can hang its recipe click area on the exact same rectangle. */
    public static final int ARROW_X = 70, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 16;
    private static final int FIELD_X = 150, FIELD_Y = 16, FIELD_SIZE = 10;
    private static final int FIELD_ON = 0xFF3AA6FF;
    private static final int FIELD_OFF = 0xFF552020;
    // Sits below the magnetic-field indicator, which already occupies the top-right corner.
    private static final int JEI_BUTTON_X = 150, JEI_BUTTON_Y = 30;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
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

        g.fill(x, y, x + w, y + h, PANEL_FILL);
        g.fill(x, y, x + w, y + 1, PANEL_LIGHT);
        g.fill(x, y, x + 1, y + h, PANEL_LIGHT);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_DARK);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_DARK);

        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_INNER);
            g.fill(sx, sy, sx + 18, sy + 1, SLOT_DARK);
            g.fill(sx, sy, sx + 1, sy + 18, SLOT_DARK);
            g.fill(sx, sy + 17, sx + 18, sy + 18, SLOT_LIGHT);
            g.fill(sx + 17, sy, sx + 18, sy + 18, SLOT_LIGHT);
        }

        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, BAR_FRAME);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, BAR_EMPTY);
        int stored = this.menu.getEnergyStored();
        int max = this.menu.getEnergyMax();
        if (max > 0 && stored > 0) {
            int fillH = (int) ((long) BAR_H * stored / max);
            g.fill(bx, by + BAR_H - fillH, bx + BAR_W, by + BAR_H, BAR_FILL);
        }

        int ax = x + ARROW_X, ay = y + ARROW_Y;
        g.fill(ax, ay, ax + ARROW_W, ay + ARROW_H, PANEL_DARK);
        int fillW = ARROW_W * this.menu.getProgress() / this.menu.getMaxProgress();
        if (fillW > 0) {
            g.fill(ax, ay, ax + fillW, ay + ARROW_H, PANEL_LIGHT);
        }

        boolean fieldActive = this.menu.isFieldActive();
        int fx = x + FIELD_X, fy = y + FIELD_Y;
        g.fill(fx - 1, fy - 1, fx + FIELD_SIZE + 1, fy + FIELD_SIZE + 1, SLOT_DARK);
        g.fill(fx, fy, fx + FIELD_SIZE, fy + FIELD_SIZE, fieldActive ? FIELD_ON : FIELD_OFF);

        if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H) {
            g.setTooltipForNextFrame(Component.literal(stored + " / " + max + " EMF"), mouseX, mouseY);
        } else if (mouseX >= fx && mouseX < fx + FIELD_SIZE && mouseY >= fy && mouseY < fy + FIELD_SIZE) {
            g.setTooltipForNextFrame(
                    Component.literal("Magnetic field: " + (fieldActive ? "active" : "inactive")), mouseX, mouseY);
        }
    }
}
