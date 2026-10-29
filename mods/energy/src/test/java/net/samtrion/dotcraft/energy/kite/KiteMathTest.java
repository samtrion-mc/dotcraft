package net.samtrion.dotcraft.energy.kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.kite.KiteMath.Weather;

/** Unit tests for the math of the kite wind generator and its economy anchor. */
class KiteMathTest {
    private static final int[] BATTERY_CAPACITY = {
            EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
            EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};

    /** The tether doubles per tier and is larger than the build height at tier 5, so the kite is not bound to it. */
    @Test
    void tetherGrowsPerTier() {
        for (int tier = 2; tier <= KiteMath.MAX_TIER; tier++) assertEquals(2 * KiteMath.tether(tier - 1), KiteMath.tether(tier));
        assertEquals(32, KiteMath.tether(1));
        assertTrue(KiteMath.tether(5) > 384, "tier 5 must reach beyond the world height from any block");
    }

    /** The output is the peak only with the tether fully out, a free sky and clear weather. */
    @Test
    void peakOnlyAtFullTetherFreeSkyAndClearWeather() {
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            assertEquals(peak, KiteMath.output(peak, tier, KiteMath.tether(tier), 1, Weather.CLEAR, true), 1e-9);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier) / 2.0, 1, Weather.CLEAR, true) < peak);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier), 0.5, Weather.CLEAR, true) < peak);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier), 1, Weather.RAIN, true) <= peak);
        }
    }

    /** The output grows with the altitude and is not clamped by the build height: it keeps growing to the tether, then stops. */
    @Test
    void outputGrowsWithAltitudeUpToTheTether() {
        double last = -1;
        for (int altitude = 0; altitude <= 512; altitude += 16) {
            double now = KiteMath.output(100, 5, altitude, 1, Weather.CLEAR, true);
            assertTrue(now > last, "altitude " + altitude);
            last = now;
        }
        assertEquals(KiteMath.output(100, 5, 512, 1, Weather.CLEAR, true), KiteMath.output(100, 5, 5000, 1, Weather.CLEAR, true));
        assertEquals(0, KiteMath.output(100, 5, 0, 1, Weather.CLEAR, true));
        assertEquals(0, KiteMath.output(100, 5, -3, 1, Weather.CLEAR, true));
    }

    /** Without a sky nothing is made. */
    @Test
    void noSkyNoOutput() {
        assertEquals(0, KiteMath.output(100, 5, 512, 1, Weather.CLEAR, false));
    }

    /** The weather perks: rain hurts less per tier, thunder tears the tether up to tier 3, tier 4 flies at half, tier 5 at full output. */
    @Test
    void weatherPerks() {
        assertEquals(0.5, KiteMath.weatherFactor(1, Weather.RAIN));
        assertEquals(0.75, KiteMath.weatherFactor(2, Weather.RAIN));
        assertEquals(1, KiteMath.weatherFactor(3, Weather.RAIN));
        for (int tier = 1; tier <= 3; tier++) assertEquals(0, KiteMath.weatherFactor(tier, Weather.THUNDER), "tier " + tier);
        assertEquals(0.5, KiteMath.weatherFactor(4, Weather.THUNDER));
        assertEquals(1, KiteMath.weatherFactor(5, Weather.THUNDER));
        for (int tier = 1; tier <= 5; tier++) assertEquals(1, KiteMath.weatherFactor(tier, Weather.CLEAR));
    }

    /** The kite climbs to the last free sample before an obstacle, nothing in the way gives the whole tether. */
    @Test
    void reachStopsBeforeTheObstacle() {
        assertEquals(32, KiteMath.reach(1, h -> false));
        assertEquals(512, KiteMath.reach(5, h -> false));
        assertEquals(0, KiteMath.reach(3, h -> true));
        assertEquals(0, KiteMath.reach(3, h -> h >= 1));
        assertEquals(1, KiteMath.reach(3, h -> h >= 4));
        assertEquals(16, KiteMath.reach(3, h -> h > 16 && h <= 40));
        assertEquals(20, KiteMath.reach(3, h -> h >= 21));
    }

    /** The sample heights are the block above and then every {@link KiteMath#STEP} blocks, never more than the tether. */
    @Test
    void reachSamplesCoarsely() {
        var heights = new java.util.ArrayList<Integer>();
        KiteMath.reach(1, h -> {
            heights.add(h);
            return false;
        });
        assertEquals(1, heights.get(0));
        assertEquals(KiteMath.STEP, heights.get(1));
        assertEquals(2 * KiteMath.STEP, heights.get(2));
        assertEquals(32, heights.get(heights.size() - 1));
    }

    /** The openness is the free share of the samples. */
    @Test
    void opennessIsTheFreeShare() {
        assertEquals(1, KiteMath.openness(16, 16));
        assertEquals(0.25, KiteMath.openness(4, 16));
        assertEquals(1, KiteMath.openness(0, 0));
    }

    /** Economy anchor: the peaks follow the battery ladder, start at 15 FE/t and every tier fills its battery in well under a day. */
    @Test
    void dailyYieldFillsTheBatteryOfTheTierWellUnderADay() {
        int[] ladder = {15, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            assertEquals(ladder[tier - 1], peak);
            double perDay = KiteMath.typicalDailyYield(peak, tier);
            double ticksToFill = BATTERY_CAPACITY[tier - 1] / (perDay / KiteMath.DAY_TICKS);
            assertTrue(perDay > BATTERY_CAPACITY[tier - 1], "tier " + tier + " yields " + perDay + " per day");
            assertTrue(ticksToFill < KiteMath.DAY_TICKS / 4.0, "tier " + tier + " needs " + ticksToFill + " ticks to fill its battery");
        }
        assertTrue(EnergyCommonConfig.KITE_GENERATORS.get(0).peak() >= 15);
    }

    /** The buffer holds 600 ticks of peak and a neighbour can take five times the peak per tick. */
    @Test
    void bufferAndPushRateFollowThePeak() {
        for (var config : EnergyCommonConfig.KITE_GENERATORS) {
            assertEquals(config.peak() * 600, config.capacity());
            assertEquals(config.peak() * 5, config.maxOutput());
        }
    }
}
