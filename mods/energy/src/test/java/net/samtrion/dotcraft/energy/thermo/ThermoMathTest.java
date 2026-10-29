package net.samtrion.dotcraft.energy.thermo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the delta math of the thermoelectric generators and their economy anchor. */
class ThermoMathTest {
    /** The capacities of the batteries of the tiers 1-5, the ladder the peaks follow. */
    private static final int[] BATTERY = {20_000, 100_000, 750_000, 7_500_000, 75_000_000};

    /** Without a hot or without a cold neighbour there is no difference, so nothing is made. */
    @Test
    void needsBothSides() {
        assertEquals(0, ThermoMath.delta(0, 0));
        assertEquals(0, ThermoMath.delta(3, 0));
        assertEquals(0, ThermoMath.delta(0, 3));
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            assertEquals(0F, ThermoMath.share(tier, 3, 0));
            assertEquals(0F, ThermoMath.share(tier, 0, 3));
        }
    }

    /** The difference is the sum of the levels, from the weakest pair (2) to the strongest (6); higher levels count as the highest. */
    @Test
    void deltaIsTheSumOfTheLevels() {
        assertEquals(ThermoMath.MIN_DELTA, ThermoMath.delta(1, 1));
        assertEquals(4, ThermoMath.delta(2, 2));
        assertEquals(6, ThermoMath.delta(3, 3));
        assertEquals(6, ThermoMath.delta(9, 9));
        assertEquals(ThermoMath.delta(3, 1), ThermoMath.delta(1, 3));
    }

    /** The share grows with the difference, never passes 1, and the ceiling falls with the tier: a higher tier gets more out of the same sources. */
    @Test
    void higherTiersNeedLessDelta() {
        for (int tier = 1; tier < ThermoMath.MAX_TIER; tier++) {
            assertTrue(ThermoMath.ceiling(tier) > ThermoMath.ceiling(tier + 1), "ceiling falls at tier " + tier);
            assertTrue(ThermoMath.share(tier, 1, 1) < ThermoMath.share(tier + 1, 1, 1), "weak sources at tier " + tier);
        }
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            float last = 0;
            for (int heat = 1; heat <= 3; heat++) {
                float share = ThermoMath.share(tier, heat, 3);
                assertTrue(share >= last && share <= 1F, "share of tier " + tier + " is monotonic and capped");
                last = share;
            }
        }
        assertEquals(1F, ThermoMath.share(1, 3, 3));
        assertEquals(1F / 3, ThermoMath.share(1, 1, 1), 1e-6);
        assertEquals(1F, ThermoMath.share(5, 1, 1));
    }

    /** More than the best source adds nothing: the arguments are levels, not counts, so a block is bounded by its peak at any ceiling. */
    @Test
    void outputIsBoundedByThePeak() {
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            assertTrue(ThermoMath.share(tier, 100, 100) <= 1F);
        }
    }

    /** The comparator signal is 0 without output, 15 at the peak and at least 1 for any output. */
    @Test
    void comparatorSignal() {
        assertEquals(0, ThermoMath.signal(0F));
        assertEquals(1, ThermoMath.signal(0.001F));
        assertEquals(5, ThermoMath.signal(1F / 3));
        assertEquals(ThermoMath.MAX_SIGNAL, ThermoMath.signal(1F));
    }

    /** The tier 1 peak is at least 15 FE/t and the peaks follow the battery ladder. */
    @Test
    void peaksFollowTheBatteryLadder() {
        int[] ladder = {15, 75, 560, 5_600, 56_000};
        assertTrue(EnergyCommonConfig.THERMO_GENERATORS.get(0).peak() >= 15);
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            var config = EnergyCommonConfig.THERMO_GENERATORS.get(tier - 1);
            assertEquals(ladder[tier - 1], config.peak(), "peak of tier " + tier);
            assertEquals(config.peak() * 600, config.capacity(), "buffer holds 600 ticks of peak");
            assertEquals(config.peak() * 5, config.maxOutput(), "output is 5 times the peak");
        }
    }

    /**
     * The daily yield: constant day and night, so peak x share x 24000 FE. Even the weakest pair of sources (the lowest levels)
     * yields at least four batteries a day and fills the battery of every tier within a quarter of an MC day; the best pair fills
     * it within 2000 ticks (a twelfth of a day).
     */
    @Test
    void everyTierFillsItsBatteryWellUnderADay() {
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.THERMO_GENERATORS.get(tier - 1).peak();
            float weakest = ThermoMath.share(tier, 1, 1);
            float best = ThermoMath.share(tier, 3, 3);
            assertEquals(1F, best, "the strongest sources give the full peak at tier " + tier);
            assertTrue(weakest > 0);
            assertTrue(ThermoMath.dailyYield(peak, weakest) > BATTERY[tier - 1] * 4.0, "daily yield of tier " + tier + " with the weakest sources");
            assertTrue(ThermoMath.ticksToFill(BATTERY[tier - 1], peak, weakest) <= ThermoMath.TICKS_PER_DAY / 4.0,
                    "tier " + tier + " fills its battery in a quarter day with the weakest sources");
            assertTrue(ThermoMath.ticksToFill(BATTERY[tier - 1], peak, best) <= 2_000, "tier " + tier + " with the best sources");
        }
        assertEquals(15 * 24_000.0, ThermoMath.dailyYield(15, 1F), 1e-9);
        assertFalse(Double.isFinite(ThermoMath.ticksToFill(1, 15, 0F)));
    }
}
