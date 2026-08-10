package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import com.suiseika.solenoid.client.ui.Painter;
import com.suiseika.solenoid.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Digester console: feedstock over reagent on the left, three products on the right. The two input
 * slots are captioned so the reagent slot is not mistaken for a second feedstock.
 */
public class DigesterScreen extends MachineScreen<DigesterMenu> {
    /** Public so the JEI plugin can hang its recipe click area on the exact same rectangle. */
    public static final int ARROW_X = 70, ARROW_Y = 44, ARROW_W = 24, ARROW_H = 16;

    public DigesterScreen(DigesterMenu menu, Inventory inventory, Component title) {
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

    @Override
    protected int[] outputSlots() {
        return new int[] {2, 3, 4};
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();

        Painter.progressArrow(g, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, progress, maxProgress);
        MachineStatus.draw(g, this.font, x, y, mouseX, mouseY, energyStored(), progress, maxProgress);

        // Captions above each input column, so feedstock and reagent are unambiguous.
        // Feed slot recess spans y34-52, reagent y52-70; captions clear both.
        g.text(this.font, Component.translatable("gui.solenoid.digester.feed"),
                x + 42, y + 25, Theme.TEXT_DIM, false);
        g.text(this.font, Component.translatable("gui.solenoid.digester.reagent"),
                x + 42, y + 71, Theme.TEXT_DIM, false);

        if (Painter.hovering(mouseX, mouseY, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H)) {
            g.setTooltipForNextFrame(
                    Component.literal(Painter.percent(progress, maxProgress) + "%"), mouseX, mouseY);
        }
    }
}
