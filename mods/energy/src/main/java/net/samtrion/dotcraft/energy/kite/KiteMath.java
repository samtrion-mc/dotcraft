package net.samtrion.dotcraft.energy.kite;

import java.util.function.IntPredicate;

/**
 * The math of the kite wind generator, free of Minecraft types so a unit test can run it.
 * <p>
 * <b>Altitude.</b> The kite flies straight up from the block on a tether of {@link #tether(int)} blocks (16 / 24 / 32 / 48 / 64 for
 * tier 1-5). The altitude is measured above the top of the block and is <i>not</i> clamped to the build height: a kite on a tower or on a
 * mountain flies on above the top of the world. Every block of the way is checked (see {@link #reach(int, IntPredicate)}), so no roof,
 * however thin, is flown through.
 * <p>
 * <b>Output</b> = base x lengthFactor x openness x weather x wind. The tier of the block fixes only the <i>base</i> (80 / 400 / 2600 /
 * 17333 / 128000 FE/t, see the common config); the length of the tether is the yield factor: {@link #lengthFactor(int, double)} =
 * effective length / {@link #REFERENCE_LENGTH} (64, the longest tether), where the effective length is what the kite actually reaches
 * (its altitude, at most the tether of the tier). The best case, {@link #peak(double, int)} = base x tether / 64, is therefore
 * 20 / 150 / 1300 / 12999.75 / 128000 FE/t for tier 1-5 (tether 16 / 24 / 32 / 48 / 64), reached with the tether fully out, a free
 * sky, clear weather and a gust at its best; {@code openness} is the free share of the air around the kite and {@link #wind(long)} is the
 * gust: the wind is never steady and swings between {@link #WIND_MIN} and 1 with a mean of {@link #WIND_MEAN}, so even a kite in the
 * open makes only about 60 % of that peak over a day.
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
 * <b>Two kites.</b> The output of tier 4 and 5 is that of the <i>pair</i>: the first kite makes {@link #SLOT_SHARES}[0] = 60 % of
 * the base, the second one, which flies in the wind shadow of the first, 40 % (see {@link #slotShare(int, int)}), so one kite alone
 * never reaches the peak and both together at full tether, clear weather and a free sky make exactly the peak, never more.
 * A kite that has to give way in a thunderstorm (tier 1-3) is simply reeled in and never struck.
 * <p>
 * <b>Wire.</b> The tether is not there by default: the player feeds iron wire into the generator, 1 wire = 1 block of tether, at most
 * {@link #tether(int)} of the tier (see {@link #wireStored(int, int, int)}). A kite reaches at most {@code min(wire, tether, free
 * altitude)} (see {@link #flightTarget(int, int, int)}); without wire no kite is launched and the output is 0.
 * <p>
 * <b>Play.</b> The single kite of tier 1-3 hangs straight above its block, as before (a single kite has nothing to keep away from, and
 * the simple layout keeps it in the chunk of its block). The two kites of tier 4 and 5 each get a horizontal play of +-{@link #SPREAD}
 * blocks around the vertical axis of the block, slot specific and slowly wandering (see {@link #offset(int, int, long, boolean)}), and
 * stay at least {@link #MIN_KITE_GAP} blocks apart. In a thunderstorm the kite is guided tightly: its horizontal offset is clamped to a
 * radius of {@link #THUNDER_RADIUS} blocks around the axis (the play of +-4 lies inside that anyway, the clamp is the guarantee), and a
 * natural lightning bolt within {@link #THUNDER_RADIUS} blocks of a kite or its tether is attracted to the generator (see
 * {@link #attracts}). The 1 % per second spawn of a visual bolt stays as the way to get bolts when natural ones rarely land there.
 * <p>
 * <b>Distance.</b> Two kite generators with a tether closer than {@link #MIN_DISTANCE} blocks horizontally (and within the tether
 * reach vertically, see {@link #tooClose}) would tangle: both are blocked, their kites are reeled in and the output is 0.
 * <p>
 * <b>Economy.</b> The ladder is deliberately gentle: the best case per MC day (tether fully out, a free sky, the real gust curve and a
 * weather mix of {@link #TYPICAL_CLEAR} / {@link #TYPICAL_RAIN} / {@link #TYPICAL_THUNDER}, see {@link #typicalDailyYield(double, int)})
 * fills the battery of the same tier about 10 / 19 / 24 / 24 / 25 times for tier 1-5 (the maxima 20 / 150 / 1300 / 13000 / 128000 FE/t are chosen so that FE/t scales
 * with the battery, which grows faster than the tether). The tier mostly buys perks (weather, obstruction, two kites) and a longer tether.
 */
public final class KiteMath {
    /** The highest tier. */
    static final int MAX_TIER = 5;
    /** The ticks of one MC day. */
    static final int DAY_TICKS = 24_000;
    /**
     * The reference tether length in blocks: a kite that reaches this far has a {@link #lengthFactor length factor} of exactly 1. It is
     * the longest tether (tier 5), so the factor is 0.25 / 0.375 / 0.5 / 0.75 / 1 for a fully extended tether of tier 1-5.
     */
    public static final int REFERENCE_LENGTH = 64;
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
    /** The chance per second (one roll per read of the world) that a kite that stays up in a thunderstorm is struck by lightning. */
    static final double LIGHTNING_CHANCE_PER_SECOND = 0.01;
    /** The ticks a kite is safe from the next bolt after a strike (five MC minutes), so a strike stays a rare jackpot. */
    static final int LIGHTNING_COOLDOWN_TICKS = 6_000;
    /** The period in ticks of the slow swell of the wind. */
    private static final long SWELL_TICKS = 1_700;
    /** The period in ticks of the quick gusts on top of the swell. */
    private static final long GUST_TICKS = 611;

    /** The minimum horizontal distance in blocks between the block positions of two kite generators with a tether; closer ones block each other. */
    public static final int MIN_DISTANCE = 16;
    /** The horizontal play in blocks of a kite of tier 4 and 5 around the vertical axis of its block, per axis. */
    public static final double SPREAD = 4;
    /** The least distance in blocks between the two kites of one block (their play is arranged so that it always holds). */
    static final double MIN_KITE_GAP = 3;
    /** In a thunderstorm the horizontal distance of a kite from the axis of its block is at most this many blocks; lightning within it is attracted. */
    public static final double THUNDER_RADIUS = 6;

    private static final int[] TETHER = {16, 24, 32, 48, 64};
    /** The most kites a block flies (tier 4 and 5). */
    public static final int MAX_KITES = 2;
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
    public static int tether(int tier) {
        return TETHER[tier - 1];
    }

    /**
     * How many kites a block of a tier flies.
     *
     * @param tier the tier, 1..5
     * @return 2 for tier 4 and 5, else 1
     */
    public static int kites(int tier) {
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
     * The yield factor of the tether: the effective length (the altitude the kite really reaches, at most the tether of the tier) as a
     * multiple of {@link #REFERENCE_LENGTH}. It never exceeds 1.
     *
     * @param tier     the tier, 1..5
     * @param altitude the altitude of the kite above the block in blocks, not limited by the build height
     * @return 0 for no length, 1 at the reference length, {@code tether / reference} at the most (0.25 / 0.375 / 0.5 / 0.75 / 1)
     */
    static double lengthFactor(int tier, double altitude) {
        return Math.clamp(altitude, 0, tether(tier)) / REFERENCE_LENGTH;
    }

    /**
     * The best case of a tier: the base with the tether fully out, a free sky, clear weather and the best gust. This is also what the
     * comparator signal and the buffer of the block are scaled to.
     *
     * @param base the base of the tier in FE/t
     * @param tier the tier, 1..5
     * @return the most FE per tick, of all kites together
     */
    static double peak(double base, int tier) {
        return base * lengthFactor(tier, tether(tier));
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
     * @param base     the base of the tier in FE/t
     * @param tier     the tier, 1..5
     * @param altitude the altitude of the kite above the block in blocks (its effective length)
     * @param openness the free share of the air around the kite, 0..1
     * @param weather  the weather at the kite
     * @param sky      whether the dimension has a sky
     * @param wind     the gust, see {@link #wind(long)}
     * @return the FE per tick, may be fractional; 0 without a sky
     */
    static double output(double base, int tier, double altitude, double openness, Weather weather, boolean sky, double wind) {
        if (!sky) return 0;
        return base * lengthFactor(tier, altitude) * opennessFactor(tier, openness) * weatherFactor(tier, weather) * Math.clamp(wind, 0, 1);
    }

    /**
     * What the kite of a slot makes right now: its {@link #slotShare share} of what {@link #output} gives for the whole base.
     *
     * @param base     the base of the tier in FE/t (of all kites together)
     * @param tier     the tier, 1..5
     * @param slot     the slot of the kite
     * @param altitude the altitude of the kite above the block in blocks
     * @param openness the free share of the air around the kite, 0..1
     * @param weather  the weather at the kite
     * @param sky      whether the dimension has a sky
     * @param wind     the gust, see {@link #wind(long)}
     * @return the FE per tick, may be fractional
     */
    static double slotOutput(double base, int tier, int slot, double altitude, double openness, Weather weather, boolean sky, double wind) {
        return slotShare(tier, slot) * output(base, tier, altitude, openness, weather, sky, wind);
    }

    /**
     * The FE a kite makes on a typical day: the tether fully out and a free sky (what a winch gets on open ground), the real gust
     * curve over one MC day and the typical weather mix.
     *
     * @param base the base of the tier in FE/t
     * @param tier the tier, 1..5
     * @return the FE per MC day, of all kites of the tier flying at the same height together (their shares add up to 1)
     */
    static double typicalDailyYield(double base, int tier) {
        double total = 0;
        for (int t = 0; t < DAY_TICKS; t++) {
            double wind = wind(t);
            total += TYPICAL_CLEAR * output(base, tier, tether(tier), 1, Weather.CLEAR, true, wind)
                    + TYPICAL_RAIN * output(base, tier, tether(tier), 1, Weather.RAIN, true, wind)
                    + TYPICAL_THUNDER * output(base, tier, tether(tier), 1, Weather.THUNDER, true, wind);
        }
        return total;
    }

    /**
     * Whether a kite can be struck: only during a thunderstorm at the kite, and only a kite that stays up in it (tiers 1-3 are reeled
     * in and never struck), when its cooldown is over and the roll is under the chance.
     *
     * @param tier         the tier, 1..5
     * @param weather      the weather at the kite
     * @param cooldownLeft the ticks of cooldown that are left, 0 or less if none
     * @param roll         a random number in 0..1
     * @return {@code true} if the kite is struck now
     */
    static boolean struck(int tier, Weather weather, long cooldownLeft, double roll) {
        return weather == Weather.THUNDER && weatherFactor(tier, Weather.THUNDER) > 0 && cooldownLeft <= 0 && roll < LIGHTNING_CHANCE_PER_SECOND;
    }

    /**
     * How the FE of a bolt is shared out: first into the buffer, what does not fit goes to the neighbours as a burst, what they do not
     * take is lost.
     *
     * @param energy   the FE of the bolt
     * @param free     the room in the buffer
     * @param accepted the FE the neighbours take of the rest
     * @return the share of the buffer, of the neighbours and the FE that is lost
     */
    static Credit credit(int energy, int free, int accepted) {
        int stored = Math.clamp(free, 0, Math.max(energy, 0));
        int rest = Math.max(energy, 0) - stored;
        int given = Math.clamp(accepted, 0, rest);
        return new Credit(stored, given, rest - given);
    }

    /**
     * The share of one bolt.
     *
     * @param stored the FE into the buffer
     * @param pushed the FE the neighbours took
     * @param lost   the FE that was lost
     */
    record Credit(int stored, int pushed, int lost) {}

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
     * @param peak   the best case of the tier in FE/t, see {@link #peak(double, int)}
     * @return 0..15
     */
    static int signal(double output, double peak) {
        if (output <= 0 || peak <= 0) return 0;
        return (int) Math.clamp(1 + (long) (14 * output / peak), 1, 15);
    }

    /**
     * The wire a generator holds after an insert: 1 wire is 1 block of tether, at most the tether of the tier.
     *
     * @param tier   the tier, 1..5
     * @param stored the wire held now
     * @param added  the wire offered
     * @return the wire held after the insert, between {@code stored} and {@link #tether(int)}
     */
    static int wireStored(int tier, int stored, int added) {
        return (int) Math.min((long) stored + Math.max(added, 0), Math.max(stored, tether(tier)));
    }

    /**
     * The altitude a kite flies to: no more than the wire, the tether of the tier and the free way up allow.
     *
     * @param tier  the tier, 1..5
     * @param wire  the wire held
     * @param reach the free altitude on the way up, see {@link #reach(int, IntPredicate)}
     * @return the altitude in blocks, 0 without wire
     */
    static int flightTarget(int tier, int wire, int reach) {
        return Math.max(0, Math.min(Math.min(wire, tether(tier)), reach));
    }

    /**
     * A horizontal offset from the axis of the block.
     *
     * @param x the offset in x in blocks
     * @param z the offset in z in blocks
     */
    record Offset(double x, double z) {}

    /**
     * Where a kite hangs horizontally relative to the vertical axis of its block, deterministic in the game time and the slot. Tier
     * 1-3 hang on the axis. The kites of tier 4 and 5 wander slowly inside +-{@link #SPREAD}: slot 0 in x between -4 and -1.5, slot 1 in
     * x between 1.5 and 4 (so they are at least {@link #MIN_KITE_GAP} apart), both in z between -4 and 4, with a different period per
     * slot and axis. In a thunderstorm the offset is clamped to {@link #THUNDER_RADIUS} around the axis.
     *
     * @param tier  the tier, 1..5
     * @param slot  the slot of the kite
     * @param time  the game time in ticks
     * @param storm whether the kite is guided tightly in a thunderstorm
     * @return the offset
     */
    static Offset offset(int tier, int slot, long time, boolean storm) {
        if (kites(tier) == 1) return new Offset(0, 0);
        int s = slot == 0 ? 0 : 1;
        double px = 1_300 + 170.0 * s;
        double pz = 1_900 - 230.0 * s;
        double x = (s == 0 ? -2.75 : 2.75) + 1.25 * Math.sin(2 * Math.PI * Math.floorMod(time, (long) px) / px + s);
        double z = SPREAD * Math.sin(2 * Math.PI * Math.floorMod(time, (long) pz) / pz + 2 * s);
        var o = new Offset(x, z);
        return storm ? clamp(o, THUNDER_RADIUS) : o;
    }

    /**
     * Limits an offset to a horizontal radius, keeping its direction.
     *
     * @param o      the offset
     * @param radius the radius in blocks
     * @return the offset itself if it is inside, else the point on the circle in the same direction
     */
    static Offset clamp(Offset o, double radius) {
        double len = Math.hypot(o.x(), o.z());
        return len <= radius ? o : new Offset(o.x() * radius / len, o.z() * radius / len);
    }

    /**
     * Whether two kite generators tangle: closer than {@link #MIN_DISTANCE} horizontally and not further apart vertically than the
     * tether of the longer one. It is symmetric, so both are blocked.
     *
     * @param dx    the x distance of the block positions
     * @param dy    the y distance
     * @param dz    the z distance
     * @param tier  the tier of one
     * @param other the tier of the other
     * @return {@code true} if both are blocked
     */
    static boolean tooClose(int dx, int dy, int dz, int tier, int other) {
        return Math.hypot(dx, dz) < MIN_DISTANCE && Math.abs(dy) <= Math.max(tether(tier), tether(other));
    }

    /**
     * Whether a lightning bolt is attracted to a kite: it lands within {@link #THUNDER_RADIUS} blocks of the kite or of any point of
     * the tether, the straight line from the top of the block (a) to the kite (k).
     *
     * @param b the position of the bolt, {x, y, z}
     * @param a the top of the block where the tether starts, {x, y, z}
     * @param k the kite, {x, y, z}
     * @return {@code true} if the bolt counts as a strike on the kite
     */
    static boolean attracts(double[] b, double[] a, double[] k) {
        double[] v = {k[0] - a[0], k[1] - a[1], k[2] - a[2]};
        double len2 = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
        double t = len2 == 0 ? 0 : Math.clamp(((b[0] - a[0]) * v[0] + (b[1] - a[1]) * v[1] + (b[2] - a[2]) * v[2]) / len2, 0, 1);
        double dx = b[0] - (a[0] + t * v[0]);
        double dy = b[1] - (a[1] + t * v[1]);
        double dz = b[2] - (a[2] + t * v[2]);
        return dx * dx + dy * dy + dz * dz <= THUNDER_RADIUS * THUNDER_RADIUS;
    }
}
