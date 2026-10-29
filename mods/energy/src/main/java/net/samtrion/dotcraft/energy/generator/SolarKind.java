package net.samtrion.dotcraft.energy.generator;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The solar panels: the output follows the height of the sun, so only dimensions with a sky and a day count. Rain and
 * biome change it, see {@link GeneratorMath#solar}.
 */
public final class SolarKind implements GeneratorKind {
    /** The only instance. */
    public static final SolarKind INSTANCE = new SolarKind();

    private SolarKind() {}

    @Override
    public String prefix() {
        return "solar_panel";
    }

    @Override
    public List<EnergyCommonConfig.Generator> configs() {
        return EnergyCommonConfig.SOLAR_PANELS;
    }

    private static GeneratorMath.Weather weather(ServerLevel level, BlockPos above) {
        if (!level.isRainingAt(above)) return GeneratorMath.Weather.CLEAR;
        return level.isThundering() ? GeneratorMath.Weather.THUNDER : GeneratorMath.Weather.RAIN;
    }

    @Override
    public double output(ServerLevel level, BlockPos pos, int tier) {
        var above = pos.above();
        float sun = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos);
        var biome = level.getBiome(pos);
        float biomeFactor = biome.is(GeneratorTags.SOLAR_BOOST) ? GeneratorMath.BIOME_BOOST
                : biome.is(GeneratorTags.SOLAR_MALUS) ? GeneratorMath.BIOME_MALUS : 1F;
        return EnergyCommonConfig.SOLAR_PANELS.get(tier - 1).peak() * GeneratorMath.solar(tier, sun, level.dimensionType().hasSkyLight() && level.canSeeSky(above),
                weather(level, above), biomeFactor);
    }
}
