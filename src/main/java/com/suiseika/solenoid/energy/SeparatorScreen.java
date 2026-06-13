package com.suiseika.solenoid.energy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

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
    private static final int ARROW_X = 70, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 16;
    private static final int FIELD_X = 150, FIELD_Y = 16, FIELD_SIZE = 10;
    private static final int FIELD_ON = 0xFF3AA6FF;
    private static final int FIELD_OFF = 0xFF552020;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
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
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int x = this.leftPos, y = this.topPos;
        int bx = x + BAR_X, by = y + BAR_Y;
        int fx = x + FIELD_X, fy = y + FIELD_Y;

        if (mouseX >= bx && mouseX < bx + BAR_W && mouseY >= by && mouseY < by + BAR_H) {
            int stored = this.menu.getEnergyStored();
            int max = this.menu.getEnergyMax();
            g.renderTooltip(this.font, Component.literal(stored + " / " + max + " EMF"), mouseX, mouseY);
        } else if (mouseX >= fx && mouseX < fx + FIELD_SIZE && mouseY >= fy && mouseY < fy + FIELD_SIZE) {
            boolean fieldActive = this.menu.isFieldActive();
            g.renderTooltip(this.font,
                    Component.literal("Magnetic field: " + (fieldActive ? "active" : "inactive")), mouseX, mouseY);
        }
    }
}
