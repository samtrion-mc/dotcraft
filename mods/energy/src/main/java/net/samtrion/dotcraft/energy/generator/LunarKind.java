package net.samtrion.dotcraft.energy.generator;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The lunar panels: the output follows the height of the moon, always opposite to the sun, and the moon phase; block light above the panel (light
 * pollution), rain and biome reduce it, see {@link GeneratorMath#lunar}.
 */
public final class LunarKind implements GeneratorKind {
    /** The only instance. */
    public static final LunarKind INSTANCE = new LunarKind();

    private LunarKind() {}

    @Override
    public String prefix() {
        return "lunar_panel";
    }

    @Override
    public List<EnergyCommonConfig.Generator> configs() {
        return EnergyCommonConfig.LUNAR_PANELS;
    }

    private static GeneratorMath.Weather weather(ServerLevel level, BlockPos above) {
        if (!level.isRainingAt(above)) return GeneratorMath.Weather.CLEAR;
        return level.isThundering() ? GeneratorMath.Weather.THUNDER : GeneratorMath.Weather.RAIN;
    }

    @Override
    public double output(ServerLevel level, BlockPos pos, int tier) {
        var above = pos.above();
        var attributes = level.environmentAttributes();
        float sun = attributes.getValue(EnvironmentAttributes.SUN_ANGLE, pos);
        int phase = attributes.getValue(EnvironmentAttributes.MOON_PHASE, pos).index();
        var biome = level.getBiome(pos);
        float biomeFactor = biome.is(GeneratorTags.LUNAR_BOOST) ? GeneratorMath.BIOME_BOOST
                : biome.is(GeneratorTags.LUNAR_MALUS) ? GeneratorMath.BIOME_MALUS : 1F;
        return EnergyCommonConfig.LUNAR_PANELS.get(tier - 1).peak() * GeneratorMath.lunar(tier, sun, phase, level.dimensionType().hasSkyLight() && level.canSeeSky(above),
                weather(level, above), biomeFactor, level.getBrightness(LightLayer.BLOCK, above));
    }
}
