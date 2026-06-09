package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.InfiniteEnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Thorium RTG Block Entity.
 * Generates energy from thorium decay and pushes it to adjacent blocks.
 */
public class ThoriumRtgBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private int fuel = EmfConstants.RTG_MAX_FUEL;

    /**
     * Server-authoritative view of the buffered/max EMF, remaining fuel and per-tick generation rate
     * the client GUI needs. Each large int is split across two 16-bit data slots because the
     * container-data wire format is a signed short; the client menu reassembles the halves as unsigned.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getAmountAsInt();
            int capacity = energyStorage.getCapacityAsInt();
            int genRate = lastOutput;
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                case 4 -> genRate;
                case 5 -> fuel & 0xFFFF;
                case 6 -> (fuel >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server reads from the block entity; the synced copy lives client-side in the menu.
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    /** EMF actually output to neighbours on the previous tick, for the GUI readout only. */
    private int lastOutput;

    /**
     * Internal buffer. Big enough to hold a full round of output on all six faces so every side can
     * be served the same tick. {@code maxInsert = capacity} lets the decay loop top it up internally;
     * {@code maxExtract = RTG_OUTPUT} rate-limits each consumer (cable or machine) to the RTG's rating.
     */
    private final SimpleEnergyHandler energyStorage = new SimpleEnergyHandler(
            EmfConstants.RTG_OUTPUT * 6, EmfConstants.RTG_OUTPUT * 6, EmfConstants.RTG_OUTPUT) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    public ThoriumRtgBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.THORIUM_RTG_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyStorage;
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean changed = false;

        // 1. Decay: top the internal buffer back up to full from remaining fuel.
        if (fuel > 0) {
            int space = energyStorage.getCapacityAsInt() - energyStorage.getAmountAsInt();
            int toGenerate = Math.min(fuel, space);
            if (toGenerate > 0) {
                long moved = EnergyHandlerUtil.move(InfiniteEnergyHandler.INSTANCE, energyStorage, toGenerate, null);
                if (moved > 0) {
                    fuel -= (int) moved;
                    changed = true;
                }
            }
        }

        // 2. Push up to RTG_OUTPUT to every adjacent EMF acceptor (cables and machines alike).
        int output = 0;
        if (energyStorage.getAmountAsInt() > 0) {
            for (Direction dir : Direction.values()) {
                if (energyStorage.getAmountAsInt() == 0) break;

                EnergyHandler neighbour = neighbourHandler(level, pos, dir);
                if (neighbour != null && neighbour != energyStorage) {
                    long moved = EnergyHandlerUtil.move(energyStorage, neighbour, EmfConstants.RTG_OUTPUT, null);
                    if (moved > 0) {
                        output += (int) moved;
                        changed = true;
                    }
                }
            }
        }
        lastOutput = output;

        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel);
        energyStorage.serialize(output.child("emf"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.getIntOr("fuel", EmfConstants.RTG_MAX_FUEL);
        energyStorage.deserialize(input.childOrEmpty("emf"));
    }

    public int getFuel() {
        return fuel;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.thorium_rtg");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ThoriumRtgMenu(containerId, playerInventory, this, dataAccess);
    }
}
