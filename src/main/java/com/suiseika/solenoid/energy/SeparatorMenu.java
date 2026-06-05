package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.SolenoidMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class SeparatorMenu extends AbstractContainerMenu {
    private final SeparatorBlockEntity blockEntity;

    public SeparatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, (SeparatorBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public SeparatorMenu(int containerId, Inventory playerInventory, SeparatorBlockEntity blockEntity) {
        super(SolenoidMenus.SEPARATOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        var handler = blockEntity.getItemHandler();
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 0, 56, 35));
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 1, 116, 35));
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 2, 116, 53));

        addPlayerInventory(playerInventory);
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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(itemstack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, EmfBlocks.SEPARATOR.get());
    }

    public SeparatorBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
