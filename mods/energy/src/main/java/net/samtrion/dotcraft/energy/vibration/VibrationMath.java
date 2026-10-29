package net.samtrion.dotcraft.energy.vibration;

/**
 * The pure rules of the vibration generators, free of any Minecraft types so unit tests can carry the balance. A tier is
 * {@code 1..5}; a frequency is the vibration frequency of a game event, {@code 1..15} (what a sculk sensor calls it).
 * <p>
 * The economy: a game event of frequency {@code f} is worth {@code f / 15} of {@link #EVENT_TICKS} ticks of the peak, so the loudest
 * event gives {@link #EVENT_TICKS} ticks of peak and a faint one a fraction. What is made is limited by a {@link VibrationBucket} that
 * refills with {@link #refill} (a {@link #DUTY} share of the peak) per tick and holds {@link #BURST_TICKS} ticks of peak. That is a
 * hard ceiling, see {@link #maxYield}: no amount of events, from a player or from a machine that loops an event, makes more than
 * {@link #DUTY} of the peak on average, about {@code 4.8} batteries of the tier per MC day, the order of a panel. A player who mines
 * or fights all day (an event every 10 ticks) is bucket-limited and reaches this ceiling, a light player (an event every 100 ticks)
 * makes about 1.4 batteries per day; a machine can reach the ceiling too, but never pass it.
 * <p>
 * What a tier hears, with the vanilla frequencies of the events in the tag (step 1, hit_ground 2, entity_damage 7, block_destroy 12,
 * block_place 13, lightning_strike 14, entity_die 15, explode 15): tier 1 the loud shocks (block broken or placed, deaths,
 * explosions, lightning), tier 2 also damage, tier 3 also landings, tier 4 the same through wool, tier 5 also steps.
 */
public final class VibrationMath {
    /** The highest tier. */
    public static final int MAX_TIER = 5;
    /** Ticks of peak a game event of the highest frequency is worth. */
    public static final int EVENT_TICKS = 10;
    /** The highest vibration frequency. */
    public static final int MAX_FREQUENCY = 15;
    /** Ticks of peak the bucket holds: the burst a generator may make at once. It holds more than one loudest event. */
    public static final int BURST_TICKS = 20;
    /** The refill of the bucket is the peak divided by this, so the average output can never pass {@code 1 / REFILL_DIVISOR} of the peak. */
    public static final int REFILL_DIVISOR = 4;
    /** The share of the ticks of a day a generator can be at its peak at most (the ceiling the economy of the defaults is anchored to). */
    public static final double DUTY = 1.0 / REFILL_DIVISOR;
    /** Ticks of one MC day. */
    public static final int DAY_TICKS = 24000;

    /** Per tier: the radius in blocks in which the generator hears game events (perk: the range grows). A game event reaches 16 blocks at most, so no tier hears further. */
    private static final int[] RADIUS = {5, 7, 9, 12, 16};
    /** Per tier: the lowest frequency it hears (perk: fainter shocks count: damage from tier 2, landings from tier 3, steps at tier 5). */
    private static final int[] MIN_FREQUENCY = {8, 7, 2, 2, 1};
    /** The first tier whose vibrations are not stopped by wool (perk: hears through insulation). */
    private static final int PIERCING_TIER = 4;

    /** Not instantiable. */
    private VibrationMath() {}

    /**
     * The listening radius of a tier.
     *
     * @param tier the tier, 1..5
     * @return the radius in blocks
     */
    public static int radius(int tier) {
        return RADIUS[tier - 1];
    }

    /**
     * The sensitivity of a tier.
     *
     * @param tier the tier, 1..5
     * @return the lowest vibration frequency the generator hears
     */
    public static int minFrequency(int tier) {
        return MIN_FREQUENCY[tier - 1];
    }

    /**
     * Whether a tier hears a game event of a frequency; the frequency 0 of a game event that is no vibration is never heard.
     *
     * @param tier      the tier, 1..5
     * @param frequency the vibration frequency of the event
     * @return {@code true} if the generator takes energy from it
     */
    public static boolean hears(int tier, int frequency) {
        return frequency > 0 && frequency <= MAX_FREQUENCY && frequency >= minFrequency(tier);
    }

    /**
     * Whether the vibrations of a tier pass wool (blocks that occlude vibration signals).
     *
     * @param tier the tier, 1..5
     * @return {@code true} from tier {@value #PIERCING_TIER} on
     */
    public static boolean pierces(int tier) {
        return tier >= PIERCING_TIER;
    }

    /**
     * The FE a game event is worth, before the bucket limits it: loud events count more, linear in the frequency.
     *
     * @param peak      the peak of the generator in FE/t
     * @param frequency the vibration frequency of the event, 1..15
     * @return the FE; 0 for a frequency outside 1..15
     */
    public static long eventEnergy(int peak, int frequency) {
        if (frequency < 1 || frequency > MAX_FREQUENCY) return 0;
        return (long) peak * EVENT_TICKS * frequency / MAX_FREQUENCY;
    }

    /**
     * The size of the bucket.
     *
     * @param peak the peak of the generator in FE/t
     * @return the most FE that can be made at once
     */
    public static long burst(int peak) {
        return (long) peak * BURST_TICKS;
    }

    /**
     * The refill of the bucket per tick, the average output the generator can reach at most.
     *
     * @param peak the peak of the generator in FE/t
     * @return the FE per tick, at least 1 for a positive peak
     */
    public static long refill(int peak) {
        return Math.ceilDiv((long) peak, REFILL_DIVISOR);
    }

    /**
     * The provable ceiling: whatever the world does, a generator makes no more than this over a span of ticks.
     *
     * @param peak  the peak of the generator in FE/t
     * @param ticks the length of the span
     * @return the most FE made over the span
     */
    public static long maxYield(int peak, long ticks) {
        return burst(peak) + refill(peak) * ticks;
    }

    /**
     * The FE of one MC day at the ceiling, which a player busy all day or a machine reaches but cannot pass.
     *
     * @param peak the peak of the generator in FE/t
     * @return the FE per day
     */
    public static double dailyCeiling(int peak) {
        return (double) refill(peak) * DAY_TICKS;
    }
}
