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

/**
 * Copper Cable. Thin version of the EMF relay.
 * Transfers up to {@link EmfConstants#COPPER_CABLE_TRANSFER} EMF/tick.
 */
public class CopperCableBlockEntity extends AbstractEmfBlockEntity {
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.CABLE_BUFFER, EmfConstants.COPPER_CABLE_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.COPPER_CABLE_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int transfer = EmfConstants.COPPER_CABLE_TRANSFER;

        for (Direction dir : Direction.values()) {
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour == null || neighbour == handler) {
                continue;
            }

            if (level.getBlockEntity(pos.relative(dir)) instanceof CopperCableBlockEntity) {
                // Cable -> cable: equalise charge, but only ever flow DOWNHILL (toward the lower
                // buffer) and move just half the difference. This drains a multi-cable run toward
                // whichever end a machine is pulling from, instead of two cables shoving the same
                // packet back and forth each tick (which left the far machine unpowered).
                int mine = handler.getAmountAsInt();
                int theirs = neighbour.getAmountAsInt();
                if (mine > theirs) {
                    int amount = Math.min(transfer, (mine - theirs + 1) / 2);
                    EnergyHandlerUtil.move(handler, neighbour, amount, null);
                }
            } else {
                // Pull from anything that will give (sources, generators, foreign FE producers).
                EnergyHandlerUtil.move(neighbour, handler, transfer, null);
                // Only push into pure consumers (machines, sink). Never push into something that can
                // extract — that would just dump our charge back into a source/generator/battery and
                // ping-pong with it.
                if (!canExtract(neighbour)) {
                    EnergyHandlerUtil.move(handler, neighbour, transfer, null);
                }
            }
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
    }
}
