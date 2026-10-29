package net.samtrion.dotcraft.energy.thermo;

/**
 * The pure math of the thermoelectric generators, free of any Minecraft types so unit tests can carry the balance.
 * <p>
 * A hot neighbour has a heat level and a cold neighbour a cold level, both {@code 1..}{@link #MAX_LEVEL} (data tags, see
 * {@link ThermoTags}). The temperature difference is the best heat level plus the best cold level of the six neighbours, and only
 * exists when there is both a hot and a cold neighbour: with {@link #MAX_LEVEL} 3 it is {@code 2..6}. A tier reaches its peak at
 * its {@linkplain #ceiling ceiling}: the higher the tier, the smaller the difference it needs for the same share of its peak (the
 * tier perk). The ceilings stay high on purpose, as the sources are free and never used up: the weakest pair (campfire and a
 * puddle) gives a tier 1 a third of its peak and a tier 5 only half of it, and full power needs real sources (tier 5: a
 * difference of 4, e.g. magma and ice; tier 1: lava and blue ice). More sources of the same kind do not add up, so the output of one block is bounded by its peak.
 */
public final class ThermoMath {
    /** The highest tier. */
    public static final int MAX_TIER = 5;
    /** The highest heat or cold level; a higher level of a tag counts as this one. */
    public static final int MAX_LEVEL = 3;
    /** The smallest temperature difference there is: the weakest hot plus the weakest cold source. */
    public static final int MIN_DELTA = 2;
    /** Ticks of one MC day. */
    public static final int TICKS_PER_DAY = 24_000;
    /** The highest comparator signal. */
    public static final int MAX_SIGNAL = 15;

    /** Per tier: the temperature difference that gives the full peak; it never rises with the tier (efficiency perk). */
    private static final int[] CEILING = {6, 6, 5, 5, 4};

    /** Not instantiable. */
    private ThermoMath() {}

    /**
     * The temperature difference of a generator.
     *
     * @param heat the best heat level of its neighbours, 0 if there is none
     * @param cold the best cold level of its neighbours, 0 if there is none
     * @return 0 without a hot or without a cold neighbour, else {@link #MIN_DELTA}..{@code 2 * MAX_LEVEL}
     */
    public static int delta(int heat, int cold) {
        if (heat <= 0 || cold <= 0) return 0;
        return Math.min(heat, MAX_LEVEL) + Math.min(cold, MAX_LEVEL);
    }

    /**
     * The difference at which a tier makes its full peak.
     *
     * @param tier the tier, 1..5
     * @return the ceiling, 6 for tier 1 down to 4 for tier 5
     */
    public static int ceiling(int tier) {
        return CEILING[tier - 1];
    }

    /**
     * The share of the peak a generator makes.
     *
     * @param tier the tier, 1..5
     * @param heat the best heat level of its neighbours, 0 if there is none
     * @param cold the best cold level of its neighbours, 0 if there is none
     * @return 0..1, linear in the difference up to the ceiling of the tier
     */
    public static float share(int tier, int heat, int cold) {
        return Math.min(1F, delta(heat, cold) / (float) ceiling(tier));
    }

    /**
     * The comparator signal of a generator.
     *
     * @param share the share of the peak, 0..1
     * @return 0 when nothing is made, else 1..{@link #MAX_SIGNAL} (15 at full output)
     */
    public static int signal(float share) {
        if (share <= 0) return 0;
        return Math.clamp(Math.round(share * MAX_SIGNAL), 1, MAX_SIGNAL);
    }

    /**
     * The FE a generator makes in one MC day at a constant share; unlike a panel it has no day and night.
     *
     * @param peak  the peak in FE/t
     * @param share the share of the peak, 0..1
     * @return the FE per MC day
     */
    public static double dailyYield(double peak, float share) {
        return peak * share * TICKS_PER_DAY;
    }

    /**
     * How long a generator needs to fill a buffer, e.g. the battery of its tier.
     *
     * @param capacity the FE to fill
     * @param peak     the peak in FE/t
     * @param share    the share of the peak, 0..1
     * @return the ticks, infinite if nothing is made
     */
    public static double ticksToFill(double capacity, double peak, float share) {
        double rate = peak * share;
        return rate <= 0 ? Double.POSITIVE_INFINITY : capacity / rate;
    }
}
