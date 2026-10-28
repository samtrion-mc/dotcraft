package net.samtrion.dotcraft.resources.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.samtrion.dotcraft.resources.common.OreExposure;

/**
 * Config of the {@link ExposedOreFeature}: the vanilla {@link OreConfiguration} of the vein and the kinds of blocks
 * of which one must touch a block of the vein for it to be placed. Serialized as
 * <pre>{"ore": {"targets": [...], "size": 6, "discard_chance_on_air_exposure": 0.0}, "exposure": ["air"]}</pre>
 *
 * @param ore      the vein: targets, size and the vanilla air exposure discard chance
 * @param exposure the kinds of neighbours that allow a block to be placed, at least one
 */
public record ExposedOreConfiguration(OreConfiguration ore, List<OreExposure> exposure) implements FeatureConfiguration {
    /** Serialized as in the class description. */
    public static final Codec<ExposedOreConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
            OreConfiguration.CODEC.fieldOf("ore").forGetter(ExposedOreConfiguration::ore),
            OreExposure.CODEC.listOf().fieldOf("exposure").forGetter(ExposedOreConfiguration::exposure)
    ).apply(i, ExposedOreConfiguration::new));

    /** Makes the list immutable. */
    public ExposedOreConfiguration {
        exposure = List.copyOf(exposure);
    }
}
