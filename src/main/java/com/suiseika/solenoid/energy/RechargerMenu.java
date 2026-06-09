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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Recharger menu: one charge slot (restricted to items that expose the {@code Energy.ITEM} capability)
 * plus the player inventory. The block's EMF is synced through a {@link ContainerData} split across two
 * 16-bit slots and reassembled unsigned, exactly like the other machine menus.
 */
public class RechargerMenu extends AbstractContainerMenu {
    private final RechargerBlockEntity blockEntity;
    private final ContainerData data;

    public RechargerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (RechargerBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(4));
    }

    public RechargerMenu(int containerId, Inventory playerInventory, RechargerBlockEntity blockEntity, ContainerData data) {
        super(SolenoidMenus.RECHARGER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        var handler = blockEntity.getItemHandler();
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty()
                        && Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack)) != null;
            }
        });

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    // ---- Client-readable synced values (energy split across two short slots, reassembled unsigned) ----

    private int combine(int lowIndex, int highIndex) {
        return ((data.get(highIndex) & 0xFFFF) << 16) | (data.get(lowIndex) & 0xFFFF);
    }

    public int getEnergyStored() {
        return combine(0, 1);
    }

    public int getEnergyMax() {
        return combine(2, 3);
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
            if (index < 1) {
                // Charge slot -> player inventory.
                if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                // Player inventory -> charge slot.
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, EmfBlocks.RECHARGER.get());
    }

    public RechargerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
