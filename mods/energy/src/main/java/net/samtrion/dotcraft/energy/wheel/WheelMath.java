package net.samtrion.dotcraft.energy.wheel;

import java.util.Arrays;

/**
 * The pure math of the mob wheels, free of any Minecraft types so unit tests can carry the balance. A tier is {@code 1..5}.
 * <p>
 * The output is {@code peak * load}. The load is the effective weight of the running mobs divided by the {@linkplain #fullLoad
 * full load} of the tier, at most 1: the strongest mobs count first, every further mob counts {@code decay} times less than
 * the one before (diminishing returns), and mobs beyond the {@linkplain #cap cap} of the tier count nothing. So no mob farm
 * can grow the output of one wheel past its peak.
 * <p>
 * Economy: the peak follows the battery ladder (15 / 75 / 560 / 5600 / 56000 FE/t). A wheel only makes FE while mobs really run,
 * which they do for a part of the day only: {@link #REFERENCE_DUTY} (a quarter) is the duty cycle the peaks are anchored by, and
 * at that duty every tier fills the battery of its tier in about 5350 ticks, a fifth of an MC day (see {@link #fillTicks}).
 * <p>
 * Perks per tier: tier 1 is the base wheel, tier 2 has a bigger cage (more mobs), tier 3 lets small animals pull their
 * full weight, tier 4 has a gearbox that softens the diminishing returns and tier 5 has a flywheel that keeps turning for a few
 * seconds after the mobs stopped ({@link #coast}).
 * <p>
 * The duty is an assumption, not a measurement: mobs stroll and idle, so a real cage may well stay below it. The only ways to
 * beat it are pushing mobs around (a piston, a lure; a mob in water does not count), and even then the output is bounded by the
 * peak. The flywheel adds the coast on top: after one scan at peak it makes about {@link #coastYield one more scan} of FE, which
 * matters for burst-driven farms only.
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
    /** The fewest blocks a mob must have moved horizontally between two scans to count as running. */
    public static final double MIN_MOVE = 0.5;
    /** The share of its weight a mob counts that stands on the plate without moving. */
    public static final double STANDING_FACTOR = 0.5;
    /** A mob below this share of its health does not run. */
    public static final double MIN_HEALTH = 0.5;

    /** Per tier: the most mobs that count. */
    private static final int[] CAP = {2, 3, 4, 6, 8};
    /** Per tier: the factor each further mob counts less than the one before. */
    private static final double[] DECAY = {0.75, 0.75, 0.75, 0.85, 0.85};
    /** The share of the last output the flywheel (tier 5) keeps from one scan to the next. */
    private static final double COAST = 0.5;

    /** How strong a mob is, which is its weight; the entity type tags decide. */
    public enum Strength {
        /** Not a runner: counts nothing. */
        NONE(0),
        /** Small animals: half a weight, a full one from tier 3 on. */
        WEAK(0.5),
        /** Most animals. */
        NORMAL(1),
        /** Horses and the like: twice as much. */
        STRONG(2);

        private final double weight;

        Strength(double weight) {
            this.weight = weight;
        }
    }

    /** Not instantiable. */
    private WheelMath() {}

    /**
     * The most mobs of a tier that count.
     *
     * @param tier the tier
     * @return the cap
     */
    public static int cap(int tier) {
        return CAP[tier - 1];
    }

    /**
     * The activity factor of a mob on the plate: one that moved between two scans runs and counts fully, one that merely stands
     * (or was seen for the first time) counts {@link #STANDING_FACTOR} of its weight. Nothing walks a mob in place and the plate
     * is small, so a mob that stands still must not make nothing.
     *
     * @param moved whether the mob {@linkplain #moved moved} since the scan before
     * @return 1 for a running mob, {@link #STANDING_FACTOR} for a standing one
     */
    public static double activity(boolean moved) {
        return moved ? 1 : STANDING_FACTOR;
    }

    /**
     * The diminishing returns factor of a tier.
     *
     * @param tier the tier
     * @return the factor each further mob counts of the one before, 0..1
     */
    public static double decay(int tier) {
        return DECAY[tier - 1];
    }

    /**
     * The weight of a mob of a strength at a tier.
     *
     * @param strength how strong the mob is
     * @param tier     the tier
     * @return the weight; small animals count fully from tier 3 on
     */
    public static double weight(Strength strength, int tier) {
        return strength == Strength.WEAK && tier >= 3 ? Strength.NORMAL.weight : strength.weight;
    }

    /**
     * How well a mob can run for its health: an animal below half of its health rests, above it the share counts.
     *
     * @param health    its health
     * @param maxHealth its maximum health
     * @return 0..1
     */
    public static double welfare(double health, double maxHealth) {
        if (maxHealth <= 0) return 0;
        double share = Math.min(1, health / maxHealth);
        return share < MIN_HEALTH ? 0 : share;
    }

    /**
     * Whether a mob moved enough between two scans to count as running.
     *
     * @param dx the horizontal change of its position in x
     * @param dz the horizontal change of its position in z
     * @return {@code true} if it ran
     */
    public static boolean moved(double dx, double dz) {
        return dx * dx + dz * dz >= MIN_MOVE * MIN_MOVE;
    }

    /**
     * The effective weight of the running mobs: the strongest {@link #cap} first, each further one weighs {@code decay} times
     * less than the one before.
     *
     * @param weights the weight of every running mob, in any order
     * @param tier    the tier
     * @return the effective weight, at least 0
     */
    public static double effective(double[] weights, int tier) {
        double[] sorted = weights.clone();
        Arrays.sort(sorted);
        double factor = 1;
        double sum = 0;
        for (int i = 0; i < Math.min(sorted.length, cap(tier)); i++) {
            sum += Math.max(0, sorted[sorted.length - 1 - i]) * factor;
            factor *= decay(tier);
        }
        return sum;
    }

    /**
     * The effective weight at which a tier reaches its peak: a full cap of normal mobs.
     *
     * @param tier the tier
     * @return the full load
     */
    public static double fullLoad(int tier) {
        double[] normal = new double[cap(tier)];
        Arrays.fill(normal, Strength.NORMAL.weight);
        return effective(normal, tier);
    }

    /**
     * The share of the peak the running mobs make.
     *
     * @param weights the weight of every running mob
     * @param tier    the tier
     * @return 0..1
     */
    public static double load(double[] weights, int tier) {
        return Math.min(1, effective(weights, tier) / fullLoad(tier));
    }

    /**
     * What the flywheel of tier 5 adds: it keeps half of the last output, so the wheel runs down instead of stopping at once.
     * Lower tiers have none.
     *
     * @param target   the output the running mobs make now
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
