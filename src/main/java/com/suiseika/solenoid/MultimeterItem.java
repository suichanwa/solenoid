package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.AbstractEmfBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class MultimeterItem extends Item {
    public MultimeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        EnergyHandler energy = level.getCapability(Capabilities.Energy.BLOCK, pos, context.getClickedFace());

        if (energy != null) {
            long stored = energy.getAmountAsInt();
            long max = energy.getCapacityAsInt();
            int pct = max > 0 ? (int) (stored * 100 / max) : 0;

            player.sendSystemMessage(Component.empty()); // Spacer
            player.sendSystemMessage(Component.literal("--- ").withStyle(ChatFormatting.GRAY)
                    .append(state.getBlock().getName().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(" ---").withStyle(ChatFormatting.GRAY));

            player.sendSystemMessage(Component.translatable("message.solenoid.multimeter.energy", 
                    String.format("%,d", stored), String.format("%,d", max), pct)
                    .withStyle(ChatFormatting.AQUA));

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractEmfBlockEntity emfBe) {
                int progress = emfBe.getProgress();
                int maxProgress = emfBe.getMaxProgress();

                if (maxProgress > 0) {
                    int progPct = (progress * 100 / maxProgress);
                    String status = progress > 0 ? "Running" : "Idle";
                    player.sendSystemMessage(Component.translatable("message.solenoid.multimeter.progress", 
                            progPct, status).withStyle(ChatFormatting.YELLOW));
                }

                int usage = emfBe.getEnergyUsage();
                if (usage > 0) {
                    player.sendSystemMessage(Component.translatable("message.solenoid.multimeter.usage", 
                            String.format("%,d", usage)).withStyle(ChatFormatting.GREEN));
                }
            }
            return InteractionResult.SUCCESS;
        } else {
            player.sendSystemMessage(Component.translatable("message.solenoid.multimeter.no_data")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
    }
}
