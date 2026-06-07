package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.CopperCableBlock;
import com.suiseika.solenoid.energy.CopperCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Direction face = context.getClickedFace();

        if (state.is(SolenoidTags.Blocks.WRENCHABLE)) {
            if (!level.isClientSide()) {
                Rotation rotation = context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()
                        ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
                BlockState rotatedState = state.rotate(rotation);
                level.setBlock(pos, rotatedState, 3);
                
                Direction newFacing = rotatedState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(
                            Component.translatable("message.solenoid.wrench.facing", newFacing.name().toUpperCase()));
                }
                
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }

        if (state.getBlock() instanceof CopperCableBlock) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CopperCableBlockEntity cableBe) {
                    boolean disconnected = cableBe.toggleSide(face);
                    
                    // Force a block update to re-calculate connections and sync to client
                    level.sendBlockUpdated(pos, state, state, 3);
                    // Notify neighbors so they also update their connections to us
                    level.updateNeighborsAt(pos, state.getBlock());
                    level.updateNeighborsAt(pos.relative(face), state.getBlock());

                    String status = disconnected ? "disconnected" : "connected";
                    if (context.getPlayer() != null) {
                        context.getPlayer().sendSystemMessage(
                                Component.translatable("message.solenoid.wrench.cable", face.name().toLowerCase(), status));
                    }
                    
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
