package net.samtrion.dotcraft.energy.melt;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A melt generator of one tier, a full block. It is no {@code GeneratorBlock}, so the solar and lunar panel groups never take it for
 * a panel. The {@link #LIT} property shows that heat is left in the reservoir, for the model and a little light.
 * A comparator reads how full the reservoir and the buffer are.
 */
public class MeltBlock extends Block implements EntityBlock {
    /** The reservoir holds heat: the block glows. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final int tier;
    private final Supplier<BlockEntityType<MeltBlockEntity>> type;

    /**
     * Creates a melt generator block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       the block entity type
     */
    public MeltBlock(Properties properties, int tier, Supplier<BlockEntityType<MeltBlockEntity>> type) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeltBlockEntity(type.get(), pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MeltBlockEntity be ? be.signal() : 0;
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((MeltBlockEntity) be).serverTick((ServerLevel) l);
    }
}
