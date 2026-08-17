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
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class ChemicalReactorMenu extends AbstractContainerMenu implements ISidedMachineMenu {
    private final ChemicalReactorBlockEntity blockEntity;
    private final ContainerData data;

    private static final int SLOT_COUNT = 2;

    public ChemicalReactorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (ChemicalReactorBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(14));
    }

    public ChemicalReactorMenu(int containerId, Inventory playerInventory, ChemicalReactorBlockEntity blockEntity, ContainerData data) {
        super(SolenoidMenus.CHEMICAL_REACTOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        var handler = blockEntity.getItemHandler();
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 0, 56, 44));
        this.addSlot(new ResourceHandlerSlot(handler, handler::set, 1, 116, 44));

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

    public int getProgress() {
        return data.get(4);
    }

    public int getMaxProgress() {
        int max = data.get(5);
        return max <= 0 ? 1 : max;
    }

    public boolean isWorking() {
        return data.get(6) != 0;
    }

    @Override
    public MachineSideMode getSideMode(RelativeSide side) {
        int ordinal = data.get(7 + side.ordinal());
        return MachineSideMode.values()[Math.min(Math.max(0, ordinal), MachineSideMode.values().length - 1)];
    }

    @Override
    public boolean isAutoEject() {
        return data.get(13) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 6) {
            blockEntity.cycleSideMode(RelativeSide.values()[id]);
            return true;
        } else if (id == 6) {
            blockEntity.toggleAutoEject();
            return true;
        }
        return false;
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
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(itemstack1, SLOT_COUNT, this.slots.size(), true)) {
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, EmfBlocks.CHEMICAL_REACTOR.get());
    }

    public ChemicalReactorBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
