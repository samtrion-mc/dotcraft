package net.samtrion.dotcraft.energy.melt;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The block tags the melt generators read. They are data files, so a data pack can add a heat source or an ice. The three ice tags
 * decide how much FE a block gives, see {@link MeltIce}; a block in none of them is not fuel.
 */
public final class MeltTags {
    /**
     * Blocks that heat a melt generator when they touch it: lava, fire, magma and the burning campfires and furnaces (a block with a
     * {@code lit} property only counts while it is lit). They are not used up.
     */
    public static final TagKey<Block> HEAT = block("melt_heat");
    /** Plain ice, the cheapest fuel. Frosted ice is deliberately not in it: it is made without limit by frost walker boots. */
    public static final TagKey<Block> ICE = block("melt_fuel/ice");
    /** Packed ice, nine ice. */
    public static final TagKey<Block> PACKED_ICE = block("melt_fuel/packed_ice");
    /** Blue ice, nine packed ice. */
    public static final TagKey<Block> BLUE_ICE = block("melt_fuel/blue_ice");

    /** Not instantiable. */
    private MeltTags() {}

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(EnergyMod.MODID, name));
    }
}
