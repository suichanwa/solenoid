package com.suiseika.solenoid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Powered handheld ore scanner. Stores EMF in the {@link SolenoidDataComponents#EMF_ENERGY} data
 * component on the stack (synced to client), and exposes that buffer as a Forge-Energy
 * {@link Capabilities.Energy#ITEM} handler via {@link ItemAccessEnergyHandler}.
 *
 * <p>Interaction:
 * <ul>
 *   <li>Right-click a block that exposes an EMF provider (e.g. a Capacitor): pull EMF into the
 *       buffer (server-authoritative, capped at {@link #CAPACITY}).</li>
 *   <li>Right-click anything else / the air: spend {@link #SCAN_COST} EMF and open the radar.
 *       Cannot scan below the scan cost.</li>
 * </ul>
 */
public class MagnetometerItem extends Item {
    /** EMF the buffer holds. */
    public static final int CAPACITY = 5_000;
    /** EMF spent per scan/refresh. */
    public static final int SCAN_COST = 50;
    /** Per-operation transfer limit of the exposed item energy handler. */
    public static final int MAX_TRANSFER = CAPACITY;

    public MagnetometerItem(Properties properties) {
        super(properties);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(SolenoidDataComponents.EMF_ENERGY.get(), 0);
    }

    private static void setEnergy(ItemStack stack, int value) {
        stack.set(SolenoidDataComponents.EMF_ENERGY.get(), Math.max(0, Math.min(CAPACITY, value)));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        // Recharge if the clicked block is an EMF provider; otherwise treat as a scan trigger.
        EnergyHandler source = level.getCapability(Capabilities.Energy.BLOCK, pos, context.getClickedFace());
        if (source != null) {
            if (!level.isClientSide()) {
                rechargeFrom(source, stack);
            }
            return InteractionResult.SUCCESS;
        }

        return scan(level, stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return scan(level, player.getItemInHand(hand));
    }

    /** Drains the scan cost (server) and opens the radar (client). Fails when under-powered. */
    private InteractionResult scan(Level level, ItemStack stack) {
        int energy = getEnergy(stack);
        if (energy < SCAN_COST) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            // Client-only class; only loaded when this branch runs (never on a dedicated server).
            com.suiseika.solenoid.client.MagnetometerScreen.open();
        } else {
            setEnergy(stack, energy - SCAN_COST);
        }
        return InteractionResult.SUCCESS;
    }

    /** Pulls EMF out of {@code source} into the item buffer, capped at remaining space. */
    private void rechargeFrom(EnergyHandler source, ItemStack stack) {
        int current = getEnergy(stack);
        int space = CAPACITY - current;
        if (space <= 0) {
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = source.extract(space, tx);
            if (extracted > 0) {
                setEnergy(stack, current + extracted);
                tx.commit();
            }
        }
    }

    /**
     * Exposes the stack's EMF buffer as a Forge-Energy item handler so external systems can charge
     * it. Registered from a {@link RegisterCapabilitiesEvent} listener wired in the mod constructor.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(
                        access, SolenoidDataComponents.EMF_ENERGY.get(), CAPACITY, MAX_TRANSFER),
                MagnetometerItems.MAGNETOMETER.get());
    }
}
