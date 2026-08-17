package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobMagnetBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    public static final int PULL_COST_PER_TICK = 20;
    public static final double RADIUS = 8.0;

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(20_000, 1_000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private boolean active = false;

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

    public MobMagnetBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.MOB_MAGNET_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }

    public @Nullable EnergyHandler getEnergyHandler() {
        return energyHandler;
    }

    @Override
    public int getEnergyUsage() {
        return active ? com.suiseika.solenoid.Config.MOB_MAGNET_ENERGY_USAGE.getAsInt() : 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.solenoid.mob_magnet");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MobMagnetMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        this.active = false;
        int pullCost = com.suiseika.solenoid.Config.MOB_MAGNET_ENERGY_USAGE.getAsInt();
        int radius = com.suiseika.solenoid.Config.MOB_MAGNET_RADIUS.getAsInt();
        double strength = com.suiseika.solenoid.Config.MOB_MAGNET_STRENGTH.getAsDouble();

        if (energyHandler.getAmountAsInt() >= pullCost) {
            AABB area = new AABB(pos).inflate(radius);
            List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> !(e instanceof Player) && !e.isSpectator() && e.isAlive());

            if (!mobs.isEmpty()) {
                Vec3 target = Vec3.atCenterOf(pos);
                for (LivingEntity mob : mobs) {
                    Vec3 mobPos = mob.position();
                    Vec3 diff = target.subtract(mobPos);
                    double dist = diff.length();
                    if (dist > 0.5 && dist <= radius) {
                        Vec3 dir = diff.normalize();
                        mob.setDeltaMovement(mob.getDeltaMovement().add(dir.x * strength, 0.05, dir.z * strength));
                        mob.hurtMarked = true;
                    }
                }
                energyHandler.set(energyHandler.getAmountAsInt() - pullCost);
                this.active = true;
            }
        }

        if (state.getValue(MobMagnetBlock.LIT) != this.active) {
            level.setBlock(pos, state.setValue(MobMagnetBlock.LIT, this.active), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyHandler.serialize(output.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyHandler.deserialize(input.childOrEmpty("energy"));
    }
}
