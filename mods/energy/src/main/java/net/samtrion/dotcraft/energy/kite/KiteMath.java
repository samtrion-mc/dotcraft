package net.samtrion.dotcraft.energy.kite;

import java.util.function.IntPredicate;

/**
 * The math of the kite wind generator, free of Minecraft types so a unit test can run it.
 * <p>
 * <b>Altitude.</b> The kite flies straight up from the block on a tether of {@link #tether(int)} blocks (32 / 64 / 128 / 256 / 512 for
 * tier 1-5). The altitude is measured above the top of the block and is <i>not</i> clamped to the build height: a kite on a tower or on a
 * mountain flies on above the top of the world.
 * <p>
 * <b>Output</b> = peak x (altitude / tether) x openness x weather. The peak (see the common config) is what the kite makes with the
 * tether fully out, a free sky and clear weather; {@code openness} is the free share of the air around the kite.
 * <p>
 * <b>Weather perk per tier</b> (one qualitative step per tier, besides the longer tether):
 * <ul>
 * <li>tier 1: rain halves the output, a thunderstorm tears the tether, so the kite is reeled in (output 0);</li>
 * <li>tier 2: a waxed line, rain costs only a quarter;</li>
 * <li>tier 3: a rain-proof sail, rain costs nothing; a thunderstorm still means reeling in;</li>
 * <li>tier 4: a storm line, the kite stays up in a thunderstorm at half the output;</li>
 * <li>tier 5: an insulated tether and storm sail, thunder costs nothing.</li>
 * </ul>
 * A thunderstorm never attracts lightning to a kite (no lightning is ever spawned): a kite that has to give way is simply reeled in.
 * <p>
 * <b>Economy.</b> The peaks follow the battery ladder 15 / 75 / 560 / 5600 / 56000 FE/t. A kite is up around the clock, but it rarely
 * has the full tether, a fully free sky and clear weather, so the daily yield is anchored at {@link #TYPICAL_SHARE} x
 * {@link #TYPICAL_OPENNESS} of the peak and a weather mix of {@link #TYPICAL_CLEAR} / {@link #TYPICAL_RAIN} / {@link #TYPICAL_THUNDER}
 * (see {@link #typicalDailyFactor(int)}); at that rate every tier fills the battery of its tier in about one seventh of an MC day.
 */
final class KiteMath {
    /** The highest tier. */
    static final int MAX_TIER = 5;
    /** Blocks between two samples on the way up, so the check is coarse: a plate thinner than this may be missed. */
    static final int STEP = 4;
    /** The ticks of one MC day. */
    static final int DAY_TICKS = 24_000;
    /** Share of the tether that is out on a typical day. */
    static final double TYPICAL_SHARE = 0.6;
    /** Free share of the air around the kite on a typical day. */
    static final double TYPICAL_OPENNESS = 0.8;
    /** Share of a typical day with clear weather. */
    static final double TYPICAL_CLEAR = 0.75;
    /** Share of a typical day with rain. */
    static final double TYPICAL_RAIN = 0.20;
    /** Share of a typical day with a thunderstorm. */
    static final double TYPICAL_THUNDER = 0.05;

    private static final int[] TETHER = {32, 64, 128, 256, 512};

    /** Not instantiable. */
    private KiteMath() {}

    /** The weather at the kite. */
    enum Weather {
        /** No precipitation. */
        CLEAR,
        /** Rain (or snow) at the kite. */
        RAIN,
        /** A thunderstorm at the kite. */
        THUNDER
    }

    /**
     * The tether length of a tier, which is the highest altitude the kite can reach.
     *
     * @param tier the tier, 1..5
     * @return the length in blocks
     */
    static int tether(int tier) {
        return TETHER[tier - 1];
    }

    /**
     * What the weather leaves of the output; 0 means the kite is reeled in.
     *
     * @param tier    the tier, 1..5
     * @param weather the weather at the kite
     * @return the factor, 0..1
     */
    static double weatherFactor(int tier, Weather weather) {
        return switch (weather) {
            case CLEAR -> 1;
            case RAIN -> tier == 1 ? 0.5 : tier == 2 ? 0.75 : 1;
            case THUNDER -> tier == 4 ? 0.5 : tier == 5 ? 1 : 0;
        };
    }

    /**
     * The altitude the kite can fly at: it climbs along the free air above the block and stops at the last free sample before an
     * obstacle. The check is coarse: samples lie {@link #STEP} blocks apart (and one directly above the block).
     *
     * @param tier      the tier, 1..5
     * @param blockedAt whether the air {@code h} blocks above the top of the block is blocked (a block within the radius of the kite)
     * @return the altitude in blocks, 0 if the air directly above the block is blocked
     */
    static int reach(int tier, IntPredicate blockedAt) {
        int max = tether(tier);
        int free = 0;
        for (int h = 1; h <= max; h = h == 1 ? STEP : h + STEP) {
            if (blockedAt.test(h)) return free;
            free = h;
        }
        return max;
    }

    /**
     * The free share of the air around the kite.
     *
     * @param free  the samples around the kite that are free
     * @param total all samples
     * @return 0..1, 1 for no samples
     */
    static double openness(int free, int total) {
        return total <= 0 ? 1 : (double) free / total;
    }

    /**
     * The altitude as share of the tether.
     *
     * @param tier     the tier, 1..5
     * @param altitude the altitude of the kite above the block in blocks, not limited by the build height
     * @return 0..1
     */
    static double share(int tier, double altitude) {
        return Math.clamp(altitude / tether(tier), 0, 1);
    }

    /**
     * What the kite makes right now.
     *
     * @param peak     the peak of the tier in FE/t
     * @param tier     the tier, 1..5
     * @param altitude the altitude of the kite above the block in blocks
     * @param openness the free share of the air around the kite, 0..1
     * @param weather  the weather at the kite
     * @param sky      whether the dimension has a sky
     * @return the FE per tick, may be fractional; 0 without a sky
     */
    static double output(double peak, int tier, double altitude, double openness, Weather weather, boolean sky) {
        if (!sky) return 0;
        return peak * share(tier, altitude) * Math.clamp(openness, 0, 1) * weatherFactor(tier, weather);
    }

    /**
     * The typical share of the peak a kite makes over a day.
     *
     * @param tier the tier, 1..5
     * @return the factor to multiply with the peak
     */
    static double typicalDailyFactor(int tier) {
        double weather = TYPICAL_CLEAR * weatherFactor(tier, Weather.CLEAR) + TYPICAL_RAIN * weatherFactor(tier, Weather.RAIN)
                + TYPICAL_THUNDER * weatherFactor(tier, Weather.THUNDER);
        return TYPICAL_SHARE * TYPICAL_OPENNESS * weather;
    }

    /**
     * The FE a kite makes on a typical day.
     *
     * @param peak the peak of the tier in FE/t
     * @param tier the tier, 1..5
     * @return the FE per MC day
     */
    static double typicalDailyYield(double peak, int tier) {
        return peak * DAY_TICKS * typicalDailyFactor(tier);
    }
}
