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
import org.jetbrains.annotations.Nullable;

/**
 * EMF Cable. Relays EMF by block adjacency: each server tick it pulls from neighbours that can
 * extract and pushes to neighbours that can receive, up to {@link EmfConstants#CABLE_TRANSFER}
 * EMF/tick per direction. Holds a small internal buffer so in-flight EMF has somewhere to sit, which
 * lets a row of cables form a connected network purely from adjacency.
 *
 * <p>Transfers go through {@link EnergyHandlerUtil#move}, which is transaction-safe and never creates
 * or duplicates energy, so the bidirectional pull/push cannot cause a feedback loop or double-count.
 */
public class EmfCableBlockEntity extends AbstractEmfBlockEntity {
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.CABLE_BUFFER, EmfConstants.CABLE_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public EmfCableBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.EMF_CABLE_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // PULL: draw EMF out of neighbours that can extract (sources, upstream cables) into our buffer.
        int pullBudget = EmfConstants.CABLE_TRANSFER;
        for (Direction dir : Direction.values()) {
            if (pullBudget <= 0) break;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour != null) {
                pullBudget -= EnergyHandlerUtil.move(neighbour, handler, pullBudget, null);
            }
        }

        // PUSH: send buffered EMF on to neighbours that can receive (downstream cables, sinks).
        int pushBudget = EmfConstants.CABLE_TRANSFER;
        for (Direction dir : Direction.values()) {
            if (pushBudget <= 0) break;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour != null) {
                pushBudget -= EnergyHandlerUtil.move(handler, neighbour, pushBudget, null);
            }
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
