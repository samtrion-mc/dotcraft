package net.samtrion.dotcraft.energy.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the math of the solar panels and their economy anchor. */
class GeneratorMathTest {
    /** The sun angle over one MC day: 0 degrees at noon (tick 6000), 180 at midnight. */
    private static float sunAngle(int tick) {
        return (tick - 6000) * 360F / 24000F;
    }

    /** Average output over a day as share of the peak, for a clear sky. */
    private static double dailyShare(int tier) {
        double sum = 0;
        for (int t = 0; t < 24000; t += 10) {
            sum += GeneratorMath.solar(tier, sunAngle(t), true, GeneratorMath.Weather.CLEAR, 1F);
        }
        return sum / 2400;
    }

    /** The sun stands highest at noon and is gone at midnight. */
    @Test
    void sunHeight() {
        assertEquals(1F, GeneratorMath.daylight(0F), 1e-6);
        assertEquals(0F, GeneratorMath.daylight(90F), 1e-6);
        assertEquals(0F, GeneratorMath.daylight(180F));
        assertEquals(0F, GeneratorMath.daylight(270F), 1e-6);
    }

    /** Without sky access nothing is made. */
    @Test
    void noSkyNoOutput() {
        assertEquals(0, GeneratorMath.solar(5, 0F, false, GeneratorMath.Weather.CLEAR, 1F));
    }

    /** Rain cuts tiers 1-4 by their perk and leaves tier 5 untouched; a thunderstorm halves what rain leaves, also for tier 5. */
    @Test
    void weatherPerk() {
        for (int tier = 1; tier <= 5; tier++) {
            double dry = GeneratorMath.solar(tier, 0F, true, GeneratorMath.Weather.CLEAR, 1F);
            double rain = GeneratorMath.solar(tier, 0F, true, GeneratorMath.Weather.RAIN, 1F);
            double storm = GeneratorMath.solar(tier, 0F, true, GeneratorMath.Weather.THUNDER, 1F);
            assertTrue(tier == 5 ? rain == dry : rain < dry, "rain tier " + tier);
            assertEquals(rain * GeneratorMath.THUNDER_FACTOR, storm, 1e-9, "thunder tier " + tier);
        }
    }

    /** A solar panel makes nothing at night, whatever its tier. */
    @Test
    void noSolarAtNight() {
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(0, GeneratorMath.solar(tier, 180F, true, GeneratorMath.Weather.CLEAR, 1F), "tier " + tier);
        }
    }

    /** Tracking widens the plateau of the higher tiers: the same low sun gives more. */
    @Test
    void trackingGrowsWithTier() {
        double previous = -1;
        for (int tier = 1; tier <= 5; tier++) {
            double v = GeneratorMath.solar(tier, 60F, true, GeneratorMath.Weather.CLEAR, 1F);
            assertTrue(v >= previous, "tier " + tier);
            previous = v;
        }
    }

    /** A waterlogged panel keeps 20 % of its output. */
    @Test
    void waterloggedLoses80Percent() {
        assertEquals(1F, GeneratorMath.waterlogged(false));
        assertEquals(0.2F, GeneratorMath.waterlogged(true), 1e-6);
    }

    /** A panel fills the battery of its tier in well under one MC day, but not in minutes: the daily yield of the default peak is 2..15 times its capacity; no tier peaks below 15 FE/t. */
    @Test
    void dailyYieldMatchesBattery() {
        var batteries = new EnergyCommonConfig.Battery[] {EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY, EnergyCommonConfig.BATTERY_TIER_2,
                EnergyCommonConfig.BATTERY_TIER_3, EnergyCommonConfig.BATTERY_TIER_4, EnergyCommonConfig.BATTERY_TIER_5};
        for (int tier = 1; tier <= 5; tier++) {
            double solar = EnergyCommonConfig.SOLAR_PANELS.get(tier - 1).peak() * dailyShare(tier) * 24000;
            double capacity = batteries[tier - 1].capacity();
            assertTrue(solar / capacity > 2 && solar / capacity < 15, "solar tier " + tier + ": " + solar / capacity);
        }
    }

    /** No default peak is below 15 FE/t. */
    @Test
    void minimumPeak() {
        for (var g : EnergyCommonConfig.SOLAR_PANELS) assertTrue(g.peak() >= 15);
    }

    /** The defaults: five tiers each, buffer and output rate derived from the peak, peaks rising. */
    @Test
    void configDefaults() {
        for (var list : java.util.List.of(EnergyCommonConfig.SOLAR_PANELS)) {
            assertEquals(5, list.size());
            for (int i = 0; i < 5; i++) {
                assertEquals(list.get(i).peak() * 600, list.get(i).capacity());
                assertEquals(list.get(i).peak() * 5, list.get(i).maxOutput());
                if (i > 0) assertTrue(list.get(i).peak() > list.get(i - 1).peak());
            }
        }
    }
}
