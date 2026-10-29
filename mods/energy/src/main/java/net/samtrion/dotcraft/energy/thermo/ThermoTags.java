package net.samtrion.dotcraft.energy.thermo;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The block tags that say which neighbour is hot and which is cold: {@code dotcraftenergy:thermo/heat_1..3} and
 * {@code thermo/cold_1..3}, data files that a data pack can extend. The number is the level, a block in several tags counts
 * with its highest level. A block that has a {@code lit} state which is off (furnace, campfire) is not hot. Nothing is consumed:
 * a cold block stays what it is (melting is the business of other blocks).
 */
public final class ThermoTags {
    /** The heat tags, level 1 first. */
    public static final List<TagKey<Block>> HEAT = levels("heat");
    /** The cold tags, level 1 first. */
    public static final List<TagKey<Block>> COLD = levels("cold");

    /** Not instantiable. */
    private ThermoTags() {}

    /** The tags {@code thermo/<kind>_1..MAX_LEVEL}. */
    private static List<TagKey<Block>> levels(String kind) {
        return IntStream.rangeClosed(1, ThermoMath.MAX_LEVEL)
                .mapToObj(l -> TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "thermo/" + kind + "_" + l))).toList();
    }

    /**
     * The heat level of a block.
     *
     * @param state the state of the neighbour
     * @return 0 if it is not a heat source (or is switched off), else 1..{@link ThermoMath#MAX_LEVEL}
     */
    public static int heat(BlockState state) {
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return 0;
        return level(HEAT, state);
    }

    /**
     * The cold level of a block.
     *
     * @param state the state of the neighbour
     * @return 0 if it is not a cold source, else 1..{@link ThermoMath#MAX_LEVEL}
     */
    public static int cold(BlockState state) {
        return level(COLD, state);
    }

    /** The highest level whose tag holds the block, 0 if none does. */
    private static int level(List<TagKey<Block>> tags, BlockState state) {
        for (int l = tags.size(); l >= 1; l--) {
            if (state.is(tags.get(l - 1))) return l;
        }
        return 0;
    }
}
