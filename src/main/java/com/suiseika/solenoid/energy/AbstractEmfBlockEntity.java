package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for every EMF block entity. Holds the {@link EnergyHandler} that is exposed as the
 * {@code Capabilities.Energy.BLOCK} capability and provides the server-tick plumbing, side-aware
 * item IO routing, auto-ejection, and capability lookups.
 */
public abstract class AbstractEmfBlockEntity extends BlockEntity {
    protected final MachineSideMode[] sideModes = new MachineSideMode[6];
    protected boolean autoEject = true;

    protected AbstractEmfBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < 6; i++) {
            sideModes[i] = MachineSideMode.BOTH;
        }
    }

    public MachineSideMode getSideMode(RelativeSide side) {
        return sideModes[side.ordinal()];
    }

    public void setSideMode(RelativeSide side, MachineSideMode mode) {
        sideModes[side.ordinal()] = mode;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public MachineSideMode cycleSideMode(RelativeSide side) {
        sideModes[side.ordinal()] = sideModes[side.ordinal()].next();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return sideModes[side.ordinal()];
    }

    public boolean isAutoEject() {
        return autoEject;
    }

    public boolean toggleAutoEject() {
        autoEject = !autoEject;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return autoEject;
    }

    public int[] getInputSlots() {
        return new int[0];
    }

    public int[] getOutputSlots() {
        return new int[0];
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    public @Nullable ItemStacksResourceHandler getItemHandler() {
        return null;
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        ItemStacksResourceHandler raw = getItemHandler();
        if (raw == null) return null;
        if (side == null) return raw;

        RelativeSide relSide = RelativeSide.fromDirection(side, getFacing());
        return new SidedMachineItemHandler(raw, () -> sideModes[relSide.ordinal()], getInputSlots(), getOutputSlots());
    }

    public void autoEjectOutputs(ServerLevel level, BlockPos pos) {
        if (!autoEject) return;
        ItemStacksResourceHandler handler = getItemHandler();
        if (handler == null) return;
        int[] outSlots = getOutputSlots();
        if (outSlots.length == 0) return;

        Direction facing = getFacing();
        for (RelativeSide relSide : RelativeSide.values()) {
            MachineSideMode mode = sideModes[relSide.ordinal()];
            if (mode != MachineSideMode.OUTPUT && mode != MachineSideMode.BOTH) {
                continue;
            }

            Direction worldDir = relSide.toDirection(facing);
            BlockPos targetPos = pos.relative(worldDir);
            Direction targetSide = worldDir.getOpposite();

            ResourceHandler<ItemResource> targetHandler = level.getCapability(Capabilities.Item.BLOCK, targetPos, targetSide);
            if (targetHandler == null) continue;

            for (int slot : outSlots) {
                if (slot < 0 || slot >= handler.size()) continue;
                ItemResource resource = handler.getResource(slot);
                if (resource.isEmpty()) continue;

                try (Transaction tx = Transaction.openRoot()) {
                    int available = (int) handler.getAmountAsLong(slot);
                    int inserted = targetHandler.insert(resource, available, tx);
                    if (inserted > 0) {
                        handler.extract(slot, resource, inserted, tx);
                        tx.commit();
                        setChanged();
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        saveSideConfig(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadSideConfig(input);
    }

    protected void saveSideConfig(ValueOutput output) {
        for (int i = 0; i < 6; i++) {
            output.putInt("side_mode_" + i, sideModes[i].ordinal());
        }
        output.putBoolean("auto_eject", autoEject);
    }

    protected void loadSideConfig(ValueInput input) {
        for (int i = 0; i < 6; i++) {
            int m = input.getIntOr("side_mode_" + i, 0);
            sideModes[i] = MachineSideMode.values()[Math.min(Math.max(0, m), MachineSideMode.values().length - 1)];
        }
        autoEject = input.getBooleanOr("auto_eject", true);
    }

    /**
     * The energy handler exposed via capability for the given side ({@code null} = no specific side).
     * Returned to other blocks/mods as the native Forge Energy handler.
     */
    public abstract @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side);

    /**
     * Energy currently consumed or generated per tick by this machine when active.
     * Returns 0 if the machine is idle, unpowered, or not actively using energy.
     */
    public int getEnergyUsage() {
        return 0;
    }

    /**
     * Processing progress of the machine in ticks (for multimeters/diagnostics).
     * Returns -1 if this machine does not have a progressive crafting process.
     */
    public int getProgress() {
        return -1;
    }

    /**
     * Total ticks required to complete the current processing operation.
     * Returns -1 if this machine does not have a progressive crafting process.
     */
    public int getMaxProgress() {
        return -1;
    }

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
