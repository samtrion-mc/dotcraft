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

    /** The length factor is length / reference, 1 at the reference length (tether of tier 1), not capped at 1, capped by the tether. */
    @Test
    void lengthFactorIsLengthOverReference() {
        assertEquals(32, KiteMath.REFERENCE_LENGTH);
        assertEquals(1, KiteMath.lengthFactor(1, 32), 1e-9);
        assertEquals(0.5, KiteMath.lengthFactor(1, 16), 1e-9);
        assertEquals(1, KiteMath.lengthFactor(5, 32), 1e-9);
        assertEquals(0, KiteMath.lengthFactor(5, -4));
        double[] best = {1, 2, 4, 8, 16};
        double[] maxFe = {15, 150, 2_240, 44_800, 896_000};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            assertEquals(best[tier - 1], KiteMath.lengthFactor(tier, 10_000), 1e-9, "tier " + tier);
            assertEquals(maxFe[tier - 1], KiteMath.peak(EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak(), tier), 1e-9, "tier " + tier);
        }
    }

    /** The output is the best case only with the tether fully out, a free sky and clear weather. */
    @Test
    void peakOnlyAtFullTetherFreeSkyAndClearWeather() {
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double base = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            double peak = KiteMath.peak(base, tier);
            assertEquals(peak, KiteMath.output(base, tier, KiteMath.tether(tier), 1, Weather.CLEAR, true, 1), 1e-9);
            assertTrue(KiteMath.output(base, tier, KiteMath.tether(tier) / 2.0, 1, Weather.CLEAR, true, 1) < peak);
            assertTrue(KiteMath.output(base, tier, KiteMath.tether(tier), 1, Weather.CLEAR, true, 0.5) < peak);
            assertTrue(KiteMath.output(base, tier, KiteMath.tether(tier), 1, Weather.RAIN, true, 1) <= peak);
            if (tier != 3 && tier < 5) assertTrue(KiteMath.output(base, tier, KiteMath.tether(tier), 0.5, Weather.CLEAR, true, 1) < peak);
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
        assertEquals(100 * 16, KiteMath.output(100, 5, 512, 1, Weather.CLEAR, true, 1), 1e-9);
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
        assertEquals(0, KiteMath.reach(2, h -> true));
        assertEquals(3, KiteMath.reach(2, h -> h >= 4));
        assertEquals(20, KiteMath.reach(4, h -> h >= 21));
    }

    /** Only the tier 3 kite ignores obstructions: nothing on the way stops it, a crowded sky costs nothing, and no limit is named. */
    @Test
    void tier3IgnoresObstructions() {
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) assertEquals(tier == 3, KiteMath.ignoresObstructions(tier), "tier " + tier);
        assertEquals(KiteMath.tether(3), KiteMath.reach(3, h -> true));
        assertEquals(KiteMath.tether(3), KiteMath.reach(3, h -> h >= 2));
        assertEquals(1, KiteMath.opennessFactor(3, 0));
        double peak = EnergyCommonConfig.KITE_GENERATORS.get(2).peak();
        assertEquals(KiteMath.peak(peak, 3), KiteMath.output(peak, 3, KiteMath.tether(3), 0, Weather.CLEAR, true, 1), 1e-9);
        assertEquals(KiteMath.Limit.NONE, KiteMath.limit(3, true, Weather.CLEAR, KiteMath.reach(3, h -> true), 0));
        assertTrue(KiteMath.output(peak, 2, KiteMath.tether(2), 0, Weather.CLEAR, true, 1) == 0);
    }

    /** Tier 4 and 5 fly two kites, the others one; the shares diminish and add up to 1, so the pair never exceeds the peak. */
    @Test
    void twoKitesFromTier4SplitThePeak() {
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            assertEquals(tier >= 4 ? 2 : 1, KiteMath.kites(tier), "tier " + tier);
            double sum = 0;
            for (int slot = 0; slot < KiteMath.MAX_KITES; slot++) sum += KiteMath.slotShare(tier, slot);
            assertEquals(1, sum, 1e-9, "tier " + tier);
            assertEquals(0, KiteMath.slotShare(tier, KiteMath.kites(tier)), "no slot beyond the kites of tier " + tier);
            assertEquals(0, KiteMath.slotShare(tier, -1));
        }
        assertTrue(KiteMath.slotShare(4, 0) > KiteMath.slotShare(4, 1) && KiteMath.slotShare(4, 1) > 0, "diminishing");
        for (int tier = 4; tier <= 5; tier++) {
            double base = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            double peak = KiteMath.peak(base, tier);
            int t = tier;
            double both = 0;
            double one = 0;
            for (int slot = 0; slot < 2; slot++) {
                double part = KiteMath.slotOutput(base, t, slot, KiteMath.tether(t), 1, Weather.CLEAR, true, 1);
                both += part;
                if (slot == 0) one = part;
            }
            assertEquals(peak, both, 1e-6, "the pair makes exactly the peak at best");
            assertTrue(one < peak, "one kite alone stays under the peak");
        }
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

    /** A crowded sky costs tier 1 and 2 all of it, tier 4 half of it, tier 3 (it ignores obstructions) and tier 5 nothing. */
    @Test
    void opennessPerkFromTier4() {
        for (int tier = 1; tier <= 2; tier++) assertEquals(0.4, KiteMath.opennessFactor(tier, 0.4), 1e-9);
        assertEquals(1, KiteMath.opennessFactor(3, 0.4));
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
     * Economy anchor on the real model (tether out, free sky, the real gust and weather mix): the bases follow the battery ladder and
     * the tether is the yield factor (1 / 2 / 4 / 8 / 16). Per unit of length factor the yield is held between 2 and 15 batteries per
     * day, the range of the panels, so a tier with a factor f yields between 2 f and 15 f batteries (about 10 f).
     */
    @Test
    void dailyYieldFillsTheBatteryOfTheTierWellUnderADay() {
        int[] ladder = {15, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double base = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            assertEquals(ladder[tier - 1], base);
            double factor = KiteMath.lengthFactor(tier, KiteMath.tether(tier));
            double perDay = KiteMath.typicalDailyYield(base, tier);
            double batteries = perDay / BATTERY_CAPACITY[tier - 1];
            assertTrue(batteries / factor >= 2 && batteries / factor <= 15, "tier " + tier + " yields " + batteries + " batteries per day");
            assertTrue(perDay <= KiteMath.peak(base, tier) * KiteMath.DAY_TICKS * KiteMath.WIND_MEAN * 1.01,
                    "tier " + tier + " yields more than the mean wind allows");
            // the battery is filled in well over a tick and well under a day at the very best
            double bestTicks = (double) BATTERY_CAPACITY[tier - 1] / KiteMath.peak(base, tier);
            assertTrue(bestTicks >= 50 && bestTicks <= 2_500, "tier " + tier + " fills its battery in " + bestTicks + " ticks at best");
        }
    }

    /** Only the kites that stay up in a thunderstorm (tier 4 and 5) are struck, only in a thunderstorm, after the cooldown and on a low roll. */
    @Test
    void lightningChanceAndCooldown() {
        double hit = KiteMath.LIGHTNING_CHANCE_PER_SECOND / 2;
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            assertEquals(tier >= 4, KiteMath.struck(tier, Weather.THUNDER, 0, hit), "tier " + tier);
            assertTrue(!KiteMath.struck(tier, Weather.RAIN, 0, 0) && !KiteMath.struck(tier, Weather.CLEAR, 0, 0), "tier " + tier + " struck without thunder");
        }
        assertTrue(!KiteMath.struck(5, Weather.THUNDER, 1, 0), "struck during the cooldown");
        assertTrue(KiteMath.struck(5, Weather.THUNDER, 0, 0) && KiteMath.struck(5, Weather.THUNDER, -50, 0));
        assertTrue(!KiteMath.struck(5, Weather.THUNDER, 0, KiteMath.LIGHTNING_CHANCE_PER_SECOND), "the chance is a strict bound");
        assertTrue(KiteMath.LIGHTNING_CHANCE_PER_SECOND <= 0.05 && KiteMath.LIGHTNING_COOLDOWN_TICKS >= 1_200, "a strike must stay rare");
        assertEquals(1_000_000, EnergyCommonConfig.kiteLightningEnergy());
    }

    /** A bolt fills the buffer first, then the neighbours take a burst, and what neither takes is lost. */
    @Test
    void lightningCreditSplitsIntoBufferNeighboursAndLoss() {
        assertEquals(new KiteMath.Credit(1_000_000, 0, 0), KiteMath.credit(1_000_000, 5_000_000, 0));
        assertEquals(new KiteMath.Credit(300_000, 700_000, 0), KiteMath.credit(1_000_000, 300_000, 5_000_000));
        assertEquals(new KiteMath.Credit(300_000, 200_000, 500_000), KiteMath.credit(1_000_000, 300_000, 200_000));
        assertEquals(new KiteMath.Credit(0, 0, 1_000_000), KiteMath.credit(1_000_000, 0, 0));
        assertEquals(new KiteMath.Credit(0, 0, 0), KiteMath.credit(0, 10, 10));
        assertEquals(new KiteMath.Credit(0, 0, 100), KiteMath.credit(100, -5, -5));
    }

    /** The buffer holds 600 ticks of the best case (base x length factor) and a neighbour can take five times that per tick. */
    @Test
    void bufferAndPushRateFollowThePeak() {
        for (var config : EnergyCommonConfig.KITE_GENERATORS) {
            int tier = EnergyCommonConfig.KITE_GENERATORS.indexOf(config) + 1;
            int best = (int) KiteMath.peak(config.peak(), tier);
            assertEquals(best * 600, config.capacity());
            assertEquals(best * 5, config.maxOutput());
        }
    }
}
