package net.samtrion.dotcraft.energy.generator;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A generator block of one family and tier, a flat plate of {@link #HEIGHT}; all of them share the {@link GeneratorBlockEntity}. The four side properties tell
 * whether another generator touches that side; the model leaves the border of the top away there, so a group looks like one surface.
 */
public class GeneratorBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    /** Another generator touches the north side. */
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    /** Another generator touches the east side. */
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    /** Another generator touches the south side. */
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    /** Another generator touches the west side. */
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    /** The height of a panel: 2 pixels, an eighth of a block. */
    public static final double HEIGHT = 0.125;
    /** The panel stands in water; it then makes {@link GeneratorMath#WATERLOGGED_FACTOR} of its output. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    private final GeneratorKind kind;
    private final int tier;
    private final Supplier<? extends BlockEntityType<? extends GeneratorBlockEntity>> type;

    /**
     * Creates a generator block.
     *
     * @param properties the block properties
     * @param kind       the family
     * @param tier       the tier, 1..5
     * @param type       the block entity type of the family
     */
    public GeneratorBlock(Properties properties, GeneratorKind kind, int tier, Supplier<? extends BlockEntityType<? extends GeneratorBlockEntity>> type) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false));
        this.kind = kind;
        this.tier = tier;
        this.type = type;
    }

    /**
     * The side property of a horizontal direction.
     *
     * @param direction north, east, south or west
     * @return its property
     */
    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException(direction.toString());
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            state = state.setValue(property(d), context.getLevel().getBlockState(context.getClickedPos().relative(d)).getBlock() instanceof GeneratorBlock);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (direction.getAxis().isHorizontal()) return state.setValue(property(direction), neighbourState.getBlock() instanceof GeneratorBlock);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /**
     * The family.
     *
     * @return the kind
     */
    public GeneratorKind kind() {
        return kind;
    }

    /**
     * The tier.
     *
     * @return 1..5
     */
    public int tier() {
        return tier;
    }

    /**
     * The block entity type of the family.
     *
     * @return the type
     */
    public BlockEntityType<? extends GeneratorBlockEntity> type() {
        return type.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(type(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((GeneratorBlockEntity) be).serverTick((net.minecraft.server.level.ServerLevel) l);
    }
}
