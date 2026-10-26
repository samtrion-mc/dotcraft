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
        long n = 1;
        for (int i = 0; i < tier; i++) n *= FACTOR;
        return n;
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

    private static void check(int tier) {
        if (tier < 1 || tier > MAX) throw new IllegalArgumentException("tier " + tier + " not in 1.." + MAX);
    }
}
