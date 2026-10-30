package net.samtrion.dotcraft.energy.vibration;

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
 * A vibration generator of one tier: a full block with a {@link VibrationBlockEntity}. It is deliberately not a
 * {@code GeneratorBlock}, so it never joins the pool of the solar and lunar panels. A comparator reads how full its buffer is.
 */
public class VibrationBlock extends Block implements EntityBlock {
    /** The tier, 1..5. */
    private final int tier;
    /** The block entity type, shared by all tiers. */
    private final Supplier<? extends BlockEntityType<VibrationBlockEntity>> type;

    /**
     * Creates a vibration generator block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       the block entity type, shared by all tiers
     */
    public VibrationBlock(Properties properties, int tier, Supplier<? extends BlockEntityType<VibrationBlockEntity>> type) {
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
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof VibrationBlockEntity be ? be.signal() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VibrationBlockEntity(type.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((VibrationBlockEntity) be).serverTick((ServerLevel) l);
    }
}
