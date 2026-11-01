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
 * Economy: the peaks follow the battery ladder (20 / 75 / 560 / 5 600 / 56 000 FE/t) so a generator of tier 2 to 5 fills the battery of
 * the same tier in about 1 340 ticks (roughly 5.6% of an MC day) if it is fed, tier 1 needs 2 000 ticks (40 000 FE at 20 FE/t, roughly
 * 8.3% of a day). The ice is the price: by default 1 000 FE per ice block, so tier 1 at full output burns 480 ice (53 packed ice) per
 * MC day. Ice can be farmed (water freezes in cold biomes, slowly), but
 * never for free: a melted block is gone (the higher tiers give water, which has to freeze again), and frosted ice, which frost
 * walker boots make without limit, is no fuel. The heat sources are not used up, so the ice is the only running cost.
 * <p>
 * Tiers 4 and 5 are burst tiers: running them at their peak for a whole day would take thousands of blue ice, so in practice they
 * empty the reservoir (600 ticks of peak) into a battery now and then. Water recovery fills the tank of the generator, which has to
 * be drained to keep the full efficiency. Tier 4 recovers one bucket per melted block, tier 5 recovers all the water (see {@link #waterBlocks}). The water goes into the tank of the generator
 * ({@link #TANK_CAPACITY_MB}), what does not fit is lost, and while the tank is full the ice only gives {@link #FULL_TANK_EFFICIENCY} of its FE
 * until the water is drained.
 */
public final class MeltMath {
    /** Ticks of full output the reservoir holds, the same as the buffer. */
    public static final int RESERVE_TICKS = 600;
    /** The number of tiers. */
    public static final int TIER_COUNT = 5;
    /** The water the tank of a generator holds, in mB (32 buckets). */
    public static final int TANK_CAPACITY_MB = 32_000;
    /** The water of one water source block, in mB. */
    public static final int BUCKET_MB = 1_000;
    /** The share of the FE per ice that a generator with a full tank still makes: the efficiency drops by 75 percent. */
    public static final double FULL_TANK_EFFICIENCY = 0.25;
    /** Ticks of one MC day. */
    public static final int DAY_TICKS = 24_000;

    /**
     * What a tier can do besides a higher peak.
     *
     * @param slots    how many ice blocks it melts at most per pulse; more than one means more of the six sides can feed it at once
     * @param interval ticks between two pulses
     * @param water    whether a melted block gives a bucket of water for the tank (not where water evaporates, as in the Nether)
     * @param complete whether the water recovery is complete: the melted block gives as many buckets as it is worth plain ice
     *                 (1 for ice, 9 for packed ice, 81 for blue ice with the default yields), not just one; implies {@code water}
     */
    public record Tier(int slots, int interval, boolean water, boolean complete) {}

    private static final Tier[] TIERS = {
            new Tier(1, 20, false, false), // 1: one block per second
            new Tier(2, 20, false, false), // 2: two blocks at once
            new Tier(3, 10, false, false), // 3: three blocks, twice a second
            new Tier(4, 10, true, false), // 4: water recovery (one bucket per block)
            new Tier(5, 5, true, true) // 5: five blocks, four times a second, and the water comes back in full
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
     * Whether a melted block gives water.
     *
     * @param tier       the perks of the tier
     * @param evaporates whether water evaporates at the position (the Nether)
     * @return {@code true} if the block gives water for the tank, {@code false} if it gives none
     */
    public static boolean leavesWater(Tier tier, boolean evaporates) {
        return tier.water() && !evaporates;
    }

    /**
     * How many buckets (water source blocks) of water a melted block gives. Tier 4 gives one bucket for every ice type; the complete recovery of tier 5
     * gives as many as the block is worth plain ice, so packed and blue ice give back the water of the ice they are made of.
     *
     * @param tier       the perks of the tier
     * @param evaporates whether water evaporates at the position (the Nether)
     * @param yield      the FE of the melted block
     * @param iceYield   the FE of one plain ice block
     * @return 0 for no water, else the number of buckets, at least 1
     */
    public static int waterBlocks(Tier tier, boolean evaporates, int yield, int iceYield) {
        if (!leavesWater(tier, evaporates)) return 0;
        if (!tier.complete() || iceYield <= 0) return 1;
        return Math.max(1, yield / iceYield);
    }

    /**
     * The FE a melted block gives: the full yield, a quarter of it while the tank is full.
     *
     * @param yield    the FE of the block
     * @param tankFull whether the water tank is full
     * @return the FE that goes into the reservoir
     */
    public static int effectiveYield(int yield, boolean tankFull) {
        return tankFull ? (int) Math.round(yield * FULL_TANK_EFFICIENCY) : yield;
    }

    /**
     * How much of some water fits into the tank; the rest is lost.
     *
     * @param stored the water in the tank in mB
     * @param add    the water to add in mB
     * @return the mB that fit, 0..add
     */
    public static int tankFit(int stored, int add) {
        return Math.max(0, Math.min(add, TANK_CAPACITY_MB - stored));
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
