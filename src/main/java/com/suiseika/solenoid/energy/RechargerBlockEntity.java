package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * Charging station. Receives EMF from adjacent cables into a {@link EmfConstants#RECHARGER_CAPACITY}
 * buffer (receive-only: cables cannot pull it back out), and each tick pumps up to
 * {@link EmfConstants#RECHARGER_TRANSFER} EMF into a single energy-buffer item placed in its slot via
 * the item's {@link Capabilities.Energy#ITEM} handler. The item's stored EMF persists through the
 * {@link ItemAccess} backing the slot — the same DataComponent path the batteries/charm use to charge
 * in the Capacitor — so there is no divergent persistence logic.
 */
public class RechargerBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {

    private static class RechargerItemHandler extends ItemStacksResourceHandler {
        private final RechargerBlockEntity be;

        RechargerItemHandler(RechargerBlockEntity be) {
            super(1);
            this.be = be;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            be.setChanged();
        }

        ItemStack getStack(int slot) {
            return this.stacks.get(slot);
        }
    }

    private final RechargerItemHandler itemHandler = new RechargerItemHandler(this);

    /** Receive-only buffer: maxInsert lets cables feed it, maxExtract = 0 stops cables draining it. */
    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(
            EmfConstants.RECHARGER_CAPACITY, EmfConstants.RECHARGER_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyHandler.getAmountAsInt();
            int capacity = energyHandler.getCapacityAsInt();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server reads from the block entity; the synced copy lives client-side in the menu.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public RechargerBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.RECHARGER_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }

    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    public ItemStacksResourceHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.recharger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RechargerMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int stored = energyHandler.getAmountAsInt();
        if (stored <= 0) {
            return;
        }

        ItemStack stack = itemHandler.getStack(0);
        if (stack.isEmpty()) {
            return;
        }

        // ItemAccess.forHandlerIndex writes the item's energy DataComponent back into the slot stack,
        // so the charge persists exactly like batteries charged inside the Capacitor.
        ItemAccess access = ItemAccess.forHandlerIndex(itemHandler, 0);
        EnergyHandler itemEnergy = Capabilities.Energy.ITEM.getCapability(stack, access);
        if (itemEnergy == null) {
            return;
        }

        int toMove = Math.min(EmfConstants.RECHARGER_TRANSFER, stored);
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = itemEnergy.insert(toMove, transaction);
            if (inserted > 0) {
                transaction.commit();
                energyHandler.set(stored - inserted);
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
        energyHandler.serialize(output.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input.childOrEmpty("inventory"));
        energyHandler.deserialize(input.childOrEmpty("energy"));
    }
}
