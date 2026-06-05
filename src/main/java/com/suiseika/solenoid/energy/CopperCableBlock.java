package com.suiseika.solenoid.energy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class CopperCableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CopperCableBlock> CODEC = simpleCodec(CopperCableBlock::new);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> PROPERTY_MAP = new EnumMap<>(Direction.class);
    static {
        PROPERTY_MAP.put(Direction.NORTH, NORTH);
        PROPERTY_MAP.put(Direction.EAST, EAST);
        PROPERTY_MAP.put(Direction.SOUTH, SOUTH);
        PROPERTY_MAP.put(Direction.WEST, WEST);
        PROPERTY_MAP.put(Direction.UP, UP);
        PROPERTY_MAP.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);
    static {
        ARM_SHAPES.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARM_SHAPES.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARM_SHAPES.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
        ARM_SHAPES.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARM_SHAPES.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
        ARM_SHAPES.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
    }

    public CopperCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperCableBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_MAP.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        
        BlockState state = defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_MAP.get(dir), canConnect(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_MAP.get(dir), canConnect(level, pos, dir));
    }

    private boolean canConnect(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        
        // Connect to other copper cables
        if (neighborState.getBlock() instanceof CopperCableBlock) {
            return true;
        }
        
        // Connect to blocks with EMF capability
        if (level instanceof Level fullLevel) {
            return fullLevel.getCapability(Capabilities.Energy.BLOCK, neighborPos, neighborState, null, dir.getOpposite()) != null;
        }
        
        return false;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, EmfBlocks.COPPER_CABLE_BE.get(), AbstractEmfBlockEntity::serverTick);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
