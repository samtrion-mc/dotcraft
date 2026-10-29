package net.samtrion.dotcraft.energy.current;

/**
 * The pure math of the current generators, free of any Minecraft types so unit tests can carry the balance. The output is a
 * multiplier of the configured peak: {@code submerged * waterShare * biome * (1 + fauna)}. A tier is {@code 1..5}.
 *
 * <h2>The tier perks</h2>
 * Every tier adds one perk and keeps those of the tiers below:
 * <ol>
 *   <li>tier 1, baseline: it has no perk and is the most demanding, its water share is full only at {@link #FULL_WEIGHT_TIER_1}
 *       (a nearly full cube of water); animals count within {@link #FAUNA_RADIUS_NEAR} blocks;</li>
 *   <li>tier 2, wide net: the fauna search reaches {@link #FAUNA_RADIUS_FAR} blocks and the water share is full at {@link #FULL_WEIGHT};</li>
 *   <li>tier 3, adaptation: outside ocean and river biomes it keeps {@link #ADAPTED_BIOME} instead of {@link #OFF_BIOME};</li>
 *   <li>tier 4, lean water: the water share is already full at {@link #FULL_WEIGHT_LEAN}, so a small pond is enough;</li>
 *   <li>tier 5, abyssal: independent of the biome, it always makes the full biome share.</li>
 * </ol>
 * The output can exceed the peak by the fauna bonus: the peak is the best case <em>without</em> animals, five types make up to
 * {@code 1 + }{@link #FAUNA_MAX} times the peak. Running around the clock, the daily yield of an ocean pod is about three times that
 * of a solar panel of the same tier; this is the anchor the unit tests document.
 *
 * <h2>Economy</h2>
 * A current generator runs around the clock (its duty cycle is 1), so even the weakest realistic placement (half the water share, a
 * biome outside ocean and river) fills the battery of its tier in a fraction of one MC day; see the unit tests.
 */
public final class CurrentMath {
    /** The highest tier. */
    public static final int MAX_TIER = 5;
    /** The scan reaches this many blocks in every direction: a 5x5x5 cube. */
    public static final int SCAN_RADIUS = 2;
    /** The blocks of the cube without the generator itself. */
    public static final int CUBE_BLOCKS = 5 * 5 * 5 - 1;
    /** What a source block of water counts. */
    public static final int SOURCE_WEIGHT = 1;
    /** What a block of flowing water counts: a current is worth more than a still pool. */
    public static final int FLOWING_WEIGHT = 2;
    /**
     * The weight at which the water share is full from tier 2 on: three layers of source water (a generator that sits on the sea
     * floor has two solid layers below it) or half as many blocks of flowing water. More water does not help, so a pool full of
     * source blocks gives no more than a modest one.
     */
    public static final int FULL_WEIGHT = 75;
    /** The weight at which the water share of tier 1 is full: four layers, a nearly full cube. */
    public static final int FULL_WEIGHT_TIER_1 = 100;
    /** The weight at which the water share is full from tier 4 on (the lean water perk): two layers of source water. */
    public static final int FULL_WEIGHT_LEAN = 50;
    /** The biome multiplier outside of ocean and river biomes (tiers 1 and 2). */
    public static final float OFF_BIOME = 0.5F;
    /** The biome multiplier outside of ocean and river biomes from tier 3 on. */
    public static final float ADAPTED_BIOME = 0.75F;
    /** The fauna search radius of tier 1. */
    public static final int FAUNA_RADIUS_NEAR = 3;
    /** The fauna search radius from tier 2 on. */
    public static final int FAUNA_RADIUS_FAR = 6;
    /** What every distinct animal type adds. */
    public static final float FAUNA_STEP = 0.06F;
    /** How many distinct animal types count. */
    public static final int FAUNA_TYPES = 5;
    /** The most the animals can add: {@code 5 * 6 % = 30 %}. */
    public static final float FAUNA_MAX = FAUNA_STEP * FAUNA_TYPES;

    /** Not instantiable. */
    private CurrentMath() {}

    /**
     * The weight at which the water share of a tier is full.
     *
     * @param tier the tier
     * @return {@link #FULL_WEIGHT_TIER_1}, {@link #FULL_WEIGHT} or, from tier 4, {@link #FULL_WEIGHT_LEAN}
     */
    public static int fullWeight(int tier) {
        if (tier <= 1) return FULL_WEIGHT_TIER_1;
        return tier >= 4 ? FULL_WEIGHT_LEAN : FULL_WEIGHT;
    }

    /**
     * The fauna search radius.
     *
     * @param tier the tier
     * @return the radius in blocks
     */
    public static int faunaRadius(int tier) {
        return tier >= 2 ? FAUNA_RADIUS_FAR : FAUNA_RADIUS_NEAR;
    }

    /**
     * The share of the water around the generator.
     *
     * @param sources the blocks of source water in the cube
     * @param flowing the blocks of flowing water in the cube
     * @param tier    the tier
     * @return 0..1; capped, so more water than {@link #fullWeight} does not help
     */
    public static double waterShare(int sources, int flowing, int tier) {
        long weight = (long) sources * SOURCE_WEIGHT + (long) flowing * FLOWING_WEIGHT;
        return Math.min(1D, Math.max(0L, weight) / (double) fullWeight(tier));
    }

    /**
     * The multiplier of the biome.
     *
     * @param tier       the tier
     * @param primeBiome whether the biome is in the tag of the best biomes (ocean and river)
     * @return the multiplier
     */
    public static float biome(int tier, boolean primeBiome) {
        if (primeBiome || tier >= MAX_TIER) return 1F;
        return tier >= 3 ? ADAPTED_BIOME : OFF_BIOME;
    }

    /**
     * The bonus of the animals around.
     *
     * @param distinctTypes how many different types of animal are near; each type counts once, however many there are
     * @return the bonus, 0..{@link #FAUNA_MAX}
     */
    public static float fauna(int distinctTypes) {
        return Math.min(Math.max(distinctTypes, 0), FAUNA_TYPES) * FAUNA_STEP;
    }

    /**
     * The share of the peak a generator makes.
     *
     * @param tier       the tier
     * @param submerged  whether the generator itself stands in water
     * @param sources    the blocks of source water in the cube
     * @param flowing    the blocks of flowing water in the cube
     * @param primeBiome whether the biome is in the tag of the best biomes
     * @param faunaTypes how many different types of animal are near
     * @return the multiplier of the peak, at most {@code 1 + }{@link #FAUNA_MAX}
     */
    public static double output(int tier, boolean submerged, int sources, int flowing, boolean primeBiome, int faunaTypes) {
        if (!submerged) return 0;
        return waterShare(sources, flowing, tier) * biome(tier, primeBiome) * (1D + fauna(faunaTypes));
    }

    /**
     * Whether a block on the flow axis (in front of or behind the generator) stops the water: a block with a full collision box that is
     * neither water nor replaceable (air, grass, snow layers and the like let the water through). Such a block makes the output 0.
     *
     * @param fullCollision whether the block's collision shape is a full block
     * @param water         whether the block's fluid is water (a waterlogged block counts)
     * @param replaceable   whether the block is replaceable
     * @return {@code true} if the block blocks the flow
     */
    public static boolean blocksFlow(boolean fullCollision, boolean water, boolean replaceable) {
        return fullCollision && !water && !replaceable;
    }

    /**
     * The comparator signal: how much of the best possible output is made.
     *
     * @param output the FE per tick now
     * @param peak   the peak FE per tick
     * @return 0 for no output, otherwise 1..15
     */
    public static int signal(double output, int peak) {
        if (output <= 0 || peak <= 0) return 0;
        return (int) Math.clamp(Math.ceil(output / (peak * (1D + FAUNA_MAX)) * 15D), 1, 15);
    }
}
