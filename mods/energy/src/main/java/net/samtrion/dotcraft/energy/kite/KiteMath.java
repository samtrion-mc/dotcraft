package net.samtrion.dotcraft.energy.kite;

import java.util.function.IntPredicate;

/**
 * The math of the kite wind generator, free of Minecraft types so a unit test can run it.
 * <p>
 * <b>Altitude.</b> The kite flies straight up from the block on a tether of {@link #tether(int)} blocks (32 / 64 / 128 / 256 / 512 for
 * tier 1-5). The altitude is measured above the top of the block and is <i>not</i> clamped to the build height: a kite on a tower or on a
 * mountain flies on above the top of the world. Every block of the way is checked (see {@link #reach(int, IntPredicate)}), so no roof,
 * however thin, is flown through.
 * <p>
 * <b>Output</b> = peak x (altitude / tether) x openness x weather x wind. The peak (see the common config) is what the kite makes with
 * the tether fully out, a free sky, clear weather and a gust at its best; {@code openness} is the free share of the air around the kite
 * and {@link #wind(long)} is the gust: the wind is never steady and swings between {@link #WIND_MIN} and 1 with a mean of
 * {@link #WIND_MEAN}, so even a kite in the open makes only about 60 % of its peak over a day.
 * <p>
 * <b>Perks per tier</b> (one qualitative step per tier besides the longer tether; the factors are fixed, not configurable):
 * <ul>
 * <li>tier 1: the plain kite. It needs a free cross of three blocks on the way up, rain halves the output and a thunderstorm tears the
 * tether, so the kite is reeled in (output 0);</li>
 * <li>tier 2: a waxed line, rain costs only a quarter;</li>
 * <li>tier 3: a rain-proof sail (rain costs nothing) and a kite that ignores obstructions (see {@link #ignoresObstructions(int)}):
 * blocks on the way up do not stop it and a crowded sky costs nothing; a thunderstorm still means reeling in. The altitude is
 * measured above the top of the generator block, so the tether never runs through the generator itself;</li>
 * <li>tier 4: a storm line, the kite stays up in a thunderstorm at half the output, a crowded sky costs only half as much
 * (see {@link #opennessFactor(int, double)}), and it flies <b>two kites</b> (see {@link #kites(int)});</li>
 * <li>tier 5: an insulated tether and storm sail, thunder costs nothing, the kites do not care how crowded the sky is, and it flies two
 * kites as well.</li>
 * </ul>
 * <b>Two kites.</b> The peak of tier 4 and 5 is the maximum of the <i>pair</i>: the first kite makes {@link #SLOT_SHARES}[0] = 60 % of
 * it, the second one, which flies in the wind shadow of the first, 40 % (see {@link #slotShare(int, int)}), so one kite alone never
 * reaches the peak and both together at full tether, clear weather and a free sky make exactly the peak, never more.
 * A thunderstorm never attracts lightning to a kite (no lightning is ever spawned): a kite that has to give way is simply reeled in.
 * <p>
 * <b>Economy.</b> The peaks follow the battery ladder 15 / 75 / 560 / 5600 / 56000 FE/t. A kite is up around the clock and on open
 * ground it climbs to the full tether, so the daily yield is anchored on the real model: the tether fully out, a free sky, the real
 * gust curve and a weather mix of {@link #TYPICAL_CLEAR} / {@link #TYPICAL_RAIN} / {@link #TYPICAL_THUNDER} (see
 * {@link #typicalDailyYield(double, int)}). That is about 9 to 11 times the battery of every tier per MC day, i.e. a battery is full in
 * about a tenth of a day, which is the same range the panels are held to.
 */
final class KiteMath {
    /** The highest tier. */
    static final int MAX_TIER = 5;
    /** The ticks of one MC day. */
    static final int DAY_TICKS = 24_000;
    /** The weakest gust: the wind never drops below this share of the peak. */
    static final double WIND_MIN = 0.2;
    /** The mean of {@link #wind(long)} over time. */
    static final double WIND_MEAN = WIND_MIN + (1 - WIND_MIN) * 0.5;
    /** Share of a typical day with clear weather. */
    static final double TYPICAL_CLEAR = 0.75;
    /** Share of a typical day with rain. */
    static final double TYPICAL_RAIN = 0.20;
    /** Share of a typical day with a thunderstorm. */
    static final double TYPICAL_THUNDER = 0.05;
    /** The period in ticks of the slow swell of the wind. */
    private static final long SWELL_TICKS = 1_700;
    /** The period in ticks of the quick gusts on top of the swell. */
    private static final long GUST_TICKS = 611;

    private static final int[] TETHER = {32, 64, 128, 256, 512};
    /** The most kites a block flies (tier 4 and 5). */
    static final int MAX_KITES = 2;
    /** The share of the peak that the kite of a slot makes; diminishing (the second one sits in the wind shadow) and adds up to 1. */
    private static final double[] SLOT_SHARES = {0.6, 0.4};

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
     * How many kites a block of a tier flies.
     *
     * @param tier the tier, 1..5
     * @return 2 for tier 4 and 5, else 1
     */
    static int kites(int tier) {
        return tier >= 4 ? MAX_KITES : 1;
    }

    /**
     * The share of the peak that the kite of a slot makes: the shares of all kites of a tier add up to 1, so the peak is never exceeded.
     *
     * @param tier the tier, 1..5
     * @param slot the slot, 0 for the first kite
     * @return 1 for a block with one kite, else 0.6 for the first and 0.4 for the second; 0 for a slot the tier does not have
     */
    static double slotShare(int tier, int slot) {
        if (slot < 0 || slot >= kites(tier)) return 0;
        return kites(tier) == 1 ? 1 : SLOT_SHARES[slot];
    }

    /**
     * Whether the kite ignores obstructions: blocks on the way up do not stop it and the crowded air around it costs nothing.
     *
     * @param tier the tier, 1..5
     * @return {@code true} for tier 3
     */
    static boolean ignoresObstructions(int tier) {
        return tier == 3;
    }

    /**
     * The weather at the kite. Rain and snow both count (a kite in a cold biome, or high in the air where it is cold enough to snow,
     * is just as wet): the temperature is ignored and only whether the biome has precipitation at all matters.
     *
     * @param raining    whether it rains in the level
     * @param thundering whether there is a thunderstorm in the level
     * @param exposed    whether the kite sees the sky and the biome has precipitation (rain or snow)
     * @return the weather at the kite; a dry biome or a covered kite stays clear
     */
    static Weather weather(boolean raining, boolean thundering, boolean exposed) {
        if (!raining || !exposed) return Weather.CLEAR;
        return thundering ? Weather.THUNDER : Weather.RAIN;
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
     * Whether the way up needs a free cross of five cells (the column and its four neighbours); tiers 3 and up need no more than the
     * column (tier 3 not even that, see {@link #ignoresObstructions(int)}).
     *
     * @param tier the tier, 1..5
     * @return {@code true} for tier 1 and 2
     */
    static boolean wide(int tier) {
        return tier < 3;
    }

    /**
     * The gust: a slow swell and quicker gusts, deterministic in the game time so it can be tested. It is between {@link #WIND_MIN}
     * and 1 and averages {@link #WIND_MEAN}.
     *
     * @param time the game time in ticks (plus a per position offset, so kites do not all gust together)
     * @return the share of the wind, {@code WIND_MIN..1}
     */
    static double wind(long time) {
        double swell = Math.sin(2 * Math.PI * Math.floorMod(time, SWELL_TICKS) / SWELL_TICKS);
        double gust = Math.sin(2 * Math.PI * Math.floorMod(time, GUST_TICKS) / GUST_TICKS);
        return WIND_MIN + (1 - WIND_MIN) * 0.25 * (2 + swell + gust);
    }

    /**
     * The altitude the kite can fly at: it climbs along the free air above the block and stops at the last free block before an
     * obstacle. Every block of the way is checked, so a plate of any thickness stops the kite.
     *
     * @param tier      the tier, 1..5
     * @param blockedAt whether the air {@code h} blocks above the top of the block is blocked
     * @return the altitude in blocks, 0 if the air directly above the block is blocked; the whole tether for a kite that ignores
     *         obstructions
     */
    static int reach(int tier, IntPredicate blockedAt) {
        int max = tether(tier);
        if (ignoresObstructions(tier)) return max;
        for (int h = 1; h <= max; h++) {
            if (blockedAt.test(h)) return h - 1;
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
     * What a crowded sky costs: tier 1-2 pay the full share of blocked air around the kite, tier 4 half of it, tier 3 and 5 nothing.
     *
     * @param tier     the tier, 1..5
     * @param openness the free share of the air around the kite, 0..1
     * @return the factor to multiply with the output, 0..1
     */
    static double opennessFactor(int tier, double openness) {
        double o = Math.clamp(openness, 0, 1);
        return tier == 3 || tier >= 5 ? 1 : tier == 4 ? (1 + o) / 2 : o;
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
     * @param wind     the gust, see {@link #wind(long)}
     * @return the FE per tick, may be fractional; 0 without a sky
     */
    static double output(double peak, int tier, double altitude, double openness, Weather weather, boolean sky, double wind) {
        if (!sky) return 0;
        return peak * share(tier, altitude) * opennessFactor(tier, openness) * weatherFactor(tier, weather) * Math.clamp(wind, 0, 1);
    }

    /**
     * What the kite of a slot makes right now: its {@link #slotShare share} of what {@link #output} gives for the whole peak.
     *
     * @param peak     the peak of the tier in FE/t (of all kites together)
     * @param tier     the tier, 1..5
     * @param slot     the slot of the kite
     * @param altitude the altitude of the kite above the block in blocks
     * @param openness the free share of the air around the kite, 0..1
     * @param weather  the weather at the kite
     * @param sky      whether the dimension has a sky
     * @param wind     the gust, see {@link #wind(long)}
     * @return the FE per tick, may be fractional
     */
    static double slotOutput(double peak, int tier, int slot, double altitude, double openness, Weather weather, boolean sky, double wind) {
        return slotShare(tier, slot) * output(peak, tier, altitude, openness, weather, sky, wind);
    }

    /**
     * The FE a kite makes on a typical day: the tether fully out and a free sky (what a winch gets on open ground), the real gust
     * curve over one MC day and the typical weather mix.
     *
     * @param peak the peak of the tier in FE/t
     * @param tier the tier, 1..5
     * @return the FE per MC day, of all kites of the tier flying at the same height together (their shares add up to 1)
     */
    static double typicalDailyYield(double peak, int tier) {
        double total = 0;
        for (int t = 0; t < DAY_TICKS; t++) {
            double wind = wind(t);
            total += TYPICAL_CLEAR * output(peak, tier, tether(tier), 1, Weather.CLEAR, true, wind)
                    + TYPICAL_RAIN * output(peak, tier, tether(tier), 1, Weather.RAIN, true, wind)
                    + TYPICAL_THUNDER * output(peak, tier, tether(tier), 1, Weather.THUNDER, true, wind);
        }
        return total;
    }

    /** What keeps a kite from its peak, the first that applies; it is shown to the player. */
    enum Limit {
        /** Nothing: the tether is out, the sky is free and the weather does not matter. */
        NONE,
        /** The dimension has no sky. */
        NO_SKY,
        /** The kite is reeled in because of a thunderstorm. */
        STORM,
        /** A block is in the way up. */
        BLOCKED,
        /** Rain or a thunderstorm costs part of the output. */
        WEATHER,
        /** The air around the kite is crowded. */
        CROWDED
    }

    /**
     * Why the output is below the peak (the gust is not counted, it is always there).
     *
     * @param tier     the tier, 1..5
     * @param sky      whether the dimension has a sky
     * @param weather  the weather at the kite
     * @param reach    the free altitude on the way up
     * @param openness the free share of the air around the kite
     * @return the first limit that applies
     */
    static Limit limit(int tier, boolean sky, Weather weather, int reach, double openness) {
        if (!sky) return Limit.NO_SKY;
        double factor = weatherFactor(tier, weather);
        if (factor == 0) return Limit.STORM;
        if (reach < tether(tier)) return Limit.BLOCKED;
        if (factor < 1) return Limit.WEATHER;
        return opennessFactor(tier, openness) < 0.75 ? Limit.CROWDED : Limit.NONE;
    }

    /**
     * The redstone signal of a comparator: 0 for no output, else 1..15 by the share of the peak.
     *
     * @param output the FE per tick now
     * @param peak   the peak of the tier in FE/t
     * @return 0..15
     */
    static int signal(double output, double peak) {
        if (output <= 0 || peak <= 0) return 0;
        return (int) Math.clamp(1 + (long) (14 * output / peak), 1, 15);
    }
}
