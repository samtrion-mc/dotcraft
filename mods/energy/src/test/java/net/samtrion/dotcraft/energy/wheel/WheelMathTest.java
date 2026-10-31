package net.samtrion.dotcraft.energy.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the weight, the share of the peak and the economy of the mob wheels (one mob per wheel). */
class WheelMathTest {
    /** A horse makes more than a pig and a pig more than a rabbit; a zombie is derived like any other mob. */
    @Test
    void weights() {
        double pig = WheelMath.weight(10, 0.25, 1);
        double horse = WheelMath.weight(22.5, 0.225, 1);
        double rabbit = WheelMath.weight(3, 0.3, 1);
        double chicken = WheelMath.weight(4, 0.25, 1);
        double zombie = WheelMath.weight(20, 0.23, 1);
        assertEquals(1.0, pig, 1e-9);
        assertTrue(horse > 1.4 && horse < 1.6, "horse " + horse);
        assertTrue(rabbit > 0.5 && rabbit < 0.65, "rabbit " + rabbit);
        assertTrue(chicken < 0.7 && chicken > rabbit, "chicken " + chicken);
        assertTrue(zombie > 1.3 && zombie < 1.5, "zombie " + zombie);
        assertTrue(WheelMath.share(horse, true) > WheelMath.share(pig, true) && WheelMath.share(pig, true) > WheelMath.share(rabbit, true));
        // light mobs pull a full weight from tier 3 on, the heavy ones are not touched
        assertEquals(rabbit, WheelMath.weight(3, 0.3, 2), 1e-9);
        assertEquals(1.0, WheelMath.weight(3, 0.3, 3), 1e-9);
        assertEquals(horse, WheelMath.weight(22.5, 0.225, 5), 1e-9);
    }

    /** The weight is clamped (a boss is at most twice a pig, a mouse still counts) and survives degenerate attributes. */
    @Test
    void weightIsClamped() {
        assertEquals(WheelMath.MAX_WEIGHT, WheelMath.weight(500, 0.3, 1), 1e-9);
        assertEquals(WheelMath.MIN_WEIGHT, WheelMath.weight(0.5, 0.2, 1), 1e-9);
        assertEquals(WheelMath.MIN_WEIGHT, WheelMath.weight(0, 0, 1), 1e-9);
        assertTrue(WheelMath.weight(10, 0, 1) > 0);
        assertTrue(WheelMath.weight(10, 0.5, 1) > WheelMath.weight(10, 0.1, 1), "faster is heavier");
    }

    /** The share of the peak is {@code min(1, weight * activity / FULL_WEIGHT)}: a strong running mob reaches the peak, nothing goes past it. */
    @Test
    void shareOfThePeak() {
        assertEquals(1.0, WheelMath.share(WheelMath.FULL_WEIGHT, true), 1e-9);
        assertEquals(1.0, WheelMath.share(WheelMath.MAX_WEIGHT, true), 1e-9);
        assertEquals(1.0 / WheelMath.FULL_WEIGHT, WheelMath.share(1, true), 1e-9);
        assertEquals(WheelMath.STANDING_FACTOR / WheelMath.FULL_WEIGHT, WheelMath.share(1, false), 1e-9);
        assertEquals(0, WheelMath.share(-1, true));
        // a running horse or zombie makes (nearly) the whole peak
        assertEquals(1.0, WheelMath.share(WheelMath.weight(22.5, 0.225, 1), true), 0.05);
        assertTrue(WheelMath.share(WheelMath.weight(20, 0.23, 1), true) > 0.9);
    }

    /** A mob that tries to move counts fully, one that merely stands half. */
    @Test
    void activity() {
        assertEquals(1.0, WheelMath.activity(true));
        assertEquals(WheelMath.STANDING_FACTOR, WheelMath.activity(false));
    }

    /** Only tier 5 has the flywheel: it halves instead of stopping, and gives up below 1 FE/t. */
    @Test
    void flywheel() {
        assertEquals(0, WheelMath.coast(0, 15, 1));
        assertEquals(0, WheelMath.coast(0, 28_000, 4));
        assertEquals(28_000, WheelMath.coast(0, 56_000, 5));
        assertEquals(56_000, WheelMath.coast(56_000, 1, 5));
        assertEquals(0, WheelMath.coast(0, 1.5, 5));
    }

    /** The peaks follow the battery ladder and start at 30 FE/t. */
    @Test
    void peaksFollowTheBatteryLadder() {
        int[] peaks = {20, 75, 560, 5_600, 56_000};
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            assertEquals(peaks[tier - 1], EnergyCommonConfig.MOB_WHEELS.get(tier - 1).peak(), "tier " + tier);
        }
    }

    /**
     * The economy: at the reference duty cycle (a quarter of the day at full load) a wheel makes more than the battery of its tier
     * holds, and fills it in under a quarter of an MC day.
     */
    @Test
    void dailyYieldFillsTheBatteryOfTheTier() {
        int[] capacities = {EnergyCommonConfig.BATTERY_TIER_1.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
                EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.MOB_WHEELS.get(tier - 1).peak();
            double daily = WheelMath.dailyYield(peak, WheelMath.REFERENCE_DUTY);
            assertTrue(daily >= capacities[tier - 1] * (tier == 1 ? 3 : 4), "tier " + tier + " makes " + daily + " FE a day for a battery of " + capacities[tier - 1]);
            double fill = WheelMath.fillTicks(capacities[tier - 1], peak, WheelMath.REFERENCE_DUTY);
            assertTrue(fill <= WheelMath.DAY_TICKS / (tier == 1 ? 3.0 : 4.0), "tier " + tier + " needs " + fill + " ticks");
        }
        assertEquals(15 * 24_000 / 4.0, WheelMath.dailyYield(15, WheelMath.REFERENCE_DUTY));
    }

    /** The coast of the flywheel adds about one more scan of the peak after a burst, and nothing below tier 5. */
    @Test
    void coastAddsAboutOneScanOfYield() {
        double peak = 56_000;
        double scan = peak * WheelMath.SCAN_TICKS;
        double extra = WheelMath.coastYield(peak, 5);
        assertTrue(extra > 0.9 * scan && extra <= scan, "the coast makes " + extra + " FE for a scan of " + scan);
        assertEquals(0, WheelMath.coastYield(peak, 4));
    }
}
