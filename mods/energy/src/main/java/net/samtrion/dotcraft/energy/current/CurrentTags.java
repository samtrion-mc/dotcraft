package net.samtrion.dotcraft.energy.current;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The tags the current generators read, data files, so data packs can tune them. */
public final class CurrentTags {
    /** Biomes where a current generator makes its full output: oceans and rivers. Everywhere else the tier decides, see {@link CurrentMath#biome}. */
    public static final TagKey<Biome> PRIME_BIOMES = TagKey.create(Registries.BIOME, id("current_prime"));
    /** The aquatic animals that raise the output when they swim near, wild or bucketed. */
    public static final TagKey<EntityType<?>> FAUNA = TagKey.create(Registries.ENTITY_TYPE, id("current_fauna"));

    /** Not instantiable. */
    private CurrentTags() {}

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, name);
    }
}
