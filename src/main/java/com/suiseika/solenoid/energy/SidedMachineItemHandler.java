package com.suiseika.solenoid.energy;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Supplier;

/**
 * Wraps an ItemStacksResourceHandler and filters slot access based on the current MachineSideMode.
 */
public class SidedMachineItemHandler implements ResourceHandler<ItemResource> {
    private final ItemStacksResourceHandler backing;
    private final Supplier<MachineSideMode> modeSupplier;
    private final int[] inputSlots;
    private final int[] outputSlots;

    public SidedMachineItemHandler(ItemStacksResourceHandler backing, Supplier<MachineSideMode> modeSupplier, int[] inputSlots, int[] outputSlots) {
        this.backing = backing;
        this.modeSupplier = modeSupplier;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public ItemResource getResource(int slot) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) return ItemResource.EMPTY;
        return backing.getResource(slot);
    }

    @Override
    public long getAmountAsLong(int slot) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) return 0;
        return backing.getAmountAsLong(slot);
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) return 0;
        return backing.getCapacityAsLong(slot, resource);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED || mode == MachineSideMode.OUTPUT) return false;
        if (mode == MachineSideMode.INPUT || mode == MachineSideMode.BOTH) {
            for (int s : inputSlots) {
                if (s == slot) return backing.isValid(slot, resource);
            }
        }
        return false;
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED || mode == MachineSideMode.OUTPUT) {
            return 0;
        }

        for (int s : inputSlots) {
            if (s == slot) {
                return backing.insert(slot, resource, amount, transaction);
            }
        }
        return 0;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED || mode == MachineSideMode.INPUT) {
            return 0;
        }

        for (int s : outputSlots) {
            if (s == slot) {
                return backing.extract(slot, resource, amount, transaction);
            }
        }
        return 0;
    }
}
