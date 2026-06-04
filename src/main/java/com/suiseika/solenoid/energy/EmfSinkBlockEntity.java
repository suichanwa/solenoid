package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jetbrains.annotations.Nullable;

/**
 * EMF Sink. Receives up to {@link EmfConstants#SINK_INPUT} EMF/tick into a
 * {@link EmfConstants#SINK_CAPACITY} buffer and cannot extract. Surfaces its charge two ways:
 * a comparator-readable analog output scaled 0-15, and a "lit" blockstate that is true while it is
 * actively receiving EMF.
 */
public class EmfSinkBlockEntity extends AbstractEmfBlockEntity {
    // maxInsert = SINK_INPUT, maxExtract = 0 -> receive-only.
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.SINK_CAPACITY, EmfConstants.SINK_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Stored amount at the end of the previous tick, used to detect "actively receiving". */
    private int lastEnergy;

    public EmfSinkBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.EMF_SINK_BE.get(), pos, state);
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
        int now = handler.getAmountAsInt();
        boolean receiving = now > lastEnergy;

        if (now != lastEnergy) {
            // Charge changed -> refresh any comparator reading us.
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        lastEnergy = now;

        if (state.getValue(BlockStateProperties.LIT) != receiving) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, receiving), Block.UPDATE_ALL);
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
