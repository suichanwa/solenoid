package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Large EMF storage block. Receives EMF on every face into a
 * {@link EmfConstants#CAPACITOR_CAPACITY} buffer, and each tick pushes up to
 * {@link EmfConstants#CAPACITOR_TRANSFER} EMF (shared fairly across all sides) into adjacent
 * Forge-Energy consumers — i.e. handlers that cannot themselves be extracted from, so we never
 * ping-pong charge back into generators, cables or other batteries. NBT-persisted; exposes a
 * comparator analog signal scaled to fill via {@link CapacitorBlock}.
 */
public class CapacitorBlockEntity extends AbstractEmfBlockEntity {
    // Receive and extract on all sides; extraction is used by our own outward push.
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.CAPACITOR_CAPACITY, EmfConstants.CAPACITOR_TRANSFER, EmfConstants.CAPACITOR_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Stored amount at the end of the previous tick, used to refresh comparators only on change. */
    private int lastEnergy;

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CAPACITOR_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return handler;
    }

    /** Comparator output 0-15 scaled to fill. */
    public int getComparatorOutput() {
        return EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(handler);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        pushToConsumers(level, pos);

        int now = handler.getAmountAsInt();
        if (now != lastEnergy) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            lastEnergy = now;
        }
    }

    /**
     * Pushes up to {@link EmfConstants#CAPACITOR_TRANSFER} EMF total this tick, split fairly among the
     * adjacent pure consumers. Producers/storage (anything that will give energy back) are skipped.
     */
    private void pushToConsumers(ServerLevel level, BlockPos pos) {
        if (handler.getAmountAsInt() <= 0) {
            return;
        }

        List<EnergyHandler> consumers = new ArrayList<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour == null || neighbour == handler) {
                continue;
            }
            if (!canExtract(neighbour)) {
                consumers.add(neighbour);
            }
        }
        if (consumers.isEmpty()) {
            return;
        }

        int budget = EmfConstants.CAPACITOR_TRANSFER;
        int remaining = consumers.size();
        for (EnergyHandler consumer : consumers) {
            if (budget <= 0) {
                break;
            }
            // Even share of the remaining budget across the remaining consumers.
            int share = Math.max(1, budget / remaining);
            int moved = EnergyHandlerUtil.move(handler, consumer, share, null);
            budget -= moved;
            remaining--;
        }
    }

    /** Simulated extract (rolled back) to classify a neighbour as a producer/storage vs a consumer. */
    private static boolean canExtract(EnergyHandler neighbour) {
        try (Transaction transaction = Transaction.openRoot()) {
            return neighbour.extract(1, transaction) > 0;
            // No commit() -> close() rolls the simulation back.
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        handler.serialize(output.child("emf"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        handler.deserialize(input.childOrEmpty("emf"));
        lastEnergy = handler.getAmountAsInt();
    }
}
