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
    private final boolean[] forcedDisconnected = new boolean[6];
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.COPPER_CABLE_BUFFER, EmfConstants.COPPER_CABLE_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.COPPER_CABLE_BE.get(), pos, state);
    }

    public boolean toggleSide(Direction dir) {
        forcedDisconnected[dir.ordinal()] = !forcedDisconnected[dir.ordinal()];
        setChanged();
        return forcedDisconnected[dir.ordinal()];
    }

    public boolean isForcedDisconnected(Direction dir) {
        return forcedDisconnected[dir.ordinal()];
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side != null && isForcedDisconnected(side)) {
            return null;
        }
        return handler;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int transfer = EmfConstants.COPPER_CABLE_TRANSFER;

        // 1. Identify neighbors: separate into Producers, Consumers (Machines), and other Cables
        java.util.List<EnergyHandler> producers = new java.util.ArrayList<>();
        java.util.List<EnergyHandler> consumers = new java.util.ArrayList<>();
        java.util.List<EnergyHandler> otherCables = new java.util.ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (isForcedDisconnected(dir)) continue;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour == null || neighbour == handler) continue;

            if (level.getBlockEntity(pos.relative(dir)) instanceof CopperCableBlockEntity) {
                otherCables.add(neighbour);
            } else if (canExtract(neighbour)) {
                producers.add(neighbour);
            } else {
                consumers.add(neighbour);
            }
        }

        // 2. Direct flow: move from producers directly to consumers, bypassing our buffer
        for (EnergyHandler p : producers) {
            for (EnergyHandler c : consumers) {
                EnergyHandlerUtil.move(p, c, transfer, null);
            }
        }

        // 3. Fill our buffer from producers (if consumers are full or missing)
        for (EnergyHandler p : producers) {
            EnergyHandlerUtil.move(p, handler, transfer, null);
        }

        // 4. Drain our buffer into consumers
        for (EnergyHandler c : consumers) {
            EnergyHandlerUtil.move(handler, c, transfer, null);
        }

        // 5. Cable-to-cable equalization (downhill only)
        for (EnergyHandler cable : otherCables) {
            int mine = handler.getAmountAsInt();
            int theirs = cable.getAmountAsInt();
            if (mine > theirs) {
                int amount = Math.min(transfer, (mine - theirs + 1) / 2);
                EnergyHandlerUtil.move(handler, cable, amount, null);
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
        for (int i = 0; i < 6; i++) {
            output.putBoolean("disconnected_" + i, forcedDisconnected[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        handler.deserialize(input.childOrEmpty("emf"));
        for (int i = 0; i < 6; i++) {
            forcedDisconnected[i] = input.getBooleanOr("disconnected_" + i, false);
        }
    }
}
