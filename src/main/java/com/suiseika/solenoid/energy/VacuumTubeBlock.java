package com.suiseika.solenoid.energy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Magnetic Vacuum Tube.
 * Pneumatic pipe that rapidly moves items between inventories using EMF acceleration.
 * Each side dynamically displays its connection state: NONE (disconnected), INSERT (normal tube), EXTRACT (extraction nozzle).
 */
public class VacuumTubeBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<VacuumTubeBlock> CODEC = simpleCodec(VacuumTubeBlock::new);

    public enum TubeConnection implements StringRepresentable {
        NONE("none"),
        INSERT("insert"),
        EXTRACT("extract");

        private final String name;

        TubeConnection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<TubeConnection> NORTH = EnumProperty.create("north", TubeConnection.class);
    public static final EnumProperty<TubeConnection> EAST = EnumProperty.create("east", TubeConnection.class);
    public static final EnumProperty<TubeConnection> SOUTH = EnumProperty.create("south", TubeConnection.class);
    public static final EnumProperty<TubeConnection> WEST = EnumProperty.create("west", TubeConnection.class);
    public static final EnumProperty<TubeConnection> UP = EnumProperty.create("up", TubeConnection.class);
    public static final EnumProperty<TubeConnection> DOWN = EnumProperty.create("down", TubeConnection.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, EnumProperty<TubeConnection>> PROPERTY_MAP = new EnumMap<>(Direction.class);
    static {
        PROPERTY_MAP.put(Direction.NORTH, NORTH);
        PROPERTY_MAP.put(Direction.EAST, EAST);
        PROPERTY_MAP.put(Direction.SOUTH, SOUTH);
        PROPERTY_MAP.put(Direction.WEST, WEST);
        PROPERTY_MAP.put(Direction.UP, UP);
        PROPERTY_MAP.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE_SHAPE = Block.box(4.5, 4.5, 4.5, 11.5, 11.5, 11.5);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);
    static {
        ARM_SHAPES.put(Direction.NORTH, Block.box(4.5, 4.5, 0.0, 11.5, 11.5, 4.5));
        ARM_SHAPES.put(Direction.SOUTH, Block.box(4.5, 4.5, 11.5, 11.5, 11.5, 16.0));
        ARM_SHAPES.put(Direction.EAST, Block.box(11.5, 4.5, 4.5, 16.0, 11.5, 11.5));
        ARM_SHAPES.put(Direction.WEST, Block.box(0.0, 4.5, 4.5, 4.5, 11.5, 11.5));
        ARM_SHAPES.put(Direction.UP, Block.box(4.5, 11.5, 4.5, 11.5, 16.0, 11.5));
        ARM_SHAPES.put(Direction.DOWN, Block.box(4.5, 0.0, 4.5, 11.5, 4.5, 11.5));
    }

    public VacuumTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, TubeConnection.NONE)
                .setValue(EAST, TubeConnection.NONE)
                .setValue(SOUTH, TubeConnection.NONE)
                .setValue(WEST, TubeConnection.NONE)
                .setValue(UP, TubeConnection.NONE)
                .setValue(DOWN, TubeConnection.NONE)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VacuumTubeBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_MAP.get(dir)) != TubeConnection.NONE) {
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
            state = state.setValue(PROPERTY_MAP.get(dir), getConnection(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_MAP.get(dir), getConnection(level, pos, dir));
    }

    public TubeConnection getConnection(LevelReader level, BlockPos pos, Direction dir) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VacuumTubeBlockEntity tubeBe) {
            if (tubeBe.isDisconnected(dir)) {
                return TubeConnection.NONE;
            }
            TubeMode mode = tubeBe.getSideMode(dir);

            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            // Connect to other vacuum tubes
            if (neighborState.getBlock() instanceof VacuumTubeBlock) {
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof VacuumTubeBlockEntity otherTube && otherTube.isDisconnected(dir.getOpposite())) {
                    return TubeConnection.NONE;
                }
                return mode == TubeMode.EXTRACT ? TubeConnection.EXTRACT : TubeConnection.INSERT;
            }

            // Connect only to blocks with Item capability (chests, machine item slots, hoppers)
            if (level instanceof Level fullLevel) {
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                boolean hasItem = fullLevel.getCapability(Capabilities.Item.BLOCK, neighborPos, neighborState, neighborBe, dir.getOpposite()) != null;
                if (hasItem) {
                    return mode == TubeMode.EXTRACT ? TubeConnection.EXTRACT : TubeConnection.INSERT;
                }
            }
        }
        return TubeConnection.NONE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, EmfBlocks.VACUUM_TUBE_BE.get(), VacuumTubeBlockEntity::clientTick);
        } else {
            return createTickerHelper(type, EmfBlocks.VACUUM_TUBE_BE.get(), AbstractEmfBlockEntity::serverTick);
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean isMoving) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VacuumTubeBlockEntity tubeBe) {
            tubeBe.dropAllItems(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
