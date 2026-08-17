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
 * Large EMF storage block with configurable sided I/O. Receives EMF into a
 * {@link EmfConstants#CAPACITOR_CAPACITY} buffer based on configured side modes (INPUT, OUTPUT, BOTH, DISABLED),
 * and each tick pushes up to {@link EmfConstants#CAPACITOR_TRANSFER} EMF (shared fairly across all output sides)
 * into adjacent Forge-Energy consumers.
 */
public class CapacitorBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private final SimpleEnergyHandler handler = new SimpleEnergyHandler(
            EmfConstants.CAPACITOR_CAPACITY, EmfConstants.CAPACITOR_TRANSFER, EmfConstants.CAPACITOR_TRANSFER) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private final EnergyHandler[] sidedHandlers = new EnergyHandler[6];

    /** Stored amount at the end of the previous tick, used to refresh comparators only on change. */
    private int lastEnergy;

    /**
     * Server-authoritative view of the stored/max EMF and side configuration.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = handler.getAmountAsInt();
            int capacity = handler.getCapacityAsInt();
            if (index == 0) return energy & 0xFFFF;
            if (index == 1) return (energy >>> 16) & 0xFFFF;
            if (index == 2) return capacity & 0xFFFF;
            if (index == 3) return (capacity >>> 16) & 0xFFFF;
            if (index >= 4 && index <= 9) return sideModes[index - 4].ordinal();
            if (index == 10) return autoEject ? 1 : 0;
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 4 && index <= 9) {
                sideModes[index - 4] = MachineSideMode.values()[Math.min(Math.max(0, value), MachineSideMode.values().length - 1)];
            } else if (index == 10) {
                autoEject = value == 1;
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CAPACITOR_BE.get(), pos, state);
        for (int i = 0; i < 6; i++) {
            sideModes[i] = MachineSideMode.BOTH;
            final int index = i;
            sidedHandlers[i] = new SidedEnergyHandler(handler, () -> sideModes[index]);
        }
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side == null) {
            return handler;
        }
        RelativeSide relSide = RelativeSide.fromDirection(side, getFacing());
        MachineSideMode mode = getSideMode(relSide);
        if (mode == MachineSideMode.DISABLED) {
            return null;
        }
        return sidedHandlers[relSide.ordinal()];
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
     * adjacent consumers and cables on sides configured for OUTPUT or BOTH.
     */
    private void pushToConsumers(ServerLevel level, BlockPos pos) {
        if (!autoEject) {
            return;
        }
        int mine = handler.getAmountAsInt();
        if (mine <= 0) {
            return;
        }

        Direction facing = getFacing();
        List<EnergyHandler> targets = new ArrayList<>(Direction.values().length);
        for (Direction dir : Direction.values()) {
            RelativeSide relSide = RelativeSide.fromDirection(dir, facing);
            MachineSideMode mode = getSideMode(relSide);
            if (mode != MachineSideMode.OUTPUT && mode != MachineSideMode.BOTH) {
                continue;
            }

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
