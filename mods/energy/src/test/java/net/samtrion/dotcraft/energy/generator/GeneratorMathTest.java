package net.samtrion.dotcraft.energy.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the math of the solar and lunar panels and their economy anchor. */
class GeneratorMathTest {
    /** The sun angle over one MC day: 0 degrees at noon (tick 6000), 180 at midnight. */
    private static float sunAngle(int tick) {
        return (tick - 6000) * 360F / 24000F;
    }

    /** Average output over a day as share of the peak, for a clear sky and the given moon phase (solar ignores it). */
    private static double dailyShare(boolean solar, int tier, int phase) {
        double sum = 0;
        for (int t = 0; t < 24000; t += 10) {
            sum += solar ? GeneratorMath.solar(tier, sunAngle(t), true, GeneratorMath.Weather.CLEAR, 1F)
                    : GeneratorMath.lunar(tier, sunAngle(t), phase, true, GeneratorMath.Weather.CLEAR, 1F, 0);
        }
        return sum / 2400;
    }

    /** The sun stands highest at noon, is gone at midnight; the moon is the other way round. */
    @Test
    void sunAndMoonHeight() {
        assertEquals(1F, GeneratorMath.daylight(0F), 1e-6);
        assertEquals(0F, GeneratorMath.daylight(90F), 1e-6);
        assertEquals(0F, GeneratorMath.daylight(180F));
        assertEquals(0F, GeneratorMath.daylight(270F), 1e-6);
        assertEquals(1F, GeneratorMath.moonlight(180F), 1e-6);
        assertEquals(0F, GeneratorMath.moonlight(0F));
    }

    /** Sun and moon never count at the same angle, so no panel makes FE from the other's light. */
    @Test
    void sunAndMoonDoNotOverlap() {
        for (float angle = 0; angle <= 360; angle += 1F) {
            assertEquals(0F, GeneratorMath.daylight(angle) * GeneratorMath.moonlight(angle), "angle " + angle);
        }
    }

    /** Without sky access nothing is made. */
    @Test
    void noSkyNoOutput() {
        assertEquals(0, GeneratorMath.solar(5, 0F, false, GeneratorMath.Weather.CLEAR, 1F));
        assertEquals(0, GeneratorMath.lunar(5, 180F, 0, false, GeneratorMath.Weather.CLEAR, 1F, 0));
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

    /** The new moon floor and the phase order: full moon beats new moon, symmetric around it. */
    @Test
    void moonPhases() {
        for (int tier = 1; tier <= 5; tier++) {
            double full = GeneratorMath.lunar(tier, 180F, 0, true, GeneratorMath.Weather.CLEAR, 1F, 0);
            double newMoon = GeneratorMath.lunar(tier, 180F, 4, true, GeneratorMath.Weather.CLEAR, 1F, 0);
            assertEquals(1.0, full, 1e-6);
            assertTrue(newMoon > 0 && newMoon < full, "tier " + tier);
            assertEquals(GeneratorMath.lunar(tier, 180F, 2, true, GeneratorMath.Weather.CLEAR, 1F, 0), GeneratorMath.lunar(tier, 180F, 6, true, GeneratorMath.Weather.CLEAR, 1F, 0), 1e-6);
        }
    }

    /** The tracking perk widens the plateau tier by tier: each tier gives strictly more of a low sun, the plateau is capped at 1. */
    @Test
    void trackingStrictlyGrowsAndCaps() {
        float[] expected = {0F, 0F, 0.15F, 0.3F, 0.45F};
        for (int tier = 1; tier <= 5; tier++) {
            assertEquals(0.5F * (1F + expected[tier - 1]), GeneratorMath.tracked(0.5F, tier), 1e-6, "tier " + tier);
            assertEquals(1F, GeneratorMath.tracked(1F, tier), 1e-6, "cap tier " + tier);
            assertEquals(0F, GeneratorMath.tracked(0F, tier), 1e-6, "zero tier " + tier);
        }
        for (int tier = 3; tier <= 5; tier++) {
            assertTrue(GeneratorMath.tracked(0.5F, tier) > GeneratorMath.tracked(0.5F, tier - 1), "tier " + tier);
        }
    }

    /** Thunder halves the rain share of every tier, tier 5 keeps full output in rain and half in a storm. */
    @Test
    void thunderAtTier5() {
        assertEquals(1F, GeneratorMath.weather(5, GeneratorMath.Weather.RAIN), 1e-6);
        assertEquals(0.5F, GeneratorMath.weather(5, GeneratorMath.Weather.THUNDER), 1e-6);
        assertEquals(0.1F, GeneratorMath.weather(1, GeneratorMath.Weather.THUNDER), 1e-6);
    }

    /** The biome boost and malus scale solar and lunar output alike. */
    @Test
    void biomeFactor() {
        double plain = GeneratorMath.solar(3, 0F, true, GeneratorMath.Weather.CLEAR, 1F);
        assertEquals(plain * GeneratorMath.BIOME_BOOST, GeneratorMath.solar(3, 0F, true, GeneratorMath.Weather.CLEAR, GeneratorMath.BIOME_BOOST), 1e-6);
        assertEquals(plain * GeneratorMath.BIOME_MALUS, GeneratorMath.solar(3, 0F, true, GeneratorMath.Weather.CLEAR, GeneratorMath.BIOME_MALUS), 1e-6);
        double moon = GeneratorMath.lunar(3, 180F, 0, true, GeneratorMath.Weather.CLEAR, 1F, 0);
        assertEquals(moon * GeneratorMath.BIOME_BOOST, GeneratorMath.lunar(3, 180F, 0, true, GeneratorMath.Weather.CLEAR, GeneratorMath.BIOME_BOOST, 0), 1e-6);
        assertEquals(moon * GeneratorMath.BIOME_MALUS, GeneratorMath.lunar(3, 180F, 0, true, GeneratorMath.Weather.CLEAR, GeneratorMath.BIOME_MALUS, 0), 1e-6);
    }

    /** Phases 1, 3, 5 and 7 mirror each other around the new moon and sit between full and new moon at every tier. */
    @Test
    void oddMoonPhases() {
        var clear = GeneratorMath.Weather.CLEAR;
        float[] floors = {0.1F, 0.2F, 0.3F, 0.4F, 0.5F};
        for (int tier = 1; tier <= 5; tier++) {
            double three = GeneratorMath.lunar(tier, 180F, 3, true, clear, 1F, 0);
            double one = GeneratorMath.lunar(tier, 180F, 1, true, clear, 1F, 0);
            assertEquals(1F - 0.25F * (1F - floors[tier - 1]), one, 1e-6, "phase 1 tier " + tier);
            assertEquals(1F - 0.75F * (1F - floors[tier - 1]), three, 1e-6, "phase 3 tier " + tier);
            assertEquals(one, GeneratorMath.lunar(tier, 180F, 7, true, clear, 1F, 0), 1e-6, "phase 7 tier " + tier);
            assertEquals(three, GeneratorMath.lunar(tier, 180F, 5, true, clear, 1F, 0), 1e-6, "phase 5 tier " + tier);
            assertEquals(floors[tier - 1], GeneratorMath.lunar(tier, 180F, 4, true, clear, 1F, 0), 1e-6, "new moon tier " + tier);
        }
    }

    /** A block light outside 0..15 counts as the nearest end of the scale. */
    @Test
    void lightPollutionClamps() {
        var clear = GeneratorMath.Weather.CLEAR;
        assertEquals(GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, 0), GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, -7), 1e-9);
        assertEquals(GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, 15), GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, 40), 1e-9);
        assertTrue(GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, 7) > GeneratorMath.lunar(1, 180F, 0, true, clear, 1F, 8));
    }

    /** Lunar panels make nothing by day and in the rain at a new moon still keep the floor times the weather share. */
    @Test
    void lunarDayAndWeather() {
        assertEquals(0, GeneratorMath.lunar(5, 0F, 0, true, GeneratorMath.Weather.CLEAR, 1F, 0));
        assertEquals(0.1F * 0.2F, GeneratorMath.lunar(1, 180F, 4, true, GeneratorMath.Weather.RAIN, 1F, 0), 1e-6);
        assertEquals(0.1F * 0.2F * 0.5F, GeneratorMath.lunar(1, 180F, 4, true, GeneratorMath.Weather.THUNDER, 1F, 0), 1e-6);
    }

    /** A waterlogged panel keeps 20 % of its output. */
    @Test
    void waterloggedLoses80Percent() {
        assertEquals(1F, GeneratorMath.waterlogged(false));
        assertEquals(0.2F, GeneratorMath.waterlogged(true), 1e-6);
    }

    /** Block light above a lunar panel halves the output at most. */
    @Test
    void lightPollution() {
        double dark = GeneratorMath.lunar(1, 180F, 0, true, GeneratorMath.Weather.CLEAR, 1F, 0);
        assertEquals(dark / 2, GeneratorMath.lunar(1, 180F, 0, true, GeneratorMath.Weather.CLEAR, 1F, 15), 1e-6);
    }

    /** A panel fills the battery of its tier in well under one MC day, but not in minutes: the daily yield of the default peak is 2..15 times its capacity; no tier peaks below 20 FE/t. */
    @Test
    void dailyYieldMatchesBattery() {
        var batteries = new EnergyCommonConfig.Battery[] {EnergyCommonConfig.BATTERY_TIER_1, EnergyCommonConfig.BATTERY_TIER_2,
                EnergyCommonConfig.BATTERY_TIER_3, EnergyCommonConfig.BATTERY_TIER_4, EnergyCommonConfig.BATTERY_TIER_5};
        for (int tier = 1; tier <= 5; tier++) {
            double solar = EnergyCommonConfig.SOLAR_PANELS.get(tier - 1).peak() * dailyShare(true, tier, 0) * 24000;
            double lunarSum = 0;
            for (int phase = 0; phase < 8; phase++) lunarSum += dailyShare(false, tier, phase);
            double lunar = EnergyCommonConfig.LUNAR_PANELS.get(tier - 1).peak() * lunarSum / 8 * 24000;
            double capacity = batteries[tier - 1].capacity();
            assertTrue(solar / capacity > 2 && solar / capacity < 15, "solar tier " + tier + ": " + solar / capacity);
            assertTrue(lunar / capacity > 2 && lunar / capacity < 15, "lunar tier " + tier + ": " + lunar / capacity);
        }
    }

    /** No default peak is below 20 FE/t. */
    @Test
    void minimumPeak() {
        for (var g : EnergyCommonConfig.SOLAR_PANELS) assertTrue(g.peak() >= 20);
        for (var g : EnergyCommonConfig.LUNAR_PANELS) assertTrue(g.peak() >= 20);
    }

    /** The defaults: five tiers each, buffer and output rate derived from the peak, peaks rising. */
    @Test
    void configDefaults() {
        for (var list : java.util.List.of(EnergyCommonConfig.SOLAR_PANELS, EnergyCommonConfig.LUNAR_PANELS)) {
            assertEquals(5, list.size());
            for (int i = 0; i < 5; i++) {
                assertEquals(list.get(i).peak() * 600, list.get(i).capacity());
                assertEquals(list.get(i).peak() * 5, list.get(i).maxOutput());
                if (i > 0) assertTrue(list.get(i).peak() > list.get(i - 1).peak());
            }
        }
    }
}
