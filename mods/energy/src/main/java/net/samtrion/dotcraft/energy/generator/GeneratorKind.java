package net.samtrion.dotcraft.energy.generator;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** A family of generators: how its blocks read the world and which config they use. */
public interface GeneratorKind {
    /**
     * The registry path prefix of the blocks, which are named {@code <prefix>_tier_<n>}.
     *
     * @return the prefix
     */
    String prefix();

    /**
     * The config of the blocks of this family.
     *
     * @return one entry per tier, tier 1 first
     */
    List<EnergyCommonConfig.Generator> configs();

    /**
     * What a generator makes right now.
     *
     * @param level the level
     * @param pos   the position of the generator
     * @param tier  the tier, 1..5
     * @return the FE per tick, may be fractional
     */
    double output(ServerLevel level, BlockPos pos, int tier);
}
