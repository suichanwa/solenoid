package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Vacuum Tube Block Entity.
 * Handles high-speed item routing, active extraction (configurable rate/speed), insertion, and disconnect states.
 * Uses BFS reachability validation to ensure items are never extracted or accepted unless a valid accepting destination exists.
 */
public class VacuumTubeBlockEntity extends AbstractEmfBlockEntity {
    public static final int MAX_TRAVELING = 4;
    public static final int ENERGY_CAPACITY = 2000;
    public static final int ENERGY_TRANSFER = 200;

    public static class TubeItemHandler extends ItemStacksResourceHandler {
        private final VacuumTubeBlockEntity be;

        public TubeItemHandler(VacuumTubeBlockEntity be, int size) {
            super(size);
            this.be = be;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            be.setChanged();
        }

        public ItemStack getStack(int slot) {
            return this.stacks.get(slot);
        }

        public void setStack(int slot, ItemStack stack) {
            this.stacks.set(slot, stack);
            onContentsChanged(slot, stack);
        }
    }

    /**
     * Sided item capability handler allowing external blocks (machines with auto-eject, hoppers)
     * to push items directly into the vacuum tube, only if a valid destination container is reachable.
     */
    public class TubeInputHandler implements ResourceHandler<ItemResource> {
        private final Direction side;

        public TubeInputHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int slot) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int slot) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            return 64;
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return !isDisconnected(side) && sideModes[side.ordinal()] != TubeMode.EXTRACT;
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            if (isDisconnected(side) || sideModes[side.ordinal()] == TubeMode.EXTRACT || resource.isEmpty() || amount <= 0) {
                return 0;
            }

            int freeSlot = -1;
            for (int i = 0; i < MAX_TRAVELING; i++) {
                if (travelingStacks.getStack(i).isEmpty()) {
                    freeSlot = i;
                    break;
                }
            }
            if (freeSlot == -1) return 0;

            int toAccept = Math.min(amount, Config.TUBE_EXTRACT_AMOUNT.getAsInt());
            ItemStack stack = resource.toStack(toAccept);
            Direction toDir = pickOutgoingDirection(level, getBlockPos(), side, stack, transaction);
            if (toDir == null) {
                return 0; // Reject if no valid destination container is reachable
            }

            int inserted = travelingStacks.insert(freeSlot, resource, toAccept, transaction);
            if (inserted > 0) {
                fromDirs[freeSlot] = side;
                toDirs[freeSlot] = toDir;
                progress[freeSlot] = 0.0f;
                prevProgress[freeSlot] = 0.0f;
                setChanged();
                return inserted;
            }

            return 0;
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    private final TubeMode[] sideModes = new TubeMode[6];
    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(ENERGY_CAPACITY, ENERGY_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final TubeItemHandler travelingStacks = new TubeItemHandler(this, MAX_TRAVELING);

    private final Direction[] fromDirs = new Direction[MAX_TRAVELING];
    private final Direction[] toDirs = new Direction[MAX_TRAVELING];
    private final float[] progress = new float[MAX_TRAVELING];
    private final float[] prevProgress = new float[MAX_TRAVELING];

    private int extractCooldown = 0;

    public VacuumTubeBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.VACUUM_TUBE_BE.get(), pos, state);
        for (int i = 0; i < 6; i++) {
            sideModes[i] = TubeMode.INSERT;
        }
    }

    public TubeMode getSideMode(Direction dir) {
        return sideModes[dir.ordinal()];
    }

    public TubeMode cycleSideMode(Direction dir) {
        sideModes[dir.ordinal()] = sideModes[dir.ordinal()].next();
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState oldState = getBlockState();
            if (oldState.getBlock() instanceof VacuumTubeBlock tubeBlock) {
                BlockState newState = oldState.setValue(VacuumTubeBlock.PROPERTY_MAP.get(dir), tubeBlock.getConnection(level, getBlockPos(), dir));
                if (newState != oldState) {
                    level.setBlock(getBlockPos(), newState, 3);
                }
            }
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            BlockPos neighborPos = getBlockPos().relative(dir);
            level.updateNeighborsAt(neighborPos, getBlockState().getBlock());

            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof VacuumTubeBlock neighborTube) {
                BlockState newNeighborState = neighborState.setValue(VacuumTubeBlock.PROPERTY_MAP.get(dir.getOpposite()), neighborTube.getConnection(level, neighborPos, dir.getOpposite()));
                if (newNeighborState != neighborState) {
                    level.setBlock(neighborPos, newNeighborState, 3);
                }
                level.sendBlockUpdated(neighborPos, newNeighborState, newNeighborState, 3);
            }
        }
        return sideModes[dir.ordinal()];
    }

    public boolean isDisconnected(Direction dir) {
        return sideModes[dir.ordinal()] == TubeMode.DISCONNECT;
    }

    public ItemStack getTravelingStack(int slot) {
        return travelingStacks.getStack(slot);
    }

    public @Nullable Direction getFromDir(int slot) {
        return fromDirs[slot];
    }

    public @Nullable Direction getToDir(int slot) {
        return toDirs[slot];
    }

    public float getProgress(int slot, float partialTick) {
        return prevProgress[slot] + (progress[slot] - prevProgress[slot]) * partialTick;
    }

    public boolean addItem(ItemStack stack, @Nullable Direction from, Direction to) {
        for (int i = 0; i < MAX_TRAVELING; i++) {
            if (travelingStacks.getStack(i).isEmpty()) {
                travelingStacks.setStack(i, stack.copy());
                fromDirs[i] = from;
                toDirs[i] = to;
                progress[i] = 0.0f;
                prevProgress[i] = 0.0f;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Node for BFS tube network pathfinding.
     */
    private record PathNode(BlockPos pos, Direction initialDir, Direction fromDir) {}

    public @Nullable Direction pickOutgoingDirection(Level level, BlockPos currentPos, @Nullable Direction from, ItemStack stack) {
        return pickOutgoingDirection(level, currentPos, from, stack, null);
    }

    /**
     * Finds the first outgoing direction from currentPos that leads to a reachable container
     * capable of accepting the stack. Returns null if no reachable container has room.
     */
    public @Nullable Direction pickOutgoingDirection(Level level, BlockPos currentPos, @Nullable Direction from, ItemStack stack, @Nullable TransactionContext parentTransaction) {
        if (stack.isEmpty()) return null;

        Queue<PathNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(currentPos);

        // 1. Check direct neighbors from currentPos
        for (Direction dir : Direction.values()) {
            if (dir == from || isDisconnected(dir) || sideModes[dir.ordinal()] == TubeMode.EXTRACT) {
                continue;
            }

            BlockPos neighborPos = currentPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof VacuumTubeBlock) {
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be instanceof VacuumTubeBlockEntity otherTube && !otherTube.isDisconnected(dir.getOpposite())) {
                    visited.add(neighborPos);
                    queue.add(new PathNode(neighborPos, dir, dir.getOpposite()));
                }
            } else if (sideModes[dir.ordinal()] == TubeMode.INSERT) {
                if (canContainerAccept(level, neighborPos, dir.getOpposite(), stack, parentTransaction)) {
                    return dir;
                }
            }
        }

        // 2. Search deeper through connected tubes (BFS up to 64 blocks)
        int steps = 0;
        while (!queue.isEmpty() && steps < 64) {
            PathNode current = queue.poll();
            steps++;

            BlockEntity be = level.getBlockEntity(current.pos());
            if (!(be instanceof VacuumTubeBlockEntity tube)) continue;

            for (Direction dir : Direction.values()) {
                if (dir == current.fromDir() || tube.isDisconnected(dir) || tube.getSideMode(dir) == TubeMode.EXTRACT) {
                    continue;
                }

                BlockPos neighborPos = current.pos().relative(dir);
                if (visited.contains(neighborPos)) continue;

                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof VacuumTubeBlock) {
                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                    if (neighborBe instanceof VacuumTubeBlockEntity otherTube && !otherTube.isDisconnected(dir.getOpposite())) {
                        visited.add(neighborPos);
                        queue.add(new PathNode(neighborPos, current.initialDir(), dir.getOpposite()));
                    }
                } else if (tube.getSideMode(dir) == TubeMode.INSERT) {
                    if (canContainerAccept(level, neighborPos, dir.getOpposite(), stack, parentTransaction)) {
                        return current.initialDir();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Simulates insertion into a destination container to check if it has space for the stack.
     */
    private static boolean canContainerAccept(Level level, BlockPos pos, Direction side, ItemStack stack, @Nullable TransactionContext parentTransaction) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler == null) {
            handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        }
        if (handler == null) return false;

        try (Transaction tx = Transaction.open(parentTransaction)) {
            ItemResource resource = ItemResource.of(stack);
            int count = stack.getCount();
            int inserted = handler.insert(resource, count, tx);
            if (inserted > 0) return true;

            for (int s = 0; s < handler.size(); s++) {
                if (handler.insert(s, resource, count, tx) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side != null && isDisconnected(side)) {
            return null;
        }
        return energyHandler;
    }

    @Override
    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        if (side == null || isDisconnected(side) || sideModes[side.ordinal()] == TubeMode.EXTRACT) {
            return null;
        }
        return new TubeInputHandler(side);
    }

    @Override
    public int getEnergyUsage() {
        boolean hasItems = false;
        for (int i = 0; i < MAX_TRAVELING; i++) {
            if (!travelingStacks.getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        return (hasItems && energyHandler.getAmountAsInt() > 0) ? 2 : 0;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // 1. Distribute / pull EMF
        for (Direction dir : Direction.values()) {
            if (isDisconnected(dir)) continue;
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour == null || neighbour == energyHandler) continue;
            EnergyHandlerUtil.move(neighbour, energyHandler, ENERGY_TRANSFER, null);
        }

        // 2. Extract from adjacent inventories on EXTRACT faces
        int extractAmount = Config.TUBE_EXTRACT_AMOUNT.getAsInt();
        int extractInterval = Config.TUBE_EXTRACT_INTERVAL_TICKS.getAsInt();
        int energyPerExtract = Config.TUBE_ENERGY_PER_EXTRACT.getAsInt();
        int energyPerTransit = Config.TUBE_ENERGY_PER_TRANSIT.getAsInt();

        if (extractCooldown > 0) {
            extractCooldown--;
        } else {
            for (Direction dir : Direction.values()) {
                if (sideModes[dir.ordinal()] != TubeMode.EXTRACT) continue;

                ResourceHandler<ItemResource> neighbour = level.getCapability(Capabilities.Item.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour == null) {
                    neighbour = level.getCapability(Capabilities.Item.BLOCK, pos.relative(dir), null);
                }
                if (neighbour == null) continue;

                // Find first free slot in tube
                boolean hasFreeSlot = false;
                for (int i = 0; i < MAX_TRAVELING; i++) {
                    if (travelingStacks.getStack(i).isEmpty()) {
                        hasFreeSlot = true;
                        break;
                    }
                }
                if (!hasFreeSlot) break;

                // Extract only if a valid destination container is reachable
                try (Transaction tx = Transaction.openRoot()) {
                    for (int slot = 0; slot < neighbour.size(); slot++) {
                        ItemResource resource = neighbour.getResource(slot);
                        if (resource.isEmpty()) continue;

                        int extracted = neighbour.extract(slot, resource, extractAmount, tx);
                        if (extracted > 0) {
                            ItemStack extractedStack = resource.toStack(extracted);
                            Direction toDir = pickOutgoingDirection(level, pos, dir, extractedStack, tx);
                            if (toDir != null) {
                                tx.commit();
                                addItem(extractedStack, dir, toDir);
                                if (energyHandler.getAmountAsInt() >= energyPerExtract) {
                                    energyHandler.set(energyHandler.getAmountAsInt() - energyPerExtract);
                                }
                                extractCooldown = extractInterval;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. Advance traveling items
        boolean dirty = false;
        boolean hasEnergy = energyHandler.getAmountAsInt() >= energyPerTransit;
        float speed = hasEnergy
                ? (float) Config.TUBE_POWERED_SPEED.getAsDouble()
                : (float) Config.TUBE_UNPOWERED_SPEED.getAsDouble();

        for (int i = 0; i < MAX_TRAVELING; i++) {
            ItemStack stack = travelingStacks.getStack(i);
            if (stack.isEmpty()) continue;

            prevProgress[i] = progress[i];
            progress[i] += speed;

            if (hasEnergy && energyPerTransit > 0 && energyHandler.getAmountAsInt() >= energyPerTransit) {
                energyHandler.set(energyHandler.getAmountAsInt() - energyPerTransit);
            }

            if (progress[i] >= 1.0f) {
                Direction to = toDirs[i];
                if (to == null) {
                    to = pickOutgoingDirection(level, pos, fromDirs[i], stack);
                    toDirs[i] = to;
                }

                if (to != null) {
                    BlockPos targetPos = pos.relative(to);
                    Direction targetSide = to.getOpposite();

                    // Case A: Transfer to next Vacuum Tube
                    BlockEntity targetBe = level.getBlockEntity(targetPos);
                    if (targetBe instanceof VacuumTubeBlockEntity nextTube && !nextTube.isDisconnected(targetSide)) {
                        Direction nextTo = nextTube.pickOutgoingDirection(level, targetPos, targetSide, stack);
                        if (nextTo != null && nextTube.addItem(stack, targetSide, nextTo)) {
                            travelingStacks.setStack(i, ItemStack.EMPTY);
                            fromDirs[i] = null;
                            toDirs[i] = null;
                            progress[i] = 0.0f;
                            prevProgress[i] = 0.0f;
                            dirty = true;
                            continue;
                        }
                    }

                    // Case B: Insert into destination container
                    if (sideModes[to.ordinal()] == TubeMode.INSERT) {
                        ResourceHandler<ItemResource> container = level.getCapability(Capabilities.Item.BLOCK, targetPos, targetSide);
                        if (container == null) {
                            container = level.getCapability(Capabilities.Item.BLOCK, targetPos, null);
                        }
                        if (container != null) {
                            try (Transaction tx = Transaction.openRoot()) {
                                ItemResource resource = ItemResource.of(stack);
                                int toInsert = stack.getCount();
                                int inserted = container.insert(resource, toInsert, tx);
                                if (inserted <= 0) {
                                    // Slot-by-slot fallback for modded inventories
                                    for (int s = 0; s < container.size() && toInsert > 0; s++) {
                                        int ins = container.insert(s, resource, toInsert, tx);
                                        if (ins > 0) {
                                            inserted += ins;
                                            toInsert -= ins;
                                        }
                                    }
                                }
                                if (inserted > 0) {
                                    tx.commit();
                                    stack.shrink(inserted);
                                    if (stack.isEmpty()) {
                                        travelingStacks.setStack(i, ItemStack.EMPTY);
                                        fromDirs[i] = null;
                                        toDirs[i] = null;
                                        progress[i] = 0.0f;
                                        prevProgress[i] = 0.0f;
                                        dirty = true;
                                        continue;
                                    }
                                }
                            }

                            // If container is temporarily full, hold at 1.0f and retry next tick
                            progress[i] = 1.0f;
                            continue;
                        }
                    }
                }

                // If path is temporarily blocked or waiting, hold in tube at 1.0f
                progress[i] = 1.0f;
                continue;
            }
        }

        if (dirty) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VacuumTubeBlockEntity be) {
        float speed = be.energyHandler.getAmountAsInt() >= 1
                ? (float) Config.TUBE_POWERED_SPEED.getAsDouble()
                : (float) Config.TUBE_UNPOWERED_SPEED.getAsDouble();
        for (int i = 0; i < MAX_TRAVELING; i++) {
            if (!be.travelingStacks.getStack(i).isEmpty()) {
                be.prevProgress[i] = be.progress[i];
                be.progress[i] = Math.min(1.0f, be.progress[i] + speed);
            }
        }
    }

    public void dropAllItems(Level level, BlockPos pos) {
        for (int i = 0; i < MAX_TRAVELING; i++) {
            ItemStack stack = travelingStacks.getStack(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        travelingStacks.serialize(output.child("stacks"));
        energyHandler.serialize(output.child("energy"));
        for (int i = 0; i < 6; i++) {
            output.putInt("mode_" + i, sideModes[i].ordinal());
        }
        for (int i = 0; i < MAX_TRAVELING; i++) {
            output.putInt("from_" + i, fromDirs[i] != null ? fromDirs[i].ordinal() : -1);
            output.putInt("to_" + i, toDirs[i] != null ? toDirs[i].ordinal() : -1);
            output.putFloat("prog_" + i, progress[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        travelingStacks.deserialize(input.childOrEmpty("stacks"));
        energyHandler.deserialize(input.childOrEmpty("energy"));
        for (int i = 0; i < 6; i++) {
            int m = input.getIntOr("mode_" + i, 0);
            sideModes[i] = TubeMode.values()[Math.min(Math.max(0, m), TubeMode.values().length - 1)];
        }
        for (int i = 0; i < MAX_TRAVELING; i++) {
            int f = input.getIntOr("from_" + i, -1);
            int t = input.getIntOr("to_" + i, -1);
            fromDirs[i] = (f >= 0 && f < 6) ? Direction.values()[f] : null;
            toDirs[i] = (t >= 0 && t < 6) ? Direction.values()[t] : null;
            progress[i] = input.getFloatOr("prog_" + i, 0.0f);
            prevProgress[i] = progress[i];
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
