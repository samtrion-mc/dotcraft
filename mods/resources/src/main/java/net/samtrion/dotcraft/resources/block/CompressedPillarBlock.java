package net.samtrion.dotcraft.resources.block;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * A compressed log or stem: like {@link CompressedBlock}, but it keeps the axis so it can be placed
 * upright and sideways.
 */
public class CompressedPillarBlock extends RotatedPillarBlock {
    private final Block base;
    private final int tier;

    /**
     * Creates a compressed pillar block.
     *
     * @param properties the block properties
     * @param base       the compressed log or stem
     * @param tier       the tier, {@code 1..MAX}
     */
    public CompressedPillarBlock(Properties properties, Block base, int tier) {
        super(properties);
        this.base = base;
        this.tier = tier;
    }

    @Override
    public MutableComponent getName() {
        return CompressedBlock.name(base, tier);
    }
}
