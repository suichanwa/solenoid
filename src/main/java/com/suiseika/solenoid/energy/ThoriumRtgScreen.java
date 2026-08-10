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
 * Thorium RTG console. Twin gauges — EMF buffer on the left, decay fuel on the right — flanking a
 * centred readout of the current generation rate.
 */
public class ThoriumRtgScreen extends MachineScreen<ThoriumRtgMenu> {
    private static final int EMF_X = 14, EMF_Y = 26, EMF_W = 14, EMF_H = 52;
    private static final int FUEL_X = 148, FUEL_Y = 26, FUEL_W = 14, FUEL_H = 52;

    public ThoriumRtgScreen(ThoriumRtgMenu menu, Inventory inventory, Component title) {
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

    /** Uses a bespoke pair of gauges rather than the standard one. */
    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    /** Generates power; it has no recipes. */
    @Override
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.empty();
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int stored = energyStored();
        int max = energyMax();
        int fuel = this.menu.getFuel();
        int fuelMax = this.menu.getFuelMax();
        int rate = this.menu.getGenRate();

        Painter.energyBar(g, x + EMF_X, y + EMF_Y, EMF_W, EMF_H, stored, max);
        Painter.barVertical(g, x + FUEL_X, y + FUEL_Y, FUEL_W, FUEL_H, fuel, fuelMax,
                Theme.FUEL, Theme.FUEL_BRIGHT, Theme.EMF_TRACK);

        int textX = x + EMF_X + EMF_W + 10;
        int textRight = x + FUEL_X - 6;

        g.text(this.font, Component.translatable("gui.solenoid.rtg.output"), textX, y + 30, Theme.TEXT_DIM, false);
        Painter.textRight(g, this.font, rate + " EMF/t", textRight, y + 30, Theme.GOOD);

        g.text(this.font, Component.translatable("gui.solenoid.rtg.buffer"), textX, y + 46, Theme.TEXT_DIM, false);
        Painter.textRight(g, this.font, Painter.number(stored), textRight, y + 46, Theme.EMF);

        g.text(this.font, Component.translatable("gui.solenoid.rtg.fuel"), textX, y + 62, Theme.TEXT_DIM, false);
        Painter.textRight(g, this.font, Painter.percent(fuel, fuelMax) + "%", textRight, y + 62, Theme.FUEL);

        (fuel > 0 ? MachineStatus.GENERATING : MachineStatus.DEPLETED)
                .draw(g, this.font, x, y, mouseX, mouseY);

        if (Painter.hovering(mouseX, mouseY, x + EMF_X, y + EMF_Y, EMF_W, EMF_H)) {
            energyTooltip(g, mouseX, mouseY);
        } else if (Painter.hovering(mouseX, mouseY, x + FUEL_X, y + FUEL_Y, FUEL_W, FUEL_H)) {
            g.setTooltipForNextFrame(Component.literal(
                    Painter.number(fuel) + " / " + Painter.number(fuelMax)), mouseX, mouseY);
        }
    }
}
