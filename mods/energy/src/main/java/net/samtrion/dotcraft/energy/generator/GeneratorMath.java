package net.samtrion.dotcraft.energy.generator;

/**
 * The pure math of the solar and lunar panels, free of any Minecraft types so unit tests can carry the balance. All factors are
 * multipliers of the peak output; a tier is {@code 1..5}.
 */
public final class GeneratorMath {
    /** The highest tier. */
    public static final int MAX_TIER = 5;
    /** Per tier: how much the daylight/moonlight plateau is widened (tracking); the factor is capped at 1. */
    private static final float[] TRACKING = {0F, 0F, 0.15F, 0.3F, 0.45F};
    /** Per tier: the share of the output that stays in rain. */
    private static final float[] WEATHER = {0.2F, 0.4F, 0.6F, 0.8F, 1F};
    /** Per tier: the output of a lunar panel at new moon, as share of its full moon output. */
    private static final float[] NEW_MOON_FLOOR = {0.1F, 0.2F, 0.3F, 0.4F, 0.5F};

    /** The biome multipliers of the tags {@code *_boost} and {@code *_malus}. */
    public static final float BIOME_BOOST = 1.15F;
    /** See {@link #BIOME_BOOST}. */
    public static final float BIOME_MALUS = 0.85F;

    /** Not instantiable. */
    private GeneratorMath() {}

    /**
     * How high the sun stands.
     *
     * @param sunAngle the sun angle in degrees: 0 at noon, 90 at sunset, 180 at midnight, 270 at sunrise
     * @return 0 below the horizon, 1 at noon
     */
    public static float daylight(float sunAngle) {
        return clamp((float) Math.cos(Math.toRadians(sunAngle)));
    }

    /**
     * How high the moon stands; it is always opposite to the sun.
     *
     * @param sunAngle the sun angle in degrees, see {@link #daylight}
     * @return 0 below the horizon, 1 at midnight
     */
    public static float moonlight(float sunAngle) {
        return clamp(-(float) Math.cos(Math.toRadians(sunAngle)));
    }

    /**
     * Widens the plateau of the sun or moon curve by the tracking perk of a tier.
     *
     * @param factor the raw factor, 0..1
     * @param tier   the tier
     * @return the widened factor, 0..1
     */
    public static float tracked(float factor, int tier) {
        return clamp(factor * (1F + TRACKING[tier - 1]));
    }

    /** The share of the output that stays when a panel is waterlogged. */
    public static final float WATERLOGGED_FACTOR = 0.2F;

    /**
     * The water malus: a waterlogged panel loses 80 % of its output.
     *
     * @param waterlogged whether the panel is waterlogged
     * @return the multiplier
     */
    public static float waterlogged(boolean waterlogged) {
        return waterlogged ? WATERLOGGED_FACTOR : 1F;
    }

    /** The weather over a panel. */
    public enum Weather {
        /** No rain over the panel. */
        CLEAR,
        /** It rains over the panel. */
        RAIN,
        /** It rains and thunders over the panel. */
        THUNDER
    }

    /** The share of the rain output that stays in a thunderstorm. */
    public static final float THUNDER_FACTOR = 0.5F;

    /**
     * The weather perk: rain cuts the output by the tier's share, tier 5 is immune to rain; a thunderstorm halves what rain leaves.
     *
     * @param tier    the tier
     * @param weather the weather over the panel
     * @return the multiplier
     */
    public static float weather(int tier, Weather weather) {
        return switch (weather) {
            case CLEAR -> 1F;
            case RAIN -> WEATHER[tier - 1];
            case THUNDER -> WEATHER[tier - 1] * THUNDER_FACTOR;
        };
    }

    /**
     * The share of the peak a solar panel makes.
     *
     * @param tier        the tier
     * @param sunAngle    the sun angle in degrees, see {@link #daylight}
     * @param skyAccess   whether the panel sees the sky
     * @param weather     the weather over the panel
     * @param biome       the biome multiplier, see {@link #BIOME_BOOST}
     * @return the multiplier of the peak
     */
    public static double solar(int tier, float sunAngle, boolean skyAccess, Weather weather, float biome) {
        if (!skyAccess) return 0;
        return tracked(daylight(sunAngle), tier) * weather(tier, weather) * biome;
    }

    /**
     * The share of the full moon peak a lunar panel makes.
     *
     * @param tier       the tier
     * @param sunAngle   the sun angle in degrees, see {@link #daylight}
     * @param moonPhase  the moon phase index, 0 full moon .. 4 new moon .. 7
     * @param skyAccess  whether the panel sees the sky
     * @param weather    the weather over the panel
     * @param biome      the biome multiplier, see {@link #BIOME_BOOST}
     * @param blockLight the block light level, 0..15, above the panel (light pollution)
     * @return the multiplier of the peak
     */
    public static double lunar(int tier, float sunAngle, int moonPhase, boolean skyAccess, Weather weather, float biome, int blockLight) {
        if (!skyAccess) return 0;
        float night = tracked(moonlight(sunAngle), tier);
        float floor = NEW_MOON_FLOOR[tier - 1];
        int distance = Math.min(moonPhase, 8 - moonPhase);
        float phase = 1F - distance / 4F * (1F - floor);
        float pollution = 1F - 0.5F * Math.clamp(blockLight, 0, 15) / 15F;
        return night * phase * weather(tier, weather) * biome * pollution;
    }

    private static float clamp(float v) {
        return Math.clamp(v, 0F, 1F);
    }
}
