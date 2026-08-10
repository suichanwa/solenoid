package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import com.suiseika.solenoid.client.ui.Painter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Crusher console: single input, single output, EMF gauge and progress chevron. */
public class CrusherScreen extends MachineScreen<CrusherMenu> {
    /** Public so the JEI plugin can hang its recipe click area on the exact same rectangle. */
    public static final int ARROW_X = 79, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 16;

    public CrusherScreen(CrusherMenu menu, Inventory inventory, Component title) {
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
        return new int[] {1};
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();

        Painter.progressArrow(g, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, progress, maxProgress);
        MachineStatus.draw(g, this.font, x, y, mouseX, mouseY, energyStored(), progress, maxProgress);

        if (Painter.hovering(mouseX, mouseY, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H)) {
            g.setTooltipForNextFrame(
                    Component.literal(Painter.percent(progress, maxProgress) + "%"), mouseX, mouseY);
        }
    }
}
