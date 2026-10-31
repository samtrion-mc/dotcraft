package net.samtrion.dotcraft.energy.vibration;

/**
 * The pure rules of the vibration generators, free of any Minecraft types so unit tests can carry the balance. A tier is
 * {@code 1..5}; a frequency is the vibration frequency of a game event, {@code 1..15} (what a sculk sensor calls it).
 * <p>
 * The economy: a game event of frequency {@code f} is worth {@code f / 15} of {@link #EVENT_TICKS} ticks of the peak, so the loudest
 * event gives {@link #EVENT_TICKS} ticks of peak and a faint one a fraction. The peak (20 / 75 / 560 / 5600 / 56000 FE/t by default)
 * is the most a generator can make at any moment, its burst power. What is made over time is limited by a {@link VibrationBucket}
 * that refills with {@link #refill} per tick and holds {@link #BURST_TICKS} ticks of peak. The refill is the {@link #duty} of the tier
 * times the peak; the duty grows with the tier (2/9, 1/3, 4/9, 5/9, 2/3), anchored so that the best typical case, a player who mines or
 * fights all day (an event every 10 ticks, bucket-limited), makes about 2.4 / 6 / 8 / 10 / 12 batteries of the same tier per MC day
 * (see {@link #dailyCeiling}; tier 1 makes 2.4: its refill of 4 FE/t is 96 000 FE per day, 2.4 of the 40 000 FE battery). No amount of events, from a player
 * or from a machine that loops an event, makes more than that on average. A light player (an event every 100 ticks) makes a fraction of it.
 * <p>
 * Boss bonus: a game event caused by an entity of the config list {@code vibration.bosses} (the ender dragon, wither, warden and elder guardian by default) is worth {@link #BOSS_FACTOR} times as much,
 * whatever the tier. The normal bucket pays what it can; the rest comes from a second bucket, the bonus bucket, that refills with the
 * rest of the peak ({@link #bonusRefill}, so both buckets together refill with exactly the peak) and holds {@link #BOSS_FACTOR} times
 * the burst. A boss fight may so beat the duty ceiling, but never the peak of the tier on average ({@link #absoluteMaxYield}).
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
    /** How many times a game event caused by a boss is worth a normal one, whatever the tier. */
    public static final int BOSS_FACTOR = 5;
    /** Ticks of one MC day. */
    public static final int DAY_TICKS = 24000;

    /** Per tier: the share of the peak the bucket refills per tick, the duty the daily yield of 2.4 / 6 / 8 / 10 / 12 batteries is anchored to. */
    private static final double[] DUTY = {2.0 / 9, 1.0 / 3, 4.0 / 9, 5.0 / 9, 2.0 / 3};
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
     * The duty of a tier: the share of the peak the generator makes on average at most from normal events.
     *
     * @param tier the tier, 1..5
     * @return a share between 0 and 1
     */
    public static double duty(int tier) {
        return DUTY[tier - 1];
    }

    /**
     * The refill of the bucket per tick, the average output the generator can reach at most from normal events.
     *
     * @param tier the tier, 1..5
     * @param peak the peak of the generator in FE/t
     * @return the FE per tick: the duty of the tier times the peak, rounded, at least 1 for a positive peak
     */
    public static long refill(int tier, int peak) {
        return peak <= 0 ? 0 : Math.max(1, Math.round(peak * duty(tier)));
    }

    /**
     * The refill per tick of the bonus bucket that only boss events use: what is left of the peak after {@link #refill}.
     *
     * @param tier the tier, 1..5
     * @param peak the peak of the generator in FE/t
     * @return the FE per tick; {@code refill + bonusRefill} is the peak
     */
    public static long bonusRefill(int tier, int peak) {
        return Math.max(0, peak - refill(tier, peak));
    }

    /**
     * The size of the bonus bucket.
     *
     * @param peak the peak of the generator in FE/t
     * @return {@link #BOSS_FACTOR} times the {@link #burst}, so a loudest boss event always fits
     */
    public static long bonusBurst(int peak) {
        return burst(peak) * BOSS_FACTOR;
    }

    /**
     * The FE a game event caused by a boss is worth, before the buckets limit it.
     *
     * @param peak      the peak of the generator in FE/t
     * @param frequency the vibration frequency of the event, 1..15
     * @return {@link #BOSS_FACTOR} times {@link #eventEnergy}
     */
    public static long bossEventEnergy(int peak, int frequency) {
        return eventEnergy(peak, frequency) * BOSS_FACTOR;
    }

    /**
     * Grants the FE of one game event that was heard: the normal bucket pays first, and for a boss event what it cannot pay comes
     * from the bonus bucket.
     *
     * @param tier   the tier, 1..5
     * @param peak   the peak of the generator in FE/t
     * @param now    the game time
     * @param energy what the event is worth normally, see {@link #eventEnergy}
     * @param boss   whether a boss caused the event (then it is worth {@link #BOSS_FACTOR} times as much)
     * @param bucket the normal bucket
     * @param bonus  the bonus bucket of boss events
     * @return the FE granted
     */
    public static long grant(int tier, int peak, long now, long energy, boolean boss, VibrationBucket bucket, VibrationBucket bonus) {
        long wanted = boss ? energy * BOSS_FACTOR : energy;
        long granted = bucket.take(now, wanted, refill(tier, peak), burst(peak));
        if (boss && granted < wanted) granted += bonus.take(now, wanted - granted, bonusRefill(tier, peak), bonusBurst(peak));
        return granted;
    }

    /**
     * The ceiling for normal events: whatever the world does, they make no more than this over a span of ticks.
     *
     * @param tier  the tier, 1..5
     * @param peak  the peak of the generator in FE/t
     * @param ticks the length of the span
     * @return the most FE made over the span
     */
    public static long maxYield(int tier, int peak, long ticks) {
        return burst(peak) + refill(tier, peak) * ticks;
    }

    /**
     * The ceiling with boss events: both buckets together never pass the peak per tick on average.
     *
     * @param peak  the peak of the generator in FE/t
     * @param ticks the length of the span
     * @return the most FE made over the span
     */
    public static long absoluteMaxYield(int peak, long ticks) {
        return burst(peak) + bonusBurst(peak) + peak * ticks;
    }

    /**
     * The FE of one MC day at the duty ceiling, which a player busy all day or a machine reaches but cannot pass (without bosses).
     *
     * @param tier the tier, 1..5
     * @param peak the peak of the generator in FE/t
     * @return the FE per day
     */
    public static double dailyCeiling(int tier, int peak) {
        return (double) refill(tier, peak) * DAY_TICKS;
    }
}
