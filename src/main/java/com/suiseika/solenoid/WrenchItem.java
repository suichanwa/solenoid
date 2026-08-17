package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.CopperCableBlock;
import com.suiseika.solenoid.energy.CopperCableBlockEntity;
import com.suiseika.solenoid.energy.VacuumTubeBlock;
import com.suiseika.solenoid.energy.VacuumTubeBlockEntity;
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

        // 1. Vacuum Tubes (Cycle side modes: NORMAL -> EXTRACT -> DISCONNECTED)
        if (state.getBlock() instanceof VacuumTubeBlock) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VacuumTubeBlockEntity tubeBe) {
                    net.minecraft.world.phys.Vec3 hit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                    Direction targetDir = face;
                    if (hit.x < 0.35) targetDir = Direction.WEST;
                    else if (hit.x > 0.65) targetDir = Direction.EAST;
                    else if (hit.y < 0.35) targetDir = Direction.DOWN;
                    else if (hit.y > 0.65) targetDir = Direction.UP;
                    else if (hit.z < 0.35) targetDir = Direction.NORTH;
                    else if (hit.z > 0.65) targetDir = Direction.SOUTH;

                    var mode = tubeBe.cycleSideMode(targetDir);

                    level.sendBlockUpdated(pos, state, state, 3);
                    level.updateNeighborsAt(pos, state.getBlock());
                    level.updateNeighborsAt(pos.relative(targetDir), state.getBlock());

                    if (context.getPlayer() != null) {
                        context.getPlayer().sendSystemMessage(
                                Component.translatable("message.solenoid.wrench.tube_mode", targetDir.name().toUpperCase(), mode.name()));
                    }

                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Copper Cables (Toggle side connections)
        if (state.getBlock() instanceof CopperCableBlock) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CopperCableBlockEntity cableBe) {
                    boolean disconnected = cableBe.toggleSide(face);

                    level.sendBlockUpdated(pos, state, state, 3);
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

        // 3. Wrenchable machines with HORIZONTAL_FACING
        if (state.is(SolenoidTags.Blocks.WRENCHABLE) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (!level.isClientSide()) {
                Rotation rotation = context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()
                        ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
                BlockState rotatedState = state.rotate(level, pos, rotation);
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

        return InteractionResult.PASS;
    }
}
