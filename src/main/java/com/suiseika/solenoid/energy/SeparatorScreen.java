package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import com.suiseika.solenoid.client.ui.Painter;
import com.suiseika.solenoid.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Separator console: one input, two outputs, plus a magnetic-field lamp reporting whether the
 * separating coil is energised.
 */
public class SeparatorScreen extends MachineScreen<SeparatorMenu> {
    /** Public so the JEI plugin can hang its recipe click area on the exact same rectangle. */
    public static final int ARROW_X = 70, ARROW_Y = 34, ARROW_W = 24, ARROW_H = 16;
    /** Magnetic-field lamp, sat in the machine well's free bottom-left corner. */
    private static final int FIELD_X = 34, FIELD_Y = 66, FIELD_SIZE = 6;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
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
        return new int[] {1, 2};
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();

        Painter.progressArrow(g, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, progress, maxProgress);
        MachineStatus.draw(g, this.font, x, y, mouseX, mouseY, energyStored(), progress, maxProgress);

        boolean fieldActive = this.menu.isFieldActive();
        int fx = x + FIELD_X, fy = y + FIELD_Y;
        Painter.lamp(g, fx, fy, FIELD_SIZE, Theme.EMF, fieldActive);
        g.text(this.font, Component.translatable("gui.solenoid.separator.field"),
                fx + 10, fy - 1, fieldActive ? Theme.TEXT : Theme.TEXT_FAINT, false);

        if (Painter.hovering(mouseX, mouseY, fx, fy, FIELD_SIZE, FIELD_SIZE)) {
            g.setTooltipForNextFrame(Component.translatable(
                    fieldActive ? "gui.solenoid.separator.field.active"
                                : "gui.solenoid.separator.field.inactive"), mouseX, mouseY);
        } else if (Painter.hovering(mouseX, mouseY, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H)) {
            g.setTooltipForNextFrame(
                    Component.literal(Painter.percent(progress, maxProgress) + "%"), mouseX, mouseY);
        }
    }
}
