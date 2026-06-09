package com.suiseika.solenoid.energy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Fully code-drawn Thorium RTG GUI, following the {@link CapacitorScreen} pattern. No machine slots.
 * A vertical EMF bar shows the buffered charge and a fuel bar shows the remaining decay fuel; text
 * lines report the current generation rate and the exact stored/max EMF. Hover tooltips on each bar
 * give precise numbers.
 */
public class ThoriumRtgScreen extends AbstractContainerScreen<ThoriumRtgMenu> {
    // Panel bevel
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    // Slot recess
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    // Text
    private static final int TEXT_COLOR = 0xFF373737;
    // EMF buffer bar (vertical, fills bottom -> top)
    private static final int EMF_X = 16, EMF_Y = 18, EMF_W = 18, EMF_H = 52;
    private static final int EMF_FILL = 0xFF3AA6FF;
    // Fuel bar (vertical, fills bottom -> top)
    private static final int FUEL_X = 142, FUEL_Y = 18, FUEL_W = 18, FUEL_H = 52;
    private static final int FUEL_FILL = 0xFF55E03A;
    // Shared bar colors
    private static final int BAR_FRAME = 0xFF373737;
    private static final int BAR_EMPTY = 0xFF555555;

    public ThoriumRtgScreen(ThoriumRtgMenu menu, Inventory inventory, Component title) {
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

        int stored = this.menu.getEnergyStored();
        int max = this.menu.getEnergyMax();
        int fuel = this.menu.getFuel();
        int fuelMax = this.menu.getFuelMax();
        int rate = this.menu.getGenRate();

        // EMF buffer bar (fills bottom -> top)
        drawVBar(g, x + EMF_X, y + EMF_Y, EMF_W, EMF_H, stored, max, EMF_FILL);
        // Fuel bar (fills bottom -> top)
        drawVBar(g, x + FUEL_X, y + FUEL_Y, FUEL_W, FUEL_H, fuel, fuelMax, FUEL_FILL);

        // Readout text
        int tx = x + EMF_X + EMF_W + 6;
        g.text(this.font, "Output: " + rate + " EMF/t", tx, y + 20, TEXT_COLOR, false);
        g.text(this.font, "Stored:", tx, y + 36, TEXT_COLOR, false);
        g.text(this.font, stored + " / " + max + " EMF", tx, y + 48, TEXT_COLOR, false);
        g.text(this.font, "Fuel: " + percent(fuel, fuelMax) + "%", tx, y + 64, TEXT_COLOR, false);

        // Bar hover tooltips
        if (inBar(mouseX, mouseY, x + EMF_X, y + EMF_Y, EMF_W, EMF_H)) {
            g.setTooltipForNextFrame(Component.literal(stored + " / " + max + " EMF"), mouseX, mouseY);
        } else if (inBar(mouseX, mouseY, x + FUEL_X, y + FUEL_Y, FUEL_W, FUEL_H)) {
            g.setTooltipForNextFrame(Component.literal(fuel + " / " + fuelMax + " fuel"), mouseX, mouseY);
        }
    }

    private static void drawVBar(GuiGraphicsExtractor g, int bx, int by, int bw, int bh, int value, int valueMax, int fillColor) {
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, BAR_FRAME);
        g.fill(bx, by, bx + bw, by + bh, BAR_EMPTY);
        if (valueMax > 0 && value > 0) {
            int fillH = (int) ((long) bh * value / valueMax);
            g.fill(bx, by + bh - fillH, bx + bw, by + bh, fillColor);
        }
    }

    private static boolean inBar(int mouseX, int mouseY, int bx, int by, int bw, int bh) {
        return mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
    }

    private static int percent(int value, int valueMax) {
        return valueMax > 0 ? (int) ((long) value * 100 / valueMax) : 0;
    }
}
