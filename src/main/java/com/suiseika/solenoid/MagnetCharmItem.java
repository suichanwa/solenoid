package com.suiseika.solenoid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;

/**
 * Rechargeable neodymium magnet charm. While it sits anywhere in the player's inventory and is
 * toggled active, {@link #inventoryTick} vacuums nearby ground {@link ItemEntity} drops straight
 * into the inventory, draining the stack's EMF buffer (the same
 * {@link SolenoidDataComponents#EMF_ENERGY} component the batteries use, exposed as a Forge-Energy
 * item handler so it charges in the Capacitor). Right-click toggles it; at 0 EMF it auto-switches off.
 *
 * <p>Also wearable in a Curios {@code charm} slot: {@link #curioTick} runs the same vacuum while the
 * charm is worn, so it works whether it sits in the main inventory or a bauble slot.
 *
 * <p>All state mutation is server-authoritative -- {@code inventoryTick} only ever runs with a
 * {@link ServerLevel} in 26.1.x, {@code curioTick} is guarded on {@code level.isClientSide()}, and
 * {@link #use} guards on the player being a {@link ServerPlayer}.
 */
public class MagnetCharmItem extends EmfPoweredItem implements ICurioItem {
    public static final int CAPACITY = 20_000;
    public static final int MAX_TRANSFER = 1_000;
    public static final int RADIUS = 6;
    public static final int IDLE_COST = 1;
    public static final int COST_PER_ITEM = 5;

    public MagnetCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean active = !isActive(stack);
            stack.set(SolenoidDataComponents.ACTIVE.get(), active);
            // overlay=true renders on the action bar.
            serverPlayer.sendSystemMessage(Component.translatable("message.solenoid.magnet_charm.toggle",
                    active ? Component.translatable("message.solenoid.on").withStyle(ChatFormatting.GREEN)
                           : Component.translatable("message.solenoid.off").withStyle(ChatFormatting.RED)), true);
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof Player player) {
            runVacuum(stack, player);
        }
    }

    /** Curios worn-slot tick: same vacuum while the charm is equipped in a bauble slot. */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            runVacuum(stack, player);
        }
    }

    /** Item must declare it carries a curio capability to become equippable in a slot. */
    @Override
    public boolean hasCurioCapability(ItemStack stack) {
        return true;
    }

    /**
     * Server-only vacuum: pulls nearby ground drops into the player's inventory, draining EMF. Shared
     * by {@link #inventoryTick} (main inventory) and {@link #curioTick} (worn in a Curios slot).
     */
    private void runVacuum(ItemStack stack, Player player) {
        if (!isActive(stack)) {
            return;
        }

        int energy = getEnergy(stack);
        if (energy < IDLE_COST) {
            stack.set(SolenoidDataComponents.ACTIVE.get(), false);
            return;
        }

        // Idle drain regardless of what gets collected.
        energy -= IDLE_COST;

        AABB area = player.getBoundingBox().inflate(RADIUS);
        // Skip items that still have a pickup delay so dropping/throwing items keeps working.
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area,
                e -> e.isAlive() && !e.hasPickUpDelay());

        for (ItemEntity itemEntity : items) {
            if (energy < COST_PER_ITEM) {
                break;
            }
            ItemStack drop = itemEntity.getItem();
            int before = drop.getCount();
            // Inventory#add mutates the stack, shrinking it by however much fit.
            player.getInventory().add(drop);
            if (drop.getCount() < before) {
                energy -= COST_PER_ITEM;
            }
            if (drop.isEmpty()) {
                itemEntity.discard();
            }
        }

        if (energy <= 0) {
            energy = 0;
            stack.set(SolenoidDataComponents.ACTIVE.get(), false);
        }
        setEnergy(stack, energy);
    }

    private boolean isActive(ItemStack stack) {
        return stack.getOrDefault(SolenoidDataComponents.ACTIVE.get(), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.solenoid.magnet_charm.state",
                isActive(stack) ? Component.translatable("message.solenoid.on").withStyle(ChatFormatting.GREEN)
                                : Component.translatable("message.solenoid.off").withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.solenoid.energy_stored", getEnergy(stack), getCapacity())
                .withStyle(ChatFormatting.AQUA));
    }

    /**
     * Exposes the stack's EMF buffer as a Forge-Energy item handler so the Capacitor can charge it,
     * exactly like the batteries. Wired from a {@link RegisterCapabilitiesEvent} listener in the
     * mod constructor.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(
                        access, SolenoidDataComponents.EMF_ENERGY.get(), CAPACITY, MAX_TRANSFER),
                MagnetiteItems.MAGNET_CHARM.get());
    }
}
