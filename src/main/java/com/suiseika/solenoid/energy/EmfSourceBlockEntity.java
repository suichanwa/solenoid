package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.InfiniteEnergyHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Creative EMF Source. Pushes a constant {@link EmfConstants#SOURCE_OUTPUT} EMF/tick into every
 * adjacent block that accepts EMF. Infinite — never depletes, stores nothing, no NBT, no GUI.
 */
public class EmfSourceBlockEntity extends AbstractEmfBlockEntity {
    /** Infinite Forge Energy supply: extract() always yields the full request, insert() rejects. */
    private final EnergyHandler handler = InfiniteEnergyHandler.INSTANCE;

    public EmfSourceBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.EMF_SOURCE_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour != null) {
                // null transaction => move() opens and commits its own.
                EnergyHandlerUtil.move(handler, neighbour, EmfConstants.SOURCE_OUTPUT, null);
            }
        }
    }
}
