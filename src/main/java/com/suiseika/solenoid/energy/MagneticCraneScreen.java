package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

public class MagneticCraneScreen extends MachineScreen<MagneticCraneMenu> {

    public MagneticCraneScreen(MagneticCraneMenu menu, Inventory inventory, Component title) {
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
    protected Optional<Class<?>> recipeScreenKey() {
        return Optional.empty();
    }

    @Override
    protected void drawMachine(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        int stored = energyStored();
        boolean active = this.menu.isActive();

        MachineStatus status = stored < MagneticCraneBlockEntity.PULL_TICK_COST
                ? MachineStatus.NO_POWER
                : (active ? MachineStatus.RUNNING : MachineStatus.IDLE);

        status.draw(g, this.font, x, y, mouseX, mouseY);
    }
}
