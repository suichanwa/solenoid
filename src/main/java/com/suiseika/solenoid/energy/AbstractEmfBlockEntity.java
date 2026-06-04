package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for every EMF block entity. Holds the {@link EnergyHandler} that is exposed as the
 * {@code Capabilities.Energy.BLOCK} capability and provides the server-tick plumbing plus the
 * side-aware neighbour capability lookup used by all transfer logic.
 */
public abstract class AbstractEmfBlockEntity extends BlockEntity {
    protected AbstractEmfBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * The energy handler exposed via capability for the given side ({@code null} = no specific side).
     * Returned to other blocks/mods as the native Forge Energy handler.
     */
    public abstract @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side);

    /** Per-block server-tick behaviour (push / relay / receive). */
    protected abstract void serverTick(ServerLevel level, BlockPos pos, BlockState state);

    /**
     * Ticker entry point wired up by each block's {@code getTicker}. Server side only; the block
     * already guards against the client, but we re-check defensively.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractEmfBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos, state);
        }
    }

    /**
     * Looks up the EMF/Forge-Energy handler of the neighbour on the touching face. Asks the neighbour
     * for the capability on {@code dir.getOpposite()} — i.e. the face that actually touches us.
     */
    protected static @Nullable EnergyHandler neighbourHandler(ServerLevel level, BlockPos pos, Direction dir) {
        return level.getCapability(Capabilities.Energy.BLOCK, pos.relative(dir), dir.getOpposite());
    }
}
