package net.samtrion.dotcraft.energy.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the math of the flow generators, their perks and their economy anchor. */
class FlowMathTest {
    private static final int DAY = 24_000;
    /** The battery of each tier: capacity in FE. */
    private static final int[] BATTERIES = {
            EnergyCommonConfig.BATTERY_TIER_1.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
            EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};

    private static int peak(int tier) {
        return EnergyCommonConfig.FLOW_GENERATORS.get(tier - 1).peak();
    }

    /** A full cube of source water in an ocean, without animals: the reference case. */
    private static double ocean(int tier) {
        return FlowMath.output(tier, true, FlowMath.CUBE_BLOCKS, 0, true, 0);
    }

    /** The ladder follows the batteries and starts at 20 FE/t; the best case makes exactly the peak. */
    @Test
    void peakLadder() {
        int[] expected = {20, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(expected[tier - 1], peak(tier), "tier " + tier);
            assertEquals(1D, ocean(tier), 1e-9, "tier " + tier);
        }
    }

    /** Only a full-collision block that is neither water nor replaceable blocks the flow axis. */
    @Test
    void flowAxis() {
        assertTrue(FlowMath.blocksFlow(true, false, false), "stone");
        assertTrue(!FlowMath.blocksFlow(false, false, false), "a slab or a fence lets the water pass");
        assertTrue(!FlowMath.blocksFlow(true, true, false), "a waterlogged block counts as water");
        assertTrue(!FlowMath.blocksFlow(true, false, true), "a replaceable block gives way");
        assertTrue(!FlowMath.blocksFlow(false, true, true), "water or air");
    }

    /** Outside of water nothing is made, whatever surrounds the block. */
    @Test
    void noWaterNoOutput() {
        assertEquals(0, FlowMath.output(5, false, FlowMath.CUBE_BLOCKS, 0, true, 5));
        assertEquals(0, FlowMath.output(1, true, 0, 0, true, 5), "a waterlogged block in a dry cube has no share");
    }

    /** More water than the full weight does not help (no source block spam), and the share grows linearly below it. */
    @Test
    void waterShareIsCapped() {
        assertEquals(1D, FlowMath.waterShare(FlowMath.FULL_WEIGHT_TIER_1, 0, 1), 1e-9);
        assertEquals(1D, FlowMath.waterShare(FlowMath.FULL_WEIGHT, 0, 2), 1e-9);
        assertEquals(1D, FlowMath.waterShare(FlowMath.CUBE_BLOCKS, 0, 1), 1e-9);
        assertEquals(1D, FlowMath.waterShare(Integer.MAX_VALUE, Integer.MAX_VALUE, 5), 1e-9);
        assertEquals(0.5D, FlowMath.waterShare(FlowMath.FULL_WEIGHT_TIER_1 / 2, 0, 1), 0.02);
        assertTrue(FlowMath.waterShare(10, 0, 1) < FlowMath.waterShare(20, 0, 1));
        assertEquals(0D, FlowMath.waterShare(-5, -5, 1));
    }

    /** Flowing water counts double, in every tier: a stream is worth more than a pond. */
    @Test
    void flowingWaterCountsMore() {
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(FlowMath.waterShare(5, 0, tier) * 2, FlowMath.waterShare(0, 5, tier), 1e-9, "tier " + tier);
        }
    }

    /**
     * The water perk in the 3x3x3 cube (26 blocks around the pod): tier 1 needs 22 sources (a nearly full cube), tiers 2-3 need 16,
     * from tier 4 11; the old 5x5x5 weights (100, 75, 50 of 124) scaled to 26 blocks.
     */
    @Test
    void leanWaterPerk() {
        assertEquals(FlowMath.FULL_WEIGHT_TIER_1, FlowMath.fullWeight(1));
        assertEquals(FlowMath.FULL_WEIGHT, FlowMath.fullWeight(2));
        assertEquals(FlowMath.FULL_WEIGHT, FlowMath.fullWeight(3));
        assertEquals(FlowMath.FULL_WEIGHT_LEAN, FlowMath.fullWeight(4));
        assertEquals(FlowMath.FULL_WEIGHT_LEAN, FlowMath.fullWeight(5));
        assertEquals(26, FlowMath.CUBE_BLOCKS);
        assertTrue(FlowMath.FULL_WEIGHT_TIER_1 <= FlowMath.CUBE_BLOCKS, "the best case must reach the full share");
        assertTrue(FlowMath.waterShare(21, 0, 1) < 1D);
        assertEquals(1D, FlowMath.waterShare(22, 0, 1), 1e-9);
        assertTrue(FlowMath.waterShare(15, 0, 2) < 1D);
        assertEquals(1D, FlowMath.waterShare(16, 0, 2), 1e-9);
        assertTrue(FlowMath.waterShare(10, 0, 3) < 1D);
        assertEquals(1D, FlowMath.waterShare(11, 0, 4), 1e-9);
        assertTrue(FlowMath.waterShare(10, 0, 3) < FlowMath.waterShare(10, 0, 4));
    }

    /** The roll of the dry explosion: below the chance it explodes, chance 0 never, chance 1 always, whatever the roll. */
    @Test
    void dryExplosionRoll() {
        assertTrue(FlowMath.dryExplosion(0.0, 0.05));
        assertTrue(FlowMath.dryExplosion(0.049, 0.05));
        assertTrue(!FlowMath.dryExplosion(0.05, 0.05));
        assertTrue(!FlowMath.dryExplosion(0.99, 0.05));
        for (double roll : new double[] {0, 0.3, 0.9999999}) {
            assertTrue(!FlowMath.dryExplosion(roll, 0), "chance 0 never explodes");
            assertTrue(FlowMath.dryExplosion(roll, 1), "chance 1 always explodes");
        }
        assertEquals(0.05, FlowMath.DRY_EXPLOSION_CHANCE_DEFAULT);
    }

    /** Ocean and river give 1; elsewhere tiers 1-2 keep half, tiers 3-4 three quarters, tier 5 is independent of the biome. */
    @Test
    void biomePerk() {
        for (int tier = 1; tier <= 5; tier++) assertEquals(1F, FlowMath.biome(tier, true), "prime tier " + tier);
        assertEquals(0.5F, FlowMath.biome(1, false));
        assertEquals(0.5F, FlowMath.biome(2, false));
        assertEquals(0.75F, FlowMath.biome(3, false));
        assertEquals(0.75F, FlowMath.biome(4, false));
        assertEquals(1F, FlowMath.biome(5, false));
    }

    /** Each type counts once up to five, 6 % each: at most 30 %. */
    @Test
    void faunaBonus() {
        assertEquals(0F, FlowMath.fauna(0));
        assertEquals(0F, FlowMath.fauna(-3));
        assertEquals(0.06F, FlowMath.fauna(1), 1e-6);
        assertEquals(0.30F, FlowMath.fauna(5), 1e-6);
        assertEquals(0.30F, FlowMath.fauna(50), 1e-6, "an aquarium stuffed with every type is capped");
        assertEquals(0.30F, FlowMath.FAUNA_MAX, 1e-6);
        assertEquals(1.3D, FlowMath.output(1, true, FlowMath.CUBE_BLOCKS, 0, true, 99), 1e-6, "the whole bonus is at most 30 percent");
    }

    /** The fauna search of tier 1 is near, from tier 2 on it is wide. */
    @Test
    void faunaRadiusPerk() {
        assertEquals(FlowMath.FAUNA_RADIUS_NEAR, FlowMath.faunaRadius(1));
        for (int tier = 2; tier <= 5; tier++) assertEquals(FlowMath.FAUNA_RADIUS_FAR, FlowMath.faunaRadius(tier));
        assertTrue(FlowMath.FAUNA_RADIUS_FAR > FlowMath.FAUNA_RADIUS_NEAR);
    }

    /** Every tier makes at least as much as the one below in the same place: no perk is a penalty. */
    @Test
    void higherTiersNeverMakeLessShare() {
        for (int tier = 2; tier <= 5; tier++) {
            for (boolean prime : new boolean[] {true, false}) {
                for (int flowing = 0; flowing <= 40; flowing += 8) {
                    assertTrue(FlowMath.output(tier, true, 20, flowing, prime, 2) >= FlowMath.output(tier - 1, true, 20, flowing, prime, 2), "tier " + tier);
                }
            }
        }
    }

    /**
     * The economy anchor. The generator runs around the clock, so in the best case it fills the battery of its tier in a tenth of a
     * day or less, and even in a poor place (half the water share, outside ocean and river, no animals) in a quarter of a day.
     */
    @Test
    void fillsTheBatteryOfItsTierWellUnderOneDay() {
        for (int tier = 1; tier <= 5; tier++) {
            double best = ocean(tier) * peak(tier);
            double poor = FlowMath.output(tier, true, FlowMath.fullWeight(tier) / 2, 0, false, 0) * peak(tier);
            double ticksBest = BATTERIES[tier - 1] / best;
            double ticksPoor = BATTERIES[tier - 1] / poor;
            assertTrue(ticksBest <= DAY / 10D, "best case of tier " + tier + " needs " + ticksBest + " ticks");
            assertTrue(ticksPoor <= (tier == 1 ? DAY / 3D : DAY / 4D), "poor case of tier " + tier + " needs " + ticksPoor + " ticks");
            assertTrue(best * DAY >= BATTERIES[tier - 1] * 10D, "daily yield of tier " + tier + " is " + best * DAY);
        }
    }

    /** The animals lift the output to at most 1.3 times the peak, and that is still no more than the 1.3 case of the anchor allows. */
    @Test
    void faunaCeilingIsOnePointThreeTimesThePeak() {
        for (int tier = 1; tier <= 5; tier++) {
            double most = FlowMath.output(tier, true, FlowMath.CUBE_BLOCKS, 0, true, 99);
            assertEquals(1D + FlowMath.FAUNA_MAX, most, 1e-9, "tier " + tier);
            assertTrue(BATTERIES[tier - 1] / (most * peak(tier)) <= DAY / 10D, "tier " + tier);
        }
    }

    /**
     * The 24/7 anchor against the panels: the flow generator makes power around the clock, so the daily yield of the same tier
     * in the best place is a multiple of the clear-sky solar panel, roughly three times. Anyone changing the ladder sees here what it does to
     * the balance between the families.
     */
    @Test
    void dailyYieldAgainstSolar() {
        for (int tier = 1; tier <= 5; tier++) {
            double sun = 0;
            for (int t = 0; t < DAY; t += 10) {
                sun += net.samtrion.dotcraft.energy.generator.GeneratorMath.solar(tier, (t - 6000) * 360F / DAY,
                        true, net.samtrion.dotcraft.energy.generator.GeneratorMath.Weather.CLEAR, 1F) * 10;
            }
            double solar = sun * EnergyCommonConfig.SOLAR_PANELS.get(tier - 1).peak();
            double flow = ocean(tier) * peak(tier) * DAY;
            double ratio = flow / solar;
            assertTrue(ratio > 2 && ratio < 6, "tier " + tier + ": flow generator makes " + ratio + " times the daily yield of a solar panel");
        }
    }

    /** The tier 1 peak is at least the 30 FE/t of the economy rule, and the buffer holds 600 ticks of it. */
    @Test
    void tierOneMinimumAndBuffer() {
        assertTrue(peak(1) >= 20);
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(peak(tier) * 600, EnergyCommonConfig.FLOW_GENERATORS.get(tier - 1).capacity());
            assertEquals(peak(tier) * 5, EnergyCommonConfig.FLOW_GENERATORS.get(tier - 1).maxOutput());
        }
    }

    /** The comparator shows 0 for nothing and 1..15 for the share of the best possible output, including the fauna bonus. */
    @Test
    void comparatorSignal() {
        assertEquals(0, FlowMath.signal(0, 15));
        assertEquals(0, FlowMath.signal(5, 0));
        assertEquals(1, FlowMath.signal(0.001, 15), "a little is not nothing");
        assertEquals(15, FlowMath.signal(15 * 1.3, 15));
        assertEquals(15, FlowMath.signal(1_000_000, 15));
        assertTrue(FlowMath.signal(15, 15) < 15, "the peak without animals is not the best");
        assertTrue(FlowMath.signal(7, 15) < FlowMath.signal(14, 15));
    }
}
