package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.SolenoidMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MobMagnetMenu extends AbstractContainerMenu {
    private final MobMagnetBlockEntity blockEntity;
    private final ContainerData data;

    public MobMagnetMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (MobMagnetBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(5));
    }

    public MobMagnetMenu(int containerId, Inventory playerInventory, MobMagnetBlockEntity blockEntity, ContainerData data) {
        super(SolenoidMenus.MOB_MAGNET_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    private int combine(int lowIndex, int highIndex) {
        return ((data.get(highIndex) & 0xFFFF) << 16) | (data.get(lowIndex) & 0xFFFF);
    }

    public int getEnergyStored() {
        return combine(0, 1);
    }

    public int getEnergyMax() {
        return combine(2, 3);
    }

    public boolean isActive() {
        return data.get(4) != 0;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, EmfBlocks.MOB_MAGNET.get());
    }

    public MobMagnetBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
