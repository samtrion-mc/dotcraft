package net.samtrion.dotcraft.energy.current;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A current generator of one tier: a turbine pod in a frame, a full block that only runs while it is waterlogged. It is deliberately not a
 * {@code GeneratorBlock}, so it never joins the pool of the solar and lunar panels. It is a sealed pod: it holds water but does not
 * spread it (it schedules no fluid tick, neither when placed in water nor when a neighbour changes), so one bucket on dry ground does not make a current around it. It has a
 * comparator signal (how much of the best output it makes).
 *
 * <p>It has a flow direction, {@link #FACING}: the player's look direction when placed. Front and back are the flow axis, which must
 * stay free for the water (see {@link CurrentMath#blocksFlow}; a block there makes the output 0); the generator gives FE to the other
 * four sides only (see {@link #connects}).
 */
public class CurrentGeneratorBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    /** The pod stands in water; without it the generator makes nothing. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** The flow direction: the water runs through the pod along this axis (front to back). */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private final int tier;
    private final Supplier<? extends BlockEntityType<CurrentGeneratorBlockEntity>> type;

    /**
     * Creates a current generator block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       the block entity type of the family
     */
    public CurrentGeneratorBlock(Properties properties, int tier, Supplier<? extends BlockEntityType<CurrentGeneratorBlockEntity>> type) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
        this.tier = tier;
        this.type = type;
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
     * Whether a side takes part in the energy exchange: every side but the front and the back, which are the flow axis.
     *
     * @param state the state of the generator
     * @param side  the side, may be {@code null} (no side given)
     * @return {@code true} for up, down, left and right of the flow direction
     */
    public static boolean connects(BlockState state, @Nullable Direction side) {
        return side != null && side.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    /**
     * Fills the pod with water (a bucket) without scheduling a fluid tick, so the water stays inside: a waterlogged vanilla block
     * spreads its water to the neighbours, and a generator in dry air must not make a current of its own from one bucket.
     */
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (state.getValue(WATERLOGGED) || !fluidState.is(Fluids.WATER)) return false;
        if (!level.isClientSide()) level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof CurrentGeneratorBlockEntity be)) return 0;
        return CurrentMath.signal(be.output(), CurrentBlocks.CONFIGS.get(tier - 1).peak());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CurrentGeneratorBlockEntity(type.get(), pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((CurrentGeneratorBlockEntity) be).serverTick((ServerLevel) l);
    }
}
