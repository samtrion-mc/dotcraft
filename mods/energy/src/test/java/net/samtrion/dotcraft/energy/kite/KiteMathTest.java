package net.samtrion.dotcraft.energy.kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.kite.KiteMath.Weather;

/** Unit tests for the math of the kite wind generator and its economy anchor. */
class KiteMathTest {
    private static final int[] BATTERY_CAPACITY = {
            EnergyCommonConfig.BATTERY_TIER_1.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
            EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};

    /** The tether ladder is 16 / 24 / 32 / 48 / 64 and ends at 64 blocks. */
    @Test
    void tetherGrowsPerTier() {
        int[] ladder = {16, 24, 32, 48, 64};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) assertEquals(ladder[tier - 1], KiteMath.tether(tier));
        assertEquals(64, KiteMath.tether(5), "the longest tether is 64 blocks");
    }

    /** The length factor is length / 64, 1 at the longest tether (tier 5), capped by the tether. */
    @Test
    void lengthFactorIsLengthOverReference() {
        assertEquals(64, KiteMath.REFERENCE_LENGTH);
        assertEquals(0.25, KiteMath.lengthFactor(1, 16), 1e-9);
        assertEquals(0.125, KiteMath.lengthFactor(1, 8), 1e-9);
        assertEquals(0.25, KiteMath.lengthFactor(5, 16), 1e-9);
        assertEquals(0, KiteMath.lengthFactor(5, -4));
        double[] best = {0.25, 0.375, 0.5, 0.75, 1};
        double[] maxFe = {20, 150, 1_300, 12_999.75, 128_000};
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
        for (int altitude = 0; altitude <= 64; altitude += 4) {
            double now = KiteMath.output(100, 5, altitude, 1, Weather.CLEAR, true, 1);
            assertTrue(now > last, "altitude " + altitude);
            last = now;
        }
        assertEquals(KiteMath.output(100, 5, 64, 1, Weather.CLEAR, true, 1), KiteMath.output(100, 5, 5000, 1, Weather.CLEAR, true, 1));
        assertEquals(100, KiteMath.output(100, 5, 64, 1, Weather.CLEAR, true, 1), 1e-9);
        assertEquals(0, KiteMath.output(100, 5, 0, 1, Weather.CLEAR, true, 1));
        assertEquals(0, KiteMath.output(100, 5, -3, 1, Weather.CLEAR, true, 1));
    }

    /** Without a sky nothing is made. */
    @Test
    void noSkyNoOutput() {
        assertEquals(0, KiteMath.output(100, 5, 64, 1, Weather.CLEAR, false, 1));
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
        assertEquals(16, KiteMath.reach(1, h -> false));
        assertEquals(64, KiteMath.reach(5, h -> false));
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
        for (int plate = 1; plate <= 16; plate++) {
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
        assertEquals(KiteMath.Limit.NO_SKY, KiteMath.limit(1, false, Weather.CLEAR, 16, 1));
        assertEquals(KiteMath.Limit.STORM, KiteMath.limit(1, true, Weather.THUNDER, 16, 1));
        assertEquals(KiteMath.Limit.BLOCKED, KiteMath.limit(1, true, Weather.CLEAR, 10, 1));
        assertEquals(KiteMath.Limit.WEATHER, KiteMath.limit(1, true, Weather.RAIN, 16, 1));
        assertEquals(KiteMath.Limit.CROWDED, KiteMath.limit(1, true, Weather.CLEAR, 16, 0.5));
        assertEquals(KiteMath.Limit.NONE, KiteMath.limit(5, true, Weather.THUNDER, 64, 0.1));
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
     * Economy anchor on the real model (tether out, free sky, the real gust and weather mix): the batteries of the same tier per day
     * are about 6.1 / 19.5 / 23.7 / 24.3 / 24.6 (tier 1-5), each within 0.5 of its target (tier 1 at least 6, the others within 8..25).
     */
    @Test
    void dailyYieldFillsTheBatteryOfTheTierWellUnderADay() {
        int[] bases = {80, 400, 2_600, 17_333, 128_000};
        double[] target = {6.1, 19.5, 23.7, 24.3, 24.6};
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double base = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            assertEquals(bases[tier - 1], base);
            double perDay = KiteMath.typicalDailyYield(base, tier);
            double batteries = perDay / BATTERY_CAPACITY[tier - 1];
            assertTrue(batteries >= (tier == 1 ? 6 : 8) && batteries <= 25, "tier " + tier + " yields " + batteries + " batteries per day");
            assertEquals(target[tier - 1], batteries, 0.5, "tier " + tier + " yields " + batteries + " batteries per day");
            assertTrue(perDay <= KiteMath.peak(base, tier) * KiteMath.DAY_TICKS * KiteMath.WIND_MEAN * 1.01,
                    "tier " + tier + " yields more than the mean wind allows");
            // the battery is filled in well over a tick and well under a day at the very best
            double bestTicks = (double) BATTERY_CAPACITY[tier - 1] / KiteMath.peak(base, tier);
            assertTrue(bestTicks >= 50 && bestTicks <= 2_500, "tier " + tier + " fills its battery in " + bestTicks + " ticks at best");
        }
    }

    /** The typical daily yield quoted in the README kite paragraph matches the real model within 0.1 (one printed decimal) for every tier. */
    @Test
    void readmeQuotesTheTypicalDailyYield() throws IOException {
        Path readme = Path.of("README.md");
        if (!Files.exists(readme)) readme = Path.of("..", "..", "README.md");
        Matcher m = Pattern.compile("gives about ([0-9.]+) / ([0-9.]+) / ([0-9.]+) / ([0-9.]+) / ([0-9.]+) batteries")
                .matcher(Files.readString(readme));
        assertTrue(m.find(), "README kite paragraph with the typical daily yield not found");
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            double base = EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak();
            double batteries = KiteMath.typicalDailyYield(base, tier) / BATTERY_CAPACITY[tier - 1];
            assertEquals(batteries, Double.parseDouble(m.group(tier)), 0.1, "README tier " + tier);
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
            double best = KiteMath.peak(config.peak(), tier);
            assertEquals(Math.round(best * 600), config.capacity());
            assertEquals(Math.round(best * 5), config.maxOutput());
        }
    }

    /** One wire is one block of tether, at most the tether of the tier; the kite reaches min(wire, tether, free altitude). */
    @Test
    void wireIsCappedByTheTetherAndLimitsTheFlight() {
        assertEquals(10, KiteMath.wireStored(1, 0, 10));
        assertEquals(16, KiteMath.wireStored(1, 10, 100), "capped at the tether of tier 1");
        assertEquals(64, KiteMath.wireStored(5, 60, 64));
        assertEquals(10, KiteMath.wireStored(1, 10, -3), "negative amounts add nothing");
        assertEquals(0, KiteMath.flightTarget(5, 0, 64), "no wire, no flight");
        assertEquals(7, KiteMath.flightTarget(5, 7, 64));
        assertEquals(16, KiteMath.flightTarget(1, 64, 100), "the tether of the tier caps it");
        assertEquals(5, KiteMath.flightTarget(5, 64, 5), "the free altitude caps it");
        assertEquals(0.125, KiteMath.lengthFactor(5, KiteMath.flightTarget(5, 8, 64)), 1e-9, "the wire feeds the length factor");
    }

    /** The play of the pair stays inside +-4, the two kites stay 3 apart, tier 1-3 hang on the axis, and it is deterministic. */
    @Test
    void spreadStaysInsideThePlayAndKeepsTheKitesApart() {
        assertEquals(new KiteMath.Offset(0, 0), KiteMath.offset(3, 0, 12345, false));
        double minX0 = 99;
        double maxX0 = -99;
        for (long t = 0; t < 40_000; t += 7) {
            var a = KiteMath.offset(4, 0, t, false);
            var b = KiteMath.offset(5, 1, t, false);
            assertTrue(Math.abs(a.x()) <= 4 && Math.abs(a.z()) <= 4 && Math.abs(b.x()) <= 4 && Math.abs(b.z()) <= 4, "outside +-4 at " + t);
            assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= KiteMath.MIN_KITE_GAP, "kites closer than 3 at " + t);
            assertEquals(a, KiteMath.offset(4, 0, t, false));
            minX0 = Math.min(minX0, a.x());
            maxX0 = Math.max(maxX0, a.x());
        }
        assertTrue(maxX0 - minX0 > 2, "the kite does not wander");
        var step = KiteMath.offset(4, 0, 101, false);
        var next = KiteMath.offset(4, 0, 102, false);
        assertTrue(Math.hypot(step.x() - next.x(), step.z() - next.z()) < 0.1, "the drift is not smooth");
    }

    /** In a thunderstorm the horizontal offset is clamped to a radius of 6; the play lies inside it and is not touched. */
    @Test
    void thunderClampsTheOffsetToTheRadius() {
        assertEquals(6, KiteMath.THUNDER_RADIUS);
        var far = KiteMath.clamp(new KiteMath.Offset(12, 0), KiteMath.THUNDER_RADIUS);
        assertEquals(6, far.x(), 1e-9);
        var diagonal = KiteMath.clamp(new KiteMath.Offset(8, 8), KiteMath.THUNDER_RADIUS);
        assertEquals(6, Math.hypot(diagonal.x(), diagonal.z()), 1e-9);
        assertEquals(new KiteMath.Offset(3, -4), KiteMath.clamp(new KiteMath.Offset(3, -4), 6));
        for (long t = 0; t < 40_000; t += 11) {
            var o = KiteMath.offset(5, 0, t, true);
            assertTrue(Math.hypot(o.x(), o.z()) <= KiteMath.THUNDER_RADIUS + 1e-9);
            assertEquals(KiteMath.offset(5, 0, t, false), o, "the play is inside the radius");
        }
    }

    /** A bolt within 6 blocks of the kite or any point of the tether is attracted; further away it is not. */
    @Test
    void boltsWithinSixBlocksOfKiteOrTetherAreAttracted() {
        double[] anchor = {0.5, 64, 0.5};
        double[] kite = {3, 94, 2};
        assertTrue(KiteMath.attracts(new double[] {3, 94, 2 + 5.9}, anchor, kite), "near the kite");
        assertTrue(KiteMath.attracts(new double[] {5, 64, 0.5}, anchor, kite), "near the foot of the tether");
        assertTrue(KiteMath.attracts(new double[] {2 + 3, 79, 1}, anchor, kite), "beside the middle of the tether");
        assertTrue(!KiteMath.attracts(new double[] {0.5 + 6.5, 64, 0.5}, anchor, kite), "too far from the foot");
        assertTrue(!KiteMath.attracts(new double[] {3, 120, 2}, anchor, kite), "above the kite");
        assertTrue(KiteMath.attracts(anchor, anchor, anchor), "a zero-length tether does not divide by zero");
    }

    /** Two generators closer than 16 blocks horizontally, and within the tether vertically, block each other (symmetric). */
    @Test
    void minimumDistanceBlocksBoth() {
        assertEquals(16, KiteMath.MIN_DISTANCE);
        assertTrue(KiteMath.tooClose(15, 0, 0, 1, 1));
        assertTrue(KiteMath.tooClose(0, 0, -15, 3, 5));
        assertTrue(!KiteMath.tooClose(16, 0, 0, 1, 1), "exactly 16 is allowed");
        assertTrue(KiteMath.tooClose(11, 0, 11, 1, 1), "diagonal 15.55");
        assertTrue(!KiteMath.tooClose(12, 0, 12, 1, 1), "diagonal 16.97");
        assertTrue(!KiteMath.tooClose(3, 17, 3, 1, 1), "above the tether of tier 1");
        assertTrue(KiteMath.tooClose(3, 17, 3, 1, 2), "within the longer tether of tier 2");
        assertEquals(KiteMath.tooClose(3, -30, 3, 1, 4), KiteMath.tooClose(-3, 30, -3, 4, 1), "symmetric");
    }
}
