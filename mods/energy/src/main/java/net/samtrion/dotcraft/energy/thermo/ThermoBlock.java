package net.samtrion.dotcraft.energy.thermo;

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

/**
 * A thermoelectric generator of one tier: a full block that makes FE from the temperature difference between two opposite neighbours, see
 * {@link ThermoMath}. It is deliberately no {@code GeneratorBlock}, so it never joins the pool of the solar and lunar panels. A
 * comparator reads its share of the peak (0..15).
 */
public class ThermoBlock extends Block implements EntityBlock {
    /** The tier, 1..5. */
    private final int tier;
    /** The block entity type. */
    private final Supplier<BlockEntityType<ThermoBlockEntity>> type;

    /**
     * Creates a generator block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       its block entity type
     */
    public ThermoBlock(Properties properties, int tier, Supplier<BlockEntityType<ThermoBlockEntity>> type) {
        super(properties);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermoBlockEntity(type.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((ThermoBlockEntity) be).serverTick((ServerLevel) l);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ThermoBlockEntity be ? be.signal() : 0;
    }
}
