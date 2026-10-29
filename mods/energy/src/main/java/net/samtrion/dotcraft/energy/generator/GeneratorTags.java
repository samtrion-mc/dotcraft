package net.samtrion.dotcraft.energy.generator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The tags the generators read, biome tags that are data files, so data packs can tune them. */
public final class GeneratorTags {
    /** Biomes where solar panels make more. */
    public static final TagKey<Biome> SOLAR_BOOST = biome("solar_boost");
    /** Biomes where solar panels make less. */
    public static final TagKey<Biome> SOLAR_MALUS = biome("solar_malus");
    /** Biomes where lunar panels make more. */
    public static final TagKey<Biome> LUNAR_BOOST = biome("lunar_boost");
    /** Biomes where lunar panels make less. */
    public static final TagKey<Biome> LUNAR_MALUS = biome("lunar_malus");

    /** Not instantiable. */
    private GeneratorTags() {}

    private static TagKey<Biome> biome(String name) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EnergyMod.MODID, name));
    }
}
