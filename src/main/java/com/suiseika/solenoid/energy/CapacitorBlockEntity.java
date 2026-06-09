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
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Large EMF storage block. Receives EMF on every face into a
 * {@link EmfConstants#CAPACITOR_CAPACITY} buffer, and each tick pushes up to
 * {@link EmfConstants#CAPACITOR_TRANSFER} EMF (shared fairly across all sides) into adjacent
 * Forge-Energy consumers — i.e. handlers that cannot themselves be extracted from, so we never
 * ping-pong charge back into generators, cables or other batteries. NBT-persisted; exposes a
 * comparator analog signal scaled to fill via {@link CapacitorBlock}.
 */
public class CapacitorBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    // Receive and extract on all sides; extraction is used by our own outward push.
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.CAPACITOR_CAPACITY, EmfConstants.CAPACITOR_TRANSFER, EmfConstants.CAPACITOR_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Stored amount at the end of the previous tick, used to refresh comparators only on change. */
    private int lastEnergy;

    /**
     * Server-authoritative view of the stored/max EMF the client GUI needs. Each large int is split
     * across two 16-bit data slots because the container-data wire format is a signed short; the
     * client menu reassembles the halves as unsigned.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = handler.getAmountAsInt();
            int capacity = handler.getCapacityAsInt();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server reads from the block entity; the synced copy lives client-side in the menu.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CAPACITOR_BE.get(), pos, state);
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return handler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.capacitor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CapacitorMenu(containerId, playerInventory, this, dataAccess);
    }

    /** Comparator output 0-15 scaled to fill. */
    public int getComparatorOutput() {
        return EnergyHandlerUtil.getRedstoneSignalFromEnergyHandler(handler);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        pushToConsumers(level, pos);

        int now = handler.getAmountAsInt();
        if (now != lastEnergy) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
            lastEnergy = now;
        }
    }

    /**
     * Pushes up to {@link EmfConstants#CAPACITOR_TRANSFER} EMF total this tick, split fairly among the
     * adjacent consumers and cables.
     */
    private void pushToConsumers(ServerLevel level, BlockPos pos) {
        int mine = handler.getAmountAsInt();
        if (mine <= 0) {
            return;
        }

        List<EnergyHandler> targets = new ArrayList<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            EnergyHandler neighbour = neighbourHandler(level, pos, dir);
            if (neighbour == null || neighbour == handler) {
                continue;
            }

            // Always push into pure consumers (machines).
            // For extractable neighbours (cables, other batteries), only push if we have more energy (flow downhill).
            if (!canExtract(neighbour) || neighbour.getAmountAsInt() < mine) {
                targets.add(neighbour);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        int budget = EmfConstants.CAPACITOR_TRANSFER;
        int remaining = targets.size();
        for (EnergyHandler target : targets) {
            if (budget <= 0) {
                break;
            }
            // Even share of the remaining budget across the remaining targets.
            int share = Math.max(1, budget / remaining);
            int moved = EnergyHandlerUtil.move(handler, target, share, null);
            budget -= moved;
            remaining--;
        }
    }

    /** Simulated extract (rolled back) to classify a neighbour as a producer/storage vs a consumer. */
    private static boolean canExtract(EnergyHandler neighbour) {
        try (Transaction transaction = Transaction.openRoot()) {
            return neighbour.extract(1, transaction) > 0;
            // No commit() -> close() rolls the simulation back.
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
