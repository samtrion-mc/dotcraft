package net.samtrion.dotcraft.resources.common;

/**
 * Tier maths shared by registration and datagen: how many base blocks a tier holds and how hard it is to break.
 */
public final class CompressedTier {
    /** Highest compression tier of every base block. */
    public static final int MAX = 9;

    /** Number of blocks of the previous tier that make up one block of the next. */
    public static final int FACTOR = 9;

    private CompressedTier() {}

    /**
     * Number of base blocks held by one block of the given tier.
     *
     * @param tier the tier, {@code 1..MAX}
     * @return {@code FACTOR^tier}
     */
    public static long count(int tier) {
        check(tier);

        return (long) Math.pow(FACTOR, tier);
    }

    /**
     * Mining hardness of a compressed block: the base hardness grows linearly with the tier.
     *
     * @param base the hardness of the base block
     * @param tier the tier, {@code 1..MAX}
     * @return the hardness of the compressed block
     */
    public static float hardness(float base, int tier) {
        check(tier);
        return base * (tier + 1);
    }

    /**
     * Furnace burn time of a compressed block: the base burn time times {@link #count(int)}, or {@code 0} (no
     * fuel) if that does not fit into an {@code int}, as the burn time of a fuel is one.
     *
     * @param base the burn time of the base block in ticks, {@code 0} if it is no fuel
     * @param tier the tier, {@code 1..MAX}
     * @return the burn time of the compressed block in ticks
     */
    public static int burnTime(int base, int tier) {
        check(tier);
        long burn = (long) base * count(tier);
        return burn > Integer.MAX_VALUE ? 0 : (int) burn;
    }

    private static void check(int tier) {
        if (tier < 1 || tier > MAX) throw new IllegalArgumentException("tier " + tier + " not in 1.." + MAX);
    }
}
