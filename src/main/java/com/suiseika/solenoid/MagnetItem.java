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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Consumer;

public class MagnetItem extends Item implements ICurioItem {
    public static final int RADIUS = 3;

    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean active = !isActive(stack);
            stack.set(SolenoidDataComponents.ACTIVE.get(), active);
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

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            runVacuum(stack, player);
        }
    }

    public boolean hasCurioCapability(ItemStack stack) {
        return true;
    }

    private void runVacuum(ItemStack stack, Player player) {
        if (!isActive(stack)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area,
                e -> e.isAlive() && !e.hasPickUpDelay());

        for (ItemEntity itemEntity : items) {
            ItemStack drop = itemEntity.getItem();
            player.getInventory().add(drop);
            if (drop.isEmpty()) {
                itemEntity.discard();
            }
        }
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
    }
}
