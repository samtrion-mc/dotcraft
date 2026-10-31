package net.samtrion.dotcraft.energy.thermo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the delta math of the thermoelectric generators and their economy anchor. */
class ThermoMathTest {
    /** The capacities of the batteries of the tiers 1-5, the ladder the peaks follow. */
    private static final int[] BATTERY = {40_000, 100_000, 750_000, 7_500_000, 75_000_000};

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

    /** An axis needs heat on one face and cold on the opposite one; either way round, and the better way counts. */
    @Test
    void axisNeedsHeatAndColdOnOppositeFaces() {
        assertEquals(ThermoMath.NONE, ThermoMath.axis(3, 0, 0, 0));
        assertEquals(ThermoMath.NONE, ThermoMath.axis(0, 3, 0, 3));
        // heat and cold on the same face do not pair up
        assertEquals(0, ThermoMath.axis(3, 3, 0, 0).delta());
        assertEquals(new ThermoMath.Pair(3, 3), ThermoMath.axis(3, 0, 0, 3));
        assertEquals(new ThermoMath.Pair(3, 3), ThermoMath.axis(0, 3, 3, 0));
        // both ways round exist: the better one wins
        assertEquals(new ThermoMath.Pair(3, 2), ThermoMath.axis(3, 1, 1, 2));
        assertEquals(5, ThermoMath.axis(3, 1, 1, 2).delta());
    }

    /** The axes never stack and heat of one axis never pairs with cold of another: the best single pair counts. */
    @Test
    void bestAxisCountsAndAxesDoNotStack() {
        var weak = ThermoMath.axis(1, 0, 0, 1);
        var strong = ThermoMath.axis(3, 0, 0, 3);
        assertEquals(2, weak.delta());
        assertEquals(strong, ThermoMath.best(weak, strong));
        assertEquals(strong, ThermoMath.best(strong, weak));
        assertEquals(6, ThermoMath.best(ThermoMath.best(strong, strong), strong).delta());
        // lava on one axis and ice on another (touching at an edge) make nothing
        assertEquals(ThermoMath.NONE, ThermoMath.best(ThermoMath.axis(3, 0, 0, 0), ThermoMath.axis(0, 3, 0, 0)));
        assertEquals(0F, ThermoMath.share(5, ThermoMath.NONE.heat(), ThermoMath.NONE.cold()));
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
            assertTrue(ThermoMath.ceiling(tier) >= ThermoMath.ceiling(tier + 1), "ceiling never rises at tier " + tier);
            assertTrue(ThermoMath.share(tier, 1, 1) <= ThermoMath.share(tier + 1, 1, 1), "weak sources at tier " + tier);
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
        assertTrue(ThermoMath.ceiling(1) > ThermoMath.ceiling(ThermoMath.MAX_TIER));
    }

    /**
     * The weakest pair (a campfire and a puddle) must not give the top tiers free power: it yields exactly these shares, and
     * the full peak needs a real difference, so retuning the ceilings fails here.
     */
    @Test
    void weakSourcesGiveOnlyAPartOfThePeak() {
        float[] weak = {1F / 3, 1F / 3, 0.4F, 0.4F, 0.5F};
        int[] fullAt = {6, 6, 5, 5, 4};
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            assertEquals(weak[tier - 1], ThermoMath.share(tier, 1, 1), 1e-6, "weakest share of tier " + tier);
            assertEquals(fullAt[tier - 1], ThermoMath.ceiling(tier));
            assertTrue(ThermoMath.share(tier, 1, 1) <= 0.5F, "the weakest sources give at most half the peak at tier " + tier);
            assertTrue(ThermoMath.share(tier, 2, 1) < 1F, "a mid pair is not full at tier " + tier);
            assertEquals(1F, ThermoMath.share(tier, 3, 3));
        }
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

    /** The tier 1 peak is at least 30 FE/t and the peaks follow the battery ladder. */
    @Test
    void peaksFollowTheBatteryLadder() {
        int[] ladder = {30, 75, 560, 5_600, 56_000};
        assertTrue(EnergyCommonConfig.THERMO_GENERATORS.get(0).peak() >= 30);
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
            assertEquals(1336, ThermoMath.ticksToFill(BATTERY[tier - 1], peak, best), 4, "tier " + tier + " fills its battery in about 1,340 ticks with the best sources");
        }
        assertEquals(15 * 24_000.0, ThermoMath.dailyYield(15, 1F), 1e-9);
        assertFalse(Double.isFinite(ThermoMath.ticksToFill(1, 15, 0F)));
    }
}
