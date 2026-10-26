package net.samtrion.dotcraft.resources.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;

/**
 * Data condition that holds while the configured maximum compression level is at least {@code level}; attached
 * to the recipes that output that level.
 *
 * @param level the level the recipe outputs
 */
public record MaxTierCondition(int level) implements ICondition {
    /** Serialized as {@code {"type": "dotcraft_resources:max_tier", "level": n}}. */
    public static final MapCodec<MaxTierCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(MaxTierCondition::level)
    ).apply(i, MaxTierCondition::new));

    @Override
    public boolean test(IContext context) {
        return level <= CraftResourcesConfig.maxCompressLevel();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
