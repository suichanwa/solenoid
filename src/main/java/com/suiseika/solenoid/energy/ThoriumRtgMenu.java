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

/**
 * Storage-only menu for the Thorium RTG. Holds no machine slots -- the player inventory is shown
 * purely for context. The synced {@link ContainerData} carries the buffered/max EMF and the
 * remaining fuel (each split across two 16-bit slots, reassembled unsigned), plus the per-tick
 * generation rate in a single slot.
 */
public class ThoriumRtgMenu extends AbstractContainerMenu {
    private final ThoriumRtgBlockEntity blockEntity;
    private final ContainerData data;

    public ThoriumRtgMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (ThoriumRtgBlockEntity) playerInventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(7));
    }

    public ThoriumRtgMenu(int containerId, Inventory playerInventory, ThoriumRtgBlockEntity blockEntity, ContainerData data) {
        super(SolenoidMenus.THORIUM_RTG_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    // ---- Client-readable synced values (large ints split across two short slots, reassembled unsigned) ----

    private int combine(int lowIndex, int highIndex) {
        return ((data.get(highIndex) & 0xFFFF) << 16) | (data.get(lowIndex) & 0xFFFF);
    }

    public int getEnergyStored() {
        return combine(0, 1);
    }

    public int getEnergyMax() {
        return combine(2, 3);
    }

    /** EMF produced this tick (0 once fuel is spent). */
    public int getGenRate() {
        return data.get(4);
    }

    public int getFuel() {
        return combine(5, 6);
    }

    public int getFuelMax() {
        return EmfConstants.RTG_MAX_FUEL;
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
        // No machine slots to shuffle into; shift-click is a no-op.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, EmfBlocks.THORIUM_RTG.get());
    }

    public ThoriumRtgBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
