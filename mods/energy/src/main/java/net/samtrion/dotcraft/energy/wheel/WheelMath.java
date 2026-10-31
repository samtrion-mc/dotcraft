package net.samtrion.dotcraft.energy.wheel;

/**
 * The pure math of the mob wheels, free of any Minecraft types so unit tests can carry the balance. A tier is {@code 1..5}.
 * <p>
 * A wheel holds one mob at a time (see {@link WheelBlockEntity}). The output is {@code peak * share}, where the share of the peak is
 * {@code min(1, weight * activity / FULL_WEIGHT)}, see {@link #share}: the {@linkplain #weight weight} of the mob comes from its
 * maximum health and movement speed, the {@linkplain #activity activity} is 1 for a mob that tries to run and
 * {@link #STANDING_FACTOR} for one that stands, and {@link #FULL_WEIGHT} is the weight at which a running mob reaches the peak. So
 * one mob can never make more than the peak, and a running zombie, horse or any mob of weight {@value #FULL_WEIGHT} or more makes
 * all of it, a pig two thirds.
 * <p>
 * Economy: the peak follows the battery ladder (20 / 75 / 560 / 5600 / 56000 FE/t). A wheel only makes FE while a mob really runs,
 * which it does for a part of the day only: {@link #REFERENCE_DUTY} (a quarter) is the duty cycle the peaks are anchored by, and
 * at that duty every tier fills the battery of its tier in about 5350 ticks, a fifth of an MC day (see {@link #fillTicks}).
 * <p>
 * Perks per tier: tier 3 lets light mobs pull a full weight of {@link #REFERENCE_WEIGHT} and tier 5 has a flywheel that keeps
 * turning for a few seconds after the mob stopped ({@link #coast}); the other tiers differ by their peak and buffer.
 */
public final class WheelMath {
    /** The highest tier. */
    public static final int MAX_TIER = 5;
    /** Ticks of an MC day. */
    public static final int DAY_TICKS = 24_000;
    /** The share of a day a wheel is at full load that its peaks are anchored by. */
    public static final double REFERENCE_DUTY = 0.25;
    /** Ticks between two scans of the plate. */
    public static final int SCAN_TICKS = 20;
    /** The share of its weight a mob counts that stands on the plate and does not try to move. */
    public static final double STANDING_FACTOR = 0.5;
    /** The maximum health of the reference mob, a pig. */
    public static final double REFERENCE_HEALTH = 10;
    /** The base movement speed attribute of the reference mob, a pig. */
    public static final double REFERENCE_SPEED = 0.25;
    /** The weight of the reference mob. */
    public static final double REFERENCE_WEIGHT = 1;
    /** The weight at which a running mob makes the whole peak: a horse or a zombie and heavier, a pig makes two thirds. */
    public static final double FULL_WEIGHT = 1.5;
    /** The exponent of the health ratio in the weight. */
    public static final double HEALTH_EXPONENT = 0.5;
    /** The exponent of the speed ratio in the weight, smaller than the health one: health matters more than speed. */
    public static final double SPEED_EXPONENT = 0.25;
    /** The lightest weight a mob has. */
    public static final double MIN_WEIGHT = 0.25;
    /** The heaviest weight a mob has: no boss or giant makes more than a running horse already does. */
    public static final double MAX_WEIGHT = 2;
    /** The slowest speed that counts, so a mob with a zero speed attribute still has a weight. */
    public static final double MIN_SPEED = 0.05;
    /** From this tier on light mobs count as the reference mob. */
    public static final int SMALL_FULL_TIER = 3;

    /** The share of the last output the flywheel (tier 5) keeps from one scan to the next. */
    private static final double COAST = 0.5;

    /** Not instantiable. */
    private WheelMath() {}

    /**
     * The activity factor of the mob held by a wheel: its real movement is always pinned to zero, so what counts is whether it tries
     * to move (a path to follow, a move wanted by its goals, a walk animation); one that tries runs and counts fully, one that
     * merely stands counts {@link #STANDING_FACTOR} of its weight.
     *
     * @param trying whether the mob tries to move
     * @return 1 for a running mob, {@link #STANDING_FACTOR} for a standing one
     */
    public static double activity(boolean trying) {
        return trying ? 1 : STANDING_FACTOR;
    }

    /**
     * The weight of a mob, derived from its attributes alone, so every mob counts, vanilla or modded: {@code (maxHealth / 10)^0.5 *
     * (speed / 0.25)^0.25}, clamped to {@link #MIN_WEIGHT}..{@link #MAX_WEIGHT}. A pig (10 health, speed 0.25) weighs exactly 1, a horse
     * (about 22 health) about 1.5, a rabbit about 0.6, a zombie about 1.4; health weighs more than speed, so a slow giant is not
     * lighter than a fast small mob. From tier {@value #SMALL_FULL_TIER} on a mob lighter than {@link #REFERENCE_WEIGHT} pulls that weight.
     *
     * @param maxHealth the maximum health of the mob
     * @param speed     its base movement speed attribute (0.25 for a pig)
     * @param tier      the tier
     * @return the weight, {@link #MIN_WEIGHT}..{@link #MAX_WEIGHT}
     */
    public static double weight(double maxHealth, double speed, int tier) {
        double health = Math.pow(Math.max(maxHealth, 0) / REFERENCE_HEALTH, HEALTH_EXPONENT);
        double pace = Math.pow(Math.max(speed, MIN_SPEED) / REFERENCE_SPEED, SPEED_EXPONENT);
        double weight = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, health * pace));
        return tier >= SMALL_FULL_TIER ? Math.max(weight, REFERENCE_WEIGHT) : weight;
    }

    /**
     * The share of the peak the held mob makes: {@code min(1, weight * activity / FULL_WEIGHT)}.
     *
     * @param weight the {@linkplain #weight weight} of the mob
     * @param trying whether it tries to move, see {@link #activity}
     * @return 0..1
     */
    public static double share(double weight, boolean trying) {
        return Math.min(1, Math.max(0, weight) * activity(trying) / FULL_WEIGHT);
    }

    /**
     * What the flywheel of tier 5 adds: it keeps half of the last output, so the wheel runs down instead of stopping at once.
     * Lower tiers have none.
     *
     * @param target   the output the mob makes now
     * @param previous the output of the scan before
     * @param tier     the tier
     * @return the output to use; 0 once the flywheel would give less than 1 FE/t
     */
    public static double coast(double target, double previous, int tier) {
        if (tier < MAX_TIER) return target;
        double result = Math.max(target, previous * COAST);
        return result < 1 ? 0 : result;
    }

    /**
     * What a wheel makes in one MC day.
     *
     * @param peak the peak in FE/t
     * @param duty the share of the day it runs at full load
     * @return the FE per day
     */
    public static double dailyYield(double peak, double duty) {
        return peak * DAY_TICKS * duty;
    }

    /**
     * How long a wheel needs to fill a buffer.
     *
     * @param capacity the FE to fill
     * @param peak     the peak in FE/t
     * @param duty     the share of the time it runs at full load
     * @return the ticks
     */
    public static double fillTicks(double capacity, double peak, double duty) {
        return capacity / (peak * duty);
    }

    /**
     * The FE per tick a flywheel adds after a burst: the sum of the coasting outputs over the scans that follow one scan at the peak.
     *
     * @param peak the peak in FE/t
     * @param tier the tier
     * @return the extra FE of the coast ({@code 0} below tier 5)
     */
    public static double coastYield(double peak, int tier) {
        double extra = 0;
        for (double out = coast(0, peak, tier); out > 0; out = coast(0, out, tier)) extra += out * SCAN_TICKS;
        return extra;
    }
}
