package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Magnetic Crane block entity.
 * Uses EMF power to vacuum dropped items in an area below/around it into its 9-slot inventory,
 * and automatically pushes stored items into adjacent item handlers (chests, hoppers, pipes).
 */
public class MagneticCraneBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000;
    public static final int MAX_INPUT = 1_000;
    public static final int PULL_COST_PER_ITEM = 5;
    public static final int PULL_TICK_COST = 1;
    public static final double HORIZONTAL_RADIUS = 6.0;
    public static final double VERTICAL_DOWN = 8.0;
    public static final double VERTICAL_UP = 1.0;

    public static class CraneItemHandler extends ItemStacksResourceHandler {
        private final MagneticCraneBlockEntity be;

        public CraneItemHandler(MagneticCraneBlockEntity be) {
            super(9);
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

    private final CraneItemHandler itemHandler = new CraneItemHandler(this);

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(CAPACITY, MAX_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private boolean active = false;
    private int currentEnergyUsage = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyHandler.getAmountAsInt();
            int capacity = energyHandler.getCapacityAsInt();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                case 4 -> active ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public MagneticCraneBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.MAGNETIC_CRANE_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }

    public CraneItemHandler getItemHandler() {
        return itemHandler;
    }

    public CraneItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public int getEnergyUsage() {
        return active ? currentEnergyUsage : 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.magnetic_crane");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagneticCraneMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        this.active = false;
        this.currentEnergyUsage = 0;

        int stored = energyHandler.getAmountAsInt();
        int pullCost = com.suiseika.solenoid.Config.CRANE_ENERGY_PER_PULL.getAsInt();
        if (stored >= pullCost) {
            vacuumItems(level, pos);
        }

        ejectItems(level, pos);

        if (state.hasProperty(MagneticCraneBlock.LIT) && state.getValue(MagneticCraneBlock.LIT) != this.active) {
            level.setBlock(pos, state.setValue(MagneticCraneBlock.LIT, this.active), 3);
        }
    }

    private void vacuumItems(ServerLevel level, BlockPos pos) {
        int radius = com.suiseika.solenoid.Config.CRANE_RADIUS.getAsInt();
        AABB area = new AABB(
                pos.getX() - radius, pos.getY() - VERTICAL_DOWN, pos.getZ() - radius,
                pos.getX() + 1 + radius, pos.getY() + 1 + VERTICAL_UP, pos.getZ() + 1 + radius);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                e -> e.isAlive() && !e.hasPickUpDelay());

        if (items.isEmpty()) {
            return;
        }

        Vec3 targetPos = Vec3.atCenterOf(pos);
        int energy = energyHandler.getAmountAsInt();
        int usedEnergy = 0;
        int pullCost = com.suiseika.solenoid.Config.CRANE_ENERGY_PER_PULL.getAsInt();

        for (ItemEntity itemEntity : items) {
            if (energy - usedEnergy < pullCost) {
                break;
            }

            Vec3 itemPos = itemEntity.position();
            Vec3 diff = targetPos.subtract(itemPos);
            double dist = diff.length();

            if (dist <= 1.5) {
                // Item is right at the crane: suck into internal slots
                ItemStack drop = itemEntity.getItem();
                int initialCount = drop.getCount();
                ItemStack remaining = insertIntoInternal(drop);

                int inserted = initialCount - remaining.getCount();
                if (inserted > 0) {
                    int cost = inserted * PULL_COST_PER_ITEM;
                    usedEnergy += cost;
                    this.active = true;

                    if (remaining.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(remaining);
                    }
                }
            } else {
                // Pull toward crane
                Vec3 pull = diff.normalize().scale(0.18);
                itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(pull.x, 0.06, pull.z));
                itemEntity.hurtMarked = true;
                usedEnergy += PULL_TICK_COST;
                this.active = true;
            }
        }

        if (usedEnergy > 0) {
            int newEnergy = Math.max(0, energy - usedEnergy);
            energyHandler.set(newEnergy);
            this.currentEnergyUsage = usedEnergy;
            setChanged();
        }
    }

    private ItemStack insertIntoInternal(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack toInsert = stack.copy();

        // 1. Try to merge with existing stacks
        for (int i = 0; i < 9; i++) {
            ItemStack existing = itemHandler.getStack(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int move = Math.min(space, toInsert.getCount());
                    existing.grow(move);
                    itemHandler.setStack(i, existing);
                    toInsert.shrink(move);
                    if (toInsert.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 2. Try to place in empty slots
        for (int i = 0; i < 9; i++) {
            ItemStack existing = itemHandler.getStack(i);
            if (existing.isEmpty()) {
                itemHandler.setStack(i, toInsert.copy());
                return ItemStack.EMPTY;
            }
        }

        return toInsert;
    }

    private void ejectItems(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            var neighbour = level.getCapability(Capabilities.Item.BLOCK, pos.relative(dir), dir.getOpposite());
            if (neighbour == null) {
                continue;
            }

            int moved = ResourceHandlerUtil.move(itemHandler, neighbour, r -> true, 4, null);
            if (moved > 0) {
                this.active = true;
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
        energyHandler.serialize(output.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input.childOrEmpty("inventory"));
        energyHandler.deserialize(input.childOrEmpty("energy"));
    }
}
