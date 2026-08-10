package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import com.suiseika.solenoid.client.ui.Painter;
import com.suiseika.solenoid.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Capacitor console. No machine slots, so the whole well becomes a charge readout: a wide
 * horizontal gauge with the exact figures beneath it.
 */
public class CapacitorScreen extends MachineScreen<CapacitorMenu> {
    private static final int BAR_X = 16, BAR_Y = 40, BAR_W = 144, BAR_H = 14;

    public CapacitorScreen(CapacitorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int energyStored() {
        return this.menu.getEnergyStored();
    }

    @Override
    protected int energyMax() {
        return this.menu.getEnergyMax();
    }

    /** The wide horizontal gauge replaces the standard vertical one. */
    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    /** No recipes to show. */
    @Override
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.empty();
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int stored = energyStored();
        int max = energyMax();

        g.text(this.font, Component.translatable("gui.solenoid.capacitor.charge"),
                x + BAR_X, y + 28, Theme.TEXT_DIM, false);
        Painter.textRight(g, this.font, Painter.percent(stored, max) + "%",
                x + BAR_X + BAR_W, y + 28, Theme.EMF);

        Painter.barHorizontal(g, x + BAR_X, y + BAR_Y, BAR_W, BAR_H,
                stored, max, Theme.EMF, Theme.EMF_BRIGHT, Theme.EMF_TRACK);

        g.text(this.font, Painter.number(stored) + " EMF", x + BAR_X, y + 60, Theme.TEXT, false);
        Painter.textRight(g, this.font, Painter.number(max) + " EMF",
                x + BAR_X + BAR_W, y + 60, Theme.TEXT_FAINT);

        (stored > 0 ? MachineStatus.CHARGED : MachineStatus.IDLE)
                .draw(g, this.font, x, y, mouseX, mouseY);

        if (Painter.hovering(mouseX, mouseY, x + BAR_X, y + BAR_Y, BAR_W, BAR_H)) {
            energyTooltip(g, mouseX, mouseY);
        }
    }
}
