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

public class HandCrankGeneratorBlockEntity extends AbstractEmfBlockEntity {
    public static final int CAPACITY = 50000;
    public static final int GENERATION_RATE = 40;
    public static final int TRANSFER_RATE = 200;
    public static final int MAX_SPIN = 60;
    public static final int SPIN_ADDED = 40;

    private int spin = 0;

    // We allow insert internally via maxInsert=CAPACITY, but we can also just use it as our buffer.
    private final SimpleEnergyHandler energyStorage = new SimpleEnergyHandler(CAPACITY, CAPACITY, TRANSFER_RATE) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public HandCrankGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.HAND_CRANK_GENERATOR_BE.get(), pos, state);
    }

    public void crank() {
        spin = Math.min(MAX_SPIN, spin + SPIN_ADDED);
        setChanged();
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyStorage;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean changed = false;

        // Generate energy if spinning
        if (spin > 0) {
            spin--;
            if (energyStorage.getAmountAsInt() < CAPACITY) {
                // If it can't accept, we just skip. We use move from an infinite source or just set internal.
                // Wait, SimpleEnergyHandler might have setAmount?
                // Let's just move from InfiniteEnergyHandler!
                EnergyHandlerUtil.move(net.neoforged.neoforge.transfer.energy.InfiniteEnergyHandler.INSTANCE, energyStorage, GENERATION_RATE, null);
            }
            changed = true;
        }

        // Push energy to neighbours
        if (energyStorage.getAmountAsInt() > 0) {
            for (Direction dir : Direction.values()) {
                if (energyStorage.getAmountAsInt() == 0) break;
                
                EnergyHandler neighbour = neighbourHandler(level, pos, dir);
                if (neighbour != null && neighbour != energyStorage) {
                    long moved = EnergyHandlerUtil.move(energyStorage, neighbour, TRANSFER_RATE, null);
                    if (moved > 0) {
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyStorage.serialize(output.child("emf"));
        output.putInt("spin", spin);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.deserialize(input.childOrEmpty("emf"));
        spin = input.getIntOr("spin", 0);
    }
}
