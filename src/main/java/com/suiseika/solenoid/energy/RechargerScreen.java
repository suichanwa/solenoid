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
 * Recharger console. The EMF gauge feeds a flow chevron pointing into the item slot, which lights
 * only while there is both charge to give and an item to receive it.
 */
public class RechargerScreen extends MachineScreen<RechargerMenu> {
    private static final int FLOW_X = 44, FLOW_Y = 35, FLOW_W = 26, FLOW_H = 16;

    public RechargerScreen(RechargerMenu menu, Inventory inventory, Component title) {
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

    /** Charges items rather than running recipes. */
    @Override
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.empty();
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int stored = energyStored();
        boolean hasItem = !this.menu.slots.get(0).getItem().isEmpty();
        boolean charging = stored > 0 && hasItem;

        // Full chevron while charging, empty track otherwise — the silhouette shows direction either way.
        Painter.progressArrow(g, x + FLOW_X, y + FLOW_Y, FLOW_W, FLOW_H, charging ? 1 : 0, 1);

        g.text(this.font, Component.translatable("gui.solenoid.recharger.slot"),
                x + 70, y + 60, Theme.TEXT_DIM, false);

        (charging ? MachineStatus.RUNNING
                  : stored <= 0 ? MachineStatus.NO_POWER : MachineStatus.IDLE)
                .draw(g, this.font, x, y, mouseX, mouseY);

        if (Painter.hovering(mouseX, mouseY, x + FLOW_X, y + FLOW_Y, FLOW_W, FLOW_H)) {
            g.setTooltipForNextFrame(Component.translatable(
                    charging ? "gui.solenoid.recharger.charging" : "gui.solenoid.recharger.idle"),
                    mouseX, mouseY);
        }
    }
}
