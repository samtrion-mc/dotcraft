package net.samtrion.dotcraft.energy.wheel;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A mob wheel of one tier: a drum that turns while mobs run on top of it, on the plate of {@link WheelPlate}. It is a block of its
 * own and no {@code GeneratorBlock}, so it never joins the pool of the solar and lunar panels. A redstone comparator reads the load
 * (0 to 15). Its collision shape ends at the height of the plate, as the model does, so mobs stand exactly on the plate.
 */
public class WheelBlock extends Block implements EntityBlock {
    /** The shape of the block and of its collision: a full footprint up to the height of the plate. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, WheelPlate.TOP * 16, 16);

    /** The side the discs of the wheel face, set by the placing player. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final int tier;
    private final Supplier<BlockEntityType<WheelBlockEntity>> type;

    /**
     * Creates the block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       its block entity type
     */
    public WheelBlock(Properties properties, int tier, Supplier<BlockEntityType<WheelBlockEntity>> type) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WheelBlockEntity(type.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((WheelBlockEntity) be).serverTick((ServerLevel) l);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof WheelBlockEntity wheel ? wheel.signal() : 0;
    }
}
