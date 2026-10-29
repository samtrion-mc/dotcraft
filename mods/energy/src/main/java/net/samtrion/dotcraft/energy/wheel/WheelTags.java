package net.samtrion.dotcraft.energy.wheel;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The entity type tags of the mob wheels, data files, so data packs can add or remove runners. */
public final class WheelTags {
    /** Every mob that runs a wheel; a mob not in this tag counts nothing, which keeps hostile mobs out. */
    public static final TagKey<EntityType<?>> RUNNERS = tag("wheel_runners");
    /** Runners that weigh twice as much, e.g. horses; also part of {@link #RUNNERS} by the data. */
    public static final TagKey<EntityType<?>> STRONG = tag("wheel_runners/strong");
    /** Small runners that weigh half as much (a full one from tier 3 on); also part of {@link #RUNNERS} by the data. */
    public static final TagKey<EntityType<?>> WEAK = tag("wheel_runners/weak");

    /** Not instantiable. */
    private WheelTags() {}

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, path));
    }

    /**
     * How strong a type of mob is as a runner.
     *
     * @param type the type
     * @return {@link WheelMath.Strength#NONE} if it is not in {@link #RUNNERS}
     */
    public static WheelMath.Strength strength(EntityType<?> type) {
        if (!type.builtInRegistryHolder().is(RUNNERS)) return WheelMath.Strength.NONE;
        if (type.builtInRegistryHolder().is(STRONG)) return WheelMath.Strength.STRONG;
        return type.builtInRegistryHolder().is(WEAK) ? WheelMath.Strength.WEAK : WheelMath.Strength.NORMAL;
    }
}
