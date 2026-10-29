package net.samtrion.dotcraft.energy.current;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the math of the current generators, their perks and their economy anchor. */
class CurrentMathTest {
    private static final int DAY = 24_000;
    /** The battery of each tier: capacity in FE. */
    private static final int[] BATTERIES = {
            EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
            EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};

    private static int peak(int tier) {
        return EnergyCommonConfig.CURRENT_GENERATORS.get(tier - 1).peak();
    }

    /** A full cube of source water in an ocean, without animals: the reference case. */
    private static double ocean(int tier) {
        return CurrentMath.output(tier, true, CurrentMath.CUBE_BLOCKS, 0, true, 0);
    }

    /** The ladder follows the batteries and starts at 15 FE/t; the best case makes exactly the peak. */
    @Test
    void peakLadder() {
        int[] expected = {15, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(expected[tier - 1], peak(tier), "tier " + tier);
            assertEquals(1D, ocean(tier), 1e-9, "tier " + tier);
        }
    }

    /** Outside of water nothing is made, whatever surrounds the block. */
    @Test
    void noWaterNoOutput() {
        assertEquals(0, CurrentMath.output(5, false, CurrentMath.CUBE_BLOCKS, 0, true, 5));
        assertEquals(0, CurrentMath.output(1, true, 0, 0, true, 5), "a waterlogged block in a dry cube has no share");
    }

    /** More water than the full weight does not help (no source block spam), and the share grows linearly below it. */
    @Test
    void waterShareIsCapped() {
        assertEquals(1D, CurrentMath.waterShare(CurrentMath.FULL_WEIGHT, 0, 1), 1e-9);
        assertEquals(1D, CurrentMath.waterShare(CurrentMath.CUBE_BLOCKS, 0, 1), 1e-9);
        assertEquals(1D, CurrentMath.waterShare(Integer.MAX_VALUE, Integer.MAX_VALUE, 5), 1e-9);
        assertEquals(0.5D, CurrentMath.waterShare(CurrentMath.FULL_WEIGHT / 2, 0, 1), 0.02);
        assertTrue(CurrentMath.waterShare(10, 0, 1) < CurrentMath.waterShare(20, 0, 1));
        assertEquals(0D, CurrentMath.waterShare(-5, -5, 1));
    }

    /** Flowing water counts double, from tier 4 on triple: a stream is worth more than a pond. */
    @Test
    void flowingWaterCountsMore() {
        assertEquals(CurrentMath.waterShare(20, 0, 1) * 2, CurrentMath.waterShare(0, 20, 1), 1e-9);
        assertEquals(CurrentMath.waterShare(0, 20, 1) * 1.5, CurrentMath.waterShare(0, 20, 4), 1e-9, "tier 4 streamlining");
        assertEquals(CurrentMath.waterShare(0, 20, 4), CurrentMath.waterShare(0, 20, 5), 1e-9, "tier 5 keeps it");
        assertEquals(CurrentMath.waterShare(0, 20, 1), CurrentMath.waterShare(0, 20, 3), 1e-9, "tier 3 does not have it yet");
    }

    /** Ocean and river give 1; elsewhere tiers 1-2 keep half, tiers 3-4 three quarters, tier 5 is independent of the biome. */
    @Test
    void biomePerk() {
        for (int tier = 1; tier <= 5; tier++) assertEquals(1F, CurrentMath.biome(tier, true), "prime tier " + tier);
        assertEquals(0.5F, CurrentMath.biome(1, false));
        assertEquals(0.5F, CurrentMath.biome(2, false));
        assertEquals(0.75F, CurrentMath.biome(3, false));
        assertEquals(0.75F, CurrentMath.biome(4, false));
        assertEquals(1F, CurrentMath.biome(5, false));
    }

    /** Each type counts once up to five, 6 % each: at most 30 %. */
    @Test
    void faunaBonus() {
        assertEquals(0F, CurrentMath.fauna(0));
        assertEquals(0F, CurrentMath.fauna(-3));
        assertEquals(0.06F, CurrentMath.fauna(1), 1e-6);
        assertEquals(0.30F, CurrentMath.fauna(5), 1e-6);
        assertEquals(0.30F, CurrentMath.fauna(50), 1e-6, "an aquarium stuffed with every type is capped");
        assertEquals(0.30F, CurrentMath.FAUNA_MAX, 1e-6);
        assertEquals(1.3D, CurrentMath.output(1, true, CurrentMath.CUBE_BLOCKS, 0, true, 99), 1e-6, "the whole bonus is at most 30 percent");
    }

    /** The fauna search of tier 1 is near, from tier 2 on it is wide. */
    @Test
    void faunaRadiusPerk() {
        assertEquals(CurrentMath.FAUNA_RADIUS_NEAR, CurrentMath.faunaRadius(1));
        for (int tier = 2; tier <= 5; tier++) assertEquals(CurrentMath.FAUNA_RADIUS_FAR, CurrentMath.faunaRadius(tier));
        assertTrue(CurrentMath.FAUNA_RADIUS_FAR > CurrentMath.FAUNA_RADIUS_NEAR);
    }

    /** Every tier makes at least as much as the one below in the same place: no perk is a penalty. */
    @Test
    void higherTiersNeverMakeLessShare() {
        for (int tier = 2; tier <= 5; tier++) {
            for (boolean prime : new boolean[] {true, false}) {
                for (int flowing = 0; flowing <= 40; flowing += 8) {
                    assertTrue(CurrentMath.output(tier, true, 20, flowing, prime, 2) >= CurrentMath.output(tier - 1, true, 20, flowing, prime, 2), "tier " + tier);
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
            double poor = CurrentMath.output(tier, true, CurrentMath.FULL_WEIGHT / 2, 0, false, 0) * peak(tier);
            double ticksBest = BATTERIES[tier - 1] / best;
            double ticksPoor = BATTERIES[tier - 1] / poor;
            assertTrue(ticksBest <= DAY / 10D, "best case of tier " + tier + " needs " + ticksBest + " ticks");
            assertTrue(ticksPoor <= DAY / 4D, "poor case of tier " + tier + " needs " + ticksPoor + " ticks");
            assertTrue(best * DAY >= BATTERIES[tier - 1] * 10D, "daily yield of tier " + tier + " is " + best * DAY);
        }
    }

    /** The tier 1 peak is at least the 15 FE/t of the economy rule, and the buffer holds 600 ticks of it. */
    @Test
    void tierOneMinimumAndBuffer() {
        assertTrue(peak(1) >= 15);
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(peak(tier) * 600, EnergyCommonConfig.CURRENT_GENERATORS.get(tier - 1).capacity());
            assertEquals(peak(tier) * 5, EnergyCommonConfig.CURRENT_GENERATORS.get(tier - 1).maxOutput());
        }
    }

    /** The comparator shows 0 for nothing and 1..15 for the share of the best possible output, including the fauna bonus. */
    @Test
    void comparatorSignal() {
        assertEquals(0, CurrentMath.signal(0, 15));
        assertEquals(0, CurrentMath.signal(5, 0));
        assertEquals(1, CurrentMath.signal(0.001, 15), "a little is not nothing");
        assertEquals(15, CurrentMath.signal(15 * 1.3, 15));
        assertEquals(15, CurrentMath.signal(1_000_000, 15));
        assertTrue(CurrentMath.signal(15, 15) < 15, "the peak without animals is not the best");
        assertTrue(CurrentMath.signal(7, 15) < CurrentMath.signal(14, 15));
    }
}
