package net.samtrion.dotcraft.energy.vibration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the weights, the perks and the economy anchor of the vibration generators. */
class VibrationMathTest {
    private static final EnergyCommonConfig.Battery[] BATTERIES = {EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY, EnergyCommonConfig.BATTERY_TIER_2,
            EnergyCommonConfig.BATTERY_TIER_3, EnergyCommonConfig.BATTERY_TIER_4, EnergyCommonConfig.BATTERY_TIER_5};

    /** A louder event is worth more, linearly; the loudest is {@link VibrationMath#EVENT_TICKS} ticks of peak; frequencies outside 1..15 are worth nothing. */
    @Test
    void weightsByFrequency() {
        assertEquals(150, VibrationMath.eventEnergy(15, 15));
        assertEquals(120, VibrationMath.eventEnergy(15, 12));
        assertEquals(10, VibrationMath.eventEnergy(15, 1));
        for (int f = 2; f <= 15; f++) assertTrue(VibrationMath.eventEnergy(75, f) > VibrationMath.eventEnergy(75, f - 1));
        assertEquals(0, VibrationMath.eventEnergy(75, 0));
        assertEquals(0, VibrationMath.eventEnergy(75, 16));
        assertEquals(0, VibrationMath.eventEnergy(75, -3));
        assertEquals(0, VibrationMath.eventEnergy(0, 15));
        // the biggest default peak does not overflow
        assertEquals(560_000, VibrationMath.eventEnergy(56_000, 15));
    }

    /** One loudest event always fits into the bucket, so no single event is cut by the burst limit. */
    @Test
    void loudestEventFitsBurst() {
        for (var g : EnergyCommonConfig.VIBRATION_GENERATORS) assertTrue(VibrationMath.eventEnergy(g.peak(), 15) <= VibrationMath.burst(g.peak()));
    }

    /** The perks only grow with the tier: a larger radius, fainter shocks, and wool is pierced from tier 4 on. */
    @Test
    void perksGrowWithTier() {
        for (int tier = 2; tier <= 5; tier++) {
            assertTrue(VibrationMath.radius(tier) > VibrationMath.radius(tier - 1));
            assertTrue(VibrationMath.minFrequency(tier) < VibrationMath.minFrequency(tier - 1));
        }
        assertFalse(VibrationMath.pierces(3));
        assertTrue(VibrationMath.pierces(4));
        assertTrue(VibrationMath.pierces(5));
    }

    /** What a tier hears: only the loud events at tier 1, everything with a frequency at tier 5, never frequency 0. */
    @Test
    void hearing() {
        assertFalse(VibrationMath.hears(1, 6));
        assertTrue(VibrationMath.hears(1, 7));
        assertTrue(VibrationMath.hears(1, 15));
        assertFalse(VibrationMath.hears(4, 1));
        assertTrue(VibrationMath.hears(5, 1));
        for (int tier = 1; tier <= 5; tier++) {
            assertFalse(VibrationMath.hears(tier, 0));
            assertFalse(VibrationMath.hears(tier, 16));
            assertTrue(VibrationMath.hears(tier, 12), "a block break is heard by every tier");
        }
    }

    /** The ceiling is the peak per tick plus one burst. */
    @Test
    void ceiling() {
        assertEquals(300, VibrationMath.burst(15));
        assertEquals(300 + 15 * 24000L, VibrationMath.maxYield(15, 24000));
        assertEquals(VibrationMath.burst(75), VibrationMath.maxYield(75, 0));
    }

    /**
     * The economy anchor: the peaks follow the battery ladder 15 / 75 / 560 / 5600 / 56000 FE/t, none below 15; with a busy player
     * (a quarter of the day at peak) a generator fills the battery of its tier 2..15 times a day, and at full duty the ceiling is below
     * 25 batteries a day, so even a farm cannot fill more: the cap is the real limit.
     */
    @Test
    void dailyYieldMatchesBattery() {
        for (int tier = 1; tier <= 5; tier++) {
            int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            double capacity = BATTERIES[tier - 1].capacity();
            double busy = VibrationMath.busyDailyYield(peak) / capacity;
            assertTrue(busy > 2 && busy < 15, "tier " + tier + ": " + busy);
            double ceiling = VibrationMath.maxYield(peak, VibrationMath.DAY_TICKS) / capacity;
            assertTrue(ceiling < 25, "tier " + tier + ": " + ceiling);
            double ticksToFill = capacity / peak;
            assertTrue(ticksToFill >= VibrationMath.DAY_TICKS / 25.0, "tier " + tier + ": " + ticksToFill);
            assertTrue(peak >= 15);
        }
        assertEquals(15, EnergyCommonConfig.VIBRATION_GENERATORS.get(0).peak());
        assertEquals(75, EnergyCommonConfig.VIBRATION_GENERATORS.get(1).peak());
        assertEquals(560, EnergyCommonConfig.VIBRATION_GENERATORS.get(2).peak());
        assertEquals(5_600, EnergyCommonConfig.VIBRATION_GENERATORS.get(3).peak());
        assertEquals(56_000, EnergyCommonConfig.VIBRATION_GENERATORS.get(4).peak());
    }

    /** The defaults: five tiers, buffer 600 ticks of peak, output 5x the peak. */
    @Test
    void configDefaults() {
        var list = EnergyCommonConfig.VIBRATION_GENERATORS;
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(list.get(i).peak() * 600, list.get(i).capacity());
            assertEquals(list.get(i).peak() * 5, list.get(i).maxOutput());
        }
    }
}
