package net.samtrion.dotcraft.energy.melt;

/**
 * The rules of the melt generators that need no world, so a unit test can check them.
 * <p>
 * A melted block does not fill the buffer at once, its FE goes into a heat reservoir that is released into the buffer at most
 * {@code peak} FE per tick, the rate limiter. A generator therefore never makes more than its peak, however much ice is next to it,
 * and a block that gives more than the reservoir holds (blue ice in a small tier) is still burnt when the reservoir is empty and then
 * lasts for a long time. No block melts while the reservoir is full, so nothing is melted for nothing (only the reservoir of a
 * generator that is mined is lost).
 * <p>
 * Economy: the peaks follow the battery ladder (15 / 75 / 560 / 5 600 / 56 000 FE/t) so a generator of a tier fills the battery of the
 * same tier in about 1 340 ticks (roughly 5.6% of an MC day) if it is fed. The ice is the price: by default 1 000 FE per ice block,
 * so tier 1 at full output burns 360 ice (40 packed ice) per MC day. Ice can be farmed (water freezes in cold biomes, slowly), but
 * never for free: a melted block is gone (the higher tiers leave water, which has to freeze again), and frosted ice, which frost
 * walker boots make without limit, is no fuel. The heat sources are not used up, so the ice is the only running cost.
 * <p>
 * Tiers 4 and 5 are burst tiers: running them at their peak for a whole day would take thousands of blue ice, so in practice they
 * empty the reservoir (600 ticks of peak) into a battery now and then. Water recovery leaves a renewable water source behind; it can
 * freeze again in a cold biome, but only slowly (random ticks of surface water), so it is a slow loop, not a free one.
 */
public final class MeltMath {
    /** Ticks of full output the reservoir holds, the same as the buffer. */
    public static final int RESERVE_TICKS = 600;
    /** The number of tiers. */
    public static final int TIER_COUNT = 5;
    /** Ticks of one MC day. */
    public static final int DAY_TICKS = 24_000;

    /**
     * What a tier can do besides a higher peak.
     *
     * @param slots    how many ice blocks it melts at most per pulse; more than one means more of the six sides can feed it at once
     * @param interval ticks between two pulses
     * @param water    whether a melted block leaves a water source (not where water evaporates, as in the Nether)
     */
    public record Tier(int slots, int interval, boolean water) {}

    private static final Tier[] TIERS = {
            new Tier(1, 20, false), // 1: one block per second
            new Tier(2, 20, false), // 2: two blocks at once
            new Tier(3, 10, false), // 3: three blocks, twice a second
            new Tier(4, 10, true), // 4: water recovery
            new Tier(5, 5, true) // 5: five blocks, four times a second: all five ice sides at full speed
    };

    /** Not instantiable. */
    private MeltMath() {}

    /**
     * The perks of a tier.
     *
     * @param tier 1..5
     * @return the perks
     */
    public static Tier tier(int tier) {
        return TIERS[tier - 1];
    }

    /**
     * How much FE the reservoir holds at most.
     *
     * @param peak the peak in FE/t
     * @return the FE
     */
    public static long reserve(int peak) {
        return (long) peak * RESERVE_TICKS;
    }

    /**
     * Whether another block may melt. It may if its FE fits into the reservoir, or if the reservoir is empty (a block bigger than
     * the reservoir is then still burnt).
     *
     * @param pending the FE in the reservoir
     * @param yield   the FE the block gives
     * @param peak    the peak in FE/t
     * @return {@code true} if it may melt
     */
    public static boolean mayMelt(long pending, int yield, int peak) {
        return yield > 0 && (pending <= 0 || pending + yield <= reserve(peak));
    }

    /**
     * The rate limiter: the FE released from the reservoir into the buffer this tick.
     *
     * @param pending the FE in the reservoir
     * @param peak    the peak in FE/t
     * @param room    the free space of the buffer
     * @return the FE, never more than the peak
     */
    public static int release(long pending, int peak, int room) {
        return (int) Math.max(0, Math.min(Math.min(pending, peak), room));
    }

    /**
     * The most FE per tick the ice of the sides can supply at the pulse rate of the tier, to see which ice a tier needs for its peak.
     *
     * @param tier  1..5
     * @param yield the FE of one block
     * @return the FE per tick
     */
    public static double supply(int tier, int yield) {
        var t = tier(tier);
        return (double) t.slots() * yield / t.interval();
    }

    /**
     * Whether a melted block leaves water.
     *
     * @param tier       the perks of the tier
     * @param evaporates whether water evaporates at the position (the Nether)
     * @return {@code true} if the block is replaced by a water source, {@code false} for air
     */
    public static boolean leavesWater(Tier tier, boolean evaporates) {
        return tier.water() && !evaporates;
    }

    /**
     * The comparator signal: how full the heat reservoir and the buffer are together.
     *
     * @param held the FE in the reservoir and the buffer
     * @param max  the FE both hold at most
     * @return 0 when empty, otherwise 1..15
     */
    public static int signal(long held, long max) {
        if (held <= 0 || max <= 0) return 0;
        return (int) Math.min(15, 1 + 14 * held / max);
    }

    /**
     * The FE per MC day a generator makes at most while it is fed.
     *
     * @param peak the peak in FE/t
     * @return the FE
     */
    public static long dailyYield(int peak) {
        return (long) peak * DAY_TICKS;
    }

    /**
     * The ticks a generator needs to fill a battery from empty.
     *
     * @param capacity the FE of the battery
     * @param peak     the peak in FE/t
     * @return the ticks, rounded up
     */
    public static long fillTicks(long capacity, int peak) {
        return (capacity + peak - 1) / peak;
    }
}
