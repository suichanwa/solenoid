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
        // PULL
        int pullBudget = EmfConstants.COPPER_CABLE_TRANSFER;
        for (Direction dir : Direction.values()) {
            if (pullBudget <= 0) break;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour != null) {
                pullBudget -= (int) EnergyHandlerUtil.move(neighbour, handler, pullBudget, null);
            }
        }

        // PUSH
        int pushBudget = EmfConstants.COPPER_CABLE_TRANSFER;
        for (Direction dir : Direction.values()) {
            if (pushBudget <= 0) break;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour != null) {
                pushBudget -= (int) EnergyHandlerUtil.move(handler, neighbour, pushBudget, null);
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
