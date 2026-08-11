package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.client.ui.MachineScreen;
import com.suiseika.solenoid.client.ui.MachineStatus;
import com.suiseika.solenoid.client.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

public class MobMagnetScreen extends MachineScreen<MobMagnetMenu> {

    public MobMagnetScreen(MobMagnetMenu menu, Inventory inventory, Component title) {
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

        // Info text inside machine well
        g.text(this.font, Component.translatable("gui.solenoid.mob_magnet.radius", (int) MobMagnetBlockEntity.RADIUS),
                x + 32, y + 30, Theme.TEXT, false);
        g.text(this.font, Component.translatable("gui.solenoid.mob_magnet.cost", MobMagnetBlockEntity.PULL_COST_PER_TICK),
                x + 32, y + 44, Theme.TEXT_DIM, false);

        MachineStatus status = stored < MobMagnetBlockEntity.PULL_COST_PER_TICK
                ? MachineStatus.NO_POWER
                : (active ? MachineStatus.RUNNING : MachineStatus.IDLE);

        status.draw(g, this.font, x, y, mouseX, mouseY);
    }
}
