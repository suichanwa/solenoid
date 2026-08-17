package com.suiseika.solenoid.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Supplier;

/**
 * Wraps an EnergyHandler and filters energy insert/extract based on the configured MachineSideMode.
 */
public class SidedEnergyHandler implements EnergyHandler {
    private final EnergyHandler backing;
    private final Supplier<MachineSideMode> modeSupplier;

    public SidedEnergyHandler(EnergyHandler backing, Supplier<MachineSideMode> modeSupplier) {
        this.backing = backing;
        this.modeSupplier = modeSupplier;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        MachineSideMode mode = modeSupplier.get();
        if (mode != MachineSideMode.INPUT && mode != MachineSideMode.BOTH) {
            return 0;
        }
        return backing.insert(amount, transaction);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        MachineSideMode mode = modeSupplier.get();
        if (mode != MachineSideMode.OUTPUT && mode != MachineSideMode.BOTH) {
            return 0;
        }
        return backing.extract(amount, transaction);
    }

    @Override
    public int getAmountAsInt() {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) {
            return 0;
        }
        return backing.getAmountAsInt();
    }

    @Override
    public int getCapacityAsInt() {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) {
            return 0;
        }
        return backing.getCapacityAsInt();
    }

    @Override
    public long getAmountAsLong() {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) {
            return 0;
        }
        return backing.getAmountAsLong();
    }

    @Override
    public long getCapacityAsLong() {
        MachineSideMode mode = modeSupplier.get();
        if (mode == MachineSideMode.DISABLED) {
            return 0;
        }
        return backing.getCapacityAsLong();
    }
}
