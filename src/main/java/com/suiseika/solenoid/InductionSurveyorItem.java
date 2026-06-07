package com.suiseika.solenoid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Advanced handheld ore scanner using electromagnetic induction. Detects non-ferromagnetic
 * conductive ores (copper, gold, lapis, redstone).
 */
public class InductionSurveyorItem extends EmfPoweredItem {
    public static final int CAPACITY = 10_000;
    public static final int SCAN_COST = 100;
    public static final int MAX_TRANSFER = 1_000;

    public InductionSurveyorItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

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

    private InteractionResult scan(Level level, ItemStack stack) {
        int energy = getEnergy(stack);
        if (energy < SCAN_COST) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            com.suiseika.solenoid.client.MagnetometerScreen.open();
        } else {
            setEnergy(stack, energy - SCAN_COST);
        }
        return InteractionResult.SUCCESS;
    }

    /** Pulls EMF out of {@code source} into the item buffer, capped at remaining space. */
    private void rechargeFrom(EnergyHandler source, ItemStack stack) {
        int current = getEnergy(stack);
        int space = getCapacity() - current;
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

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(
                        access, SolenoidDataComponents.EMF_ENERGY.get(), CAPACITY, MAX_TRANSFER),
                MagnetiteItems.INDUCTION_SURVEYOR.get());
    }
}
