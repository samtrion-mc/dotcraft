package net.samtrion.dotcraft.energy.melt;

import java.util.function.IntSupplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The kinds of ice a melt generator burns, with the FE a block of each gives. The values follow what the block is made of: packed
 * ice is nine ice and blue ice nine packed ice, so by default one FE costs the same amount of ice whichever type is melted (the
 * compact types only save space and hand-work). The values are in the config.
 */
public enum MeltIce {
    /** Plain ice. */
    ICE(MeltTags.ICE, () -> EnergyCommonConfig.MELT_YIELD.ice()),
    /** Packed ice. */
    PACKED_ICE(MeltTags.PACKED_ICE, () -> EnergyCommonConfig.MELT_YIELD.packedIce()),
    /** Blue ice. */
    BLUE_ICE(MeltTags.BLUE_ICE, () -> EnergyCommonConfig.MELT_YIELD.blueIce());

    private final TagKey<Block> tag;
    private final IntSupplier yield;

    MeltIce(TagKey<Block> tag, IntSupplier yield) {
        this.tag = tag;
        this.yield = yield;
    }

    /**
     * The FE one block of this kind gives.
     *
     * @return the FE, as configured
     */
    public int yield() {
        return yield.getAsInt();
    }

    /**
     * Which kind of ice a block is.
     *
     * @param state the block state
     * @return the kind, or {@code null} if the block is not fuel; the tags are checked from the most to the least valuable
     */
    public static @Nullable MeltIce of(BlockState state) {
        for (int i = values().length - 1; i >= 0; i--) {
            if (state.is(values()[i].tag)) return values()[i];
        }
        return null;
    }
}
