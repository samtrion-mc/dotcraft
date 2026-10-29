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
            assertEquals(peak, KiteMath.output(peak, tier, KiteMath.tether(tier), 1, Weather.CLEAR, true, 1), 1e-9);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier) / 2.0, 1, Weather.CLEAR, true, 1) < peak);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier), 1, Weather.CLEAR, true, 0.5) < peak);
            assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier), 1, Weather.RAIN, true, 1) <= peak);
            if (tier < 5) assertTrue(KiteMath.output(peak, tier, KiteMath.tether(tier), 0.5, Weather.CLEAR, true, 1) < peak);
        }
    }

    /** The output grows with the altitude and is not clamped by the build height: it keeps growing to the tether, then stops. */
    @Test
    void outputGrowsWithAltitudeUpToTheTether() {
        double last = -1;
        for (int altitude = 0; altitude <= 512; altitude += 16) {
            double now = KiteMath.output(100, 5, altitude, 1, Weather.CLEAR, true, 1);
            assertTrue(now > last, "altitude " + altitude);
            last = now;
        }
        assertEquals(KiteMath.output(100, 5, 512, 1, Weather.CLEAR, true, 1), KiteMath.output(100, 5, 5000, 1, Weather.CLEAR, true, 1));
        assertEquals(0, KiteMath.output(100, 5, 0, 1, Weather.CLEAR, true, 1));
        assertEquals(0, KiteMath.output(100, 5, -3, 1, Weather.CLEAR, true, 1));
    }

    /** Without a sky nothing is made. */
    @Test
    void noSkyNoOutput() {
        assertEquals(0, KiteMath.output(100, 5, 512, 1, Weather.CLEAR, false, 1));
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

    /** The kite climbs to the last free block before an obstacle, nothing in the way gives the whole tether. */
    @Test
    void reachStopsBeforeTheObstacle() {
        assertEquals(32, KiteMath.reach(1, h -> false));
        assertEquals(512, KiteMath.reach(5, h -> false));
        assertEquals(0, KiteMath.reach(3, h -> true));
        assertEquals(3, KiteMath.reach(3, h -> h >= 4));
        assertEquals(20, KiteMath.reach(3, h -> h >= 21));
    }

    /** No block of the way is skipped: a plate of one block is found at any height. */
    @Test
    void reachSeesAThinPlateAtAnyHeight() {
        for (int plate = 1; plate <= 32; plate++) {
            int at = plate;
            assertEquals(plate - 1, KiteMath.reach(1, h -> h == at), "plate at " + plate);
        }
    }

    /** Rain and snow both count, a covered kite or a dry biome stays clear; thunder only counts where it rains or snows. */
    @Test
    void weatherCountsRainAndSnowAlike() {
        assertEquals(Weather.CLEAR, KiteMath.weather(false, false, true));
        assertEquals(Weather.CLEAR, KiteMath.weather(false, true, true));
        assertEquals(Weather.CLEAR, KiteMath.weather(true, true, false));
        assertEquals(Weather.RAIN, KiteMath.weather(true, false, true));
        assertEquals(Weather.THUNDER, KiteMath.weather(true, true, true));
    }

    /** Tier 1 and 2 need a free cross, tier 3 and up fly through a shaft of one block. */
    @Test
    void slimLineFromTier3() {
        assertTrue(KiteMath.wide(1) && KiteMath.wide(2));
        for (int tier = 3; tier <= 5; tier++) assertTrue(!KiteMath.wide(tier));
    }

    /** A crowded sky costs tier 1-3 all of it, tier 4 half of it and tier 5 nothing. */
    @Test
    void opennessPerkFromTier4() {
        for (int tier = 1; tier <= 3; tier++) assertEquals(0.4, KiteMath.opennessFactor(tier, 0.4), 1e-9);
        assertEquals(0.7, KiteMath.opennessFactor(4, 0.4), 1e-9);
        assertEquals(1, KiteMath.opennessFactor(5, 0.4));
        for (int tier = 1; tier <= 5; tier++) assertEquals(1, KiteMath.opennessFactor(tier, 1));
    }

    /** The gust stays between its bounds and averages {@link KiteMath#WIND_MEAN}; it is defined for negative times as well. */
    @Test
    void windSwingsAroundItsMean() {
        double sum = 0;
        double min = 2;
        double max = -1;
        for (int t = 0; t < 1_038_700; t++) {
            double w = KiteMath.wind(t);
            sum += w;
            min = Math.min(min, w);
            max = Math.max(max, w);
        }
        assertEquals(KiteMath.WIND_MEAN, sum / 1_038_700, 0.01);
        assertTrue(min >= KiteMath.WIND_MIN && min < 0.3, "min " + min);
        assertTrue(max <= 1 && max > 0.9, "max " + max);
        assertTrue(KiteMath.wind(-5) >= KiteMath.WIND_MIN);
    }

    /** The limit names the first reason: no sky, a storm, an obstacle, weather, a crowded sky. */
    @Test
    void limitNamesTheFirstReason() {
        assertEquals(KiteMath.Limit.NO_SKY, KiteMath.limit(1, false, Weather.CLEAR, 32, 1));
        assertEquals(KiteMath.Limit.STORM, KiteMath.limit(1, true, Weather.THUNDER, 32, 1));
        assertEquals(KiteMath.Limit.BLOCKED, KiteMath.limit(1, true, Weather.CLEAR, 10, 1));
        assertEquals(KiteMath.Limit.WEATHER, KiteMath.limit(1, true, Weather.RAIN, 32, 1));
        assertEquals(KiteMath.Limit.CROWDED, KiteMath.limit(1, true, Weather.CLEAR, 32, 0.5));
        assertEquals(KiteMath.Limit.NONE, KiteMath.limit(5, true, Weather.THUNDER, 512, 0.1));
    }

    /** The comparator signal is 0 for no output and 1..15 up to the peak. */
    @Test
    void comparatorSignal() {
        assertEquals(0, KiteMath.signal(0, 15));
        assertEquals(1, KiteMath.signal(0.1, 15));
        assertEquals(15, KiteMath.signal(15, 15));
        assertEquals(15, KiteMath.signal(30, 15));
        assertEquals(8, KiteMath.signal(7.5, 15));
    }

    /** The openness is the free share of the samples. */
    @Test
    void opennessIsTheFreeShare() {
        assertEquals(1, KiteMath.openness(16, 16));
        assertEquals(0.25, KiteMath.openness(4, 16));
        assertEquals(1, KiteMath.openness(0, 0));
    }

    /**
     * Economy anchor on the real model (tether out, free sky, the real gust and weather mix): the peaks follow the battery ladder, start
     * at 15 FE/t and every tier fills its battery in well under a day but not in a moment; the yield per day is held between 2 and 15
     * batteries, the range of the panels.
     */
    @Test
    void dailyYieldFillsTheBatteryOfTheTierWellUnderADay() {
        int[] ladder = {15, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            assertEquals(ladder[tier - 1], peak);
            double perDay = KiteMath.typicalDailyYield(peak, tier);
            double batteries = perDay / BATTERY_CAPACITY[tier - 1];
            assertTrue(batteries >= 2 && batteries <= 15, "tier " + tier + " yields " + batteries + " batteries per day");
            assertTrue(perDay <= peak * KiteMath.DAY_TICKS * KiteMath.WIND_MEAN * 1.01, "tier " + tier + " yields more than the mean wind allows");
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
