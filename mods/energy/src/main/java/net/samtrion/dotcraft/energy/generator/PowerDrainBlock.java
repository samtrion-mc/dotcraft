package net.samtrion.dotcraft.energy.generator;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The creative-only power drain: it takes any FE offered to it on any side, and it pulls FE out of every neighbour that gives
 * some on that side; the FE is destroyed. For testing generators.
 */
public class PowerDrainBlock extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<PowerDrainBlockEntity>> type;

    /**
     * Creates the block.
     *
     * @param properties the block properties
     * @param type       its block entity type
     */
    public PowerDrainBlock(Properties properties, Supplier<BlockEntityType<PowerDrainBlockEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerDrainBlockEntity(type.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((PowerDrainBlockEntity) be).serverTick((ServerLevel) l);
    }
}
