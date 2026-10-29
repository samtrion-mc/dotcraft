package net.samtrion.dotcraft.energy.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the weights, caps, diminishing returns and the economy of the mob wheels. */
class WheelMathTest {
    private static double[] mobs(int count, double weight) {
        double[] weights = new double[count];
        Arrays.fill(weights, weight);
        return weights;
    }

    /** A full cage of normal mobs is the peak, whatever the tier. */
    @Test
    void bestCaseIsThePeak() {
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            assertEquals(1.0, WheelMath.load(mobs(WheelMath.cap(tier), 1), tier), 1e-9, "tier " + tier);
        }
    }

    /** Mobs beyond the cap add nothing and no crowd goes past the peak. */
    @Test
    void capBoundsTheOutput() {
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            int cap = WheelMath.cap(tier);
            assertEquals(WheelMath.effective(mobs(cap, 1), tier), WheelMath.effective(mobs(cap + 50, 1), tier), 1e-9, "tier " + tier);
            assertEquals(1.0, WheelMath.load(mobs(1000, 2), tier), 1e-9);
        }
    }

    /** Every further mob adds less than the one before, and never less than nothing. */
    @Test
    void diminishingAndMonotone() {
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            double previous = 0;
            double previousStep = Double.MAX_VALUE;
            for (int n = 1; n <= WheelMath.cap(tier); n++) {
                double now = WheelMath.effective(mobs(n, 1), tier);
                double step = now - previous;
                assertTrue(step > 0, "adding a mob must help, tier " + tier);
                assertTrue(step < previousStep + 1e-12, "returns must diminish, tier " + tier);
                previous = now;
                previousStep = step;
            }
        }
    }

    /** The order of the weights does not matter: the strongest count first. */
    @Test
    void strongestFirst() {
        assertEquals(WheelMath.effective(new double[] {2, 1, 0.5}, 3), WheelMath.effective(new double[] {0.5, 2, 1}, 3), 1e-9);
        assertEquals(2 + 0.75, WheelMath.effective(new double[] {1, 2}, 1), 1e-9);
        // cap of tier 1 is 2: the weakest one is left out
        assertEquals(2 + 0.75, WheelMath.effective(new double[] {1, 2, 0.5}, 1), 1e-9);
    }

    /** A horse makes more than a pig, and a small animal less; from tier 3 on small animals pull their full weight. */
    @Test
    void strengths() {
        double pig = WheelMath.load(new double[] {WheelMath.weight(WheelMath.Strength.NORMAL, 1)}, 1);
        double horse = WheelMath.load(new double[] {WheelMath.weight(WheelMath.Strength.STRONG, 1)}, 1);
        double rabbit = WheelMath.load(new double[] {WheelMath.weight(WheelMath.Strength.WEAK, 1)}, 1);
        assertTrue(horse > pig && pig > rabbit);
        assertEquals(0.5, WheelMath.weight(WheelMath.Strength.WEAK, 2));
        assertEquals(1.0, WheelMath.weight(WheelMath.Strength.WEAK, 3));
        assertEquals(0, WheelMath.weight(WheelMath.Strength.NONE, 5));
    }

    /** The perk ladder: more mobs per tier, softer returns from tier 4 on. */
    @Test
    void tiersGrow() {
        for (int tier = 2; tier <= WheelMath.MAX_TIER; tier++) {
            assertTrue(WheelMath.cap(tier) > WheelMath.cap(tier - 1));
            assertTrue(WheelMath.decay(tier) >= WheelMath.decay(tier - 1));
        }
        assertTrue(WheelMath.decay(4) > WheelMath.decay(3));
    }

    /** An animal below half of its health rests, above it the health counts. */
    @Test
    void welfare() {
        assertEquals(1.0, WheelMath.welfare(10, 10));
        assertEquals(0.75, WheelMath.welfare(7.5, 10));
        assertEquals(0.5, WheelMath.welfare(5, 10));
        assertEquals(0.0, WheelMath.welfare(4.9, 10));
        assertEquals(0.0, WheelMath.welfare(1, 0));
        assertEquals(1.0, WheelMath.welfare(12, 10));
    }

    /** A mob that stands or pottered about does not run, one that covered half a block does. */
    @Test
    void movement() {
        assertTrue(!WheelMath.moved(0, 0));
        assertTrue(!WheelMath.moved(0.3, 0.3));
        assertTrue(WheelMath.moved(0.5, 0));
        assertTrue(WheelMath.moved(-0.4, 0.4));
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

    /** The peaks follow the battery ladder and start at 15 FE/t. */
    @Test
    void peaksFollowTheBatteryLadder() {
        int[] peaks = {15, 75, 560, 5_600, 56_000};
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
        int[] capacities = {EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(),
                EnergyCommonConfig.BATTERY_TIER_3.capacity(), EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            double peak = EnergyCommonConfig.MOB_WHEELS.get(tier - 1).peak();
            double daily = WheelMath.dailyYield(peak, WheelMath.REFERENCE_DUTY);
            assertTrue(daily >= capacities[tier - 1] * 4, "tier " + tier + " makes " + daily + " FE a day for a battery of " + capacities[tier - 1]);
            double fill = WheelMath.fillTicks(capacities[tier - 1], peak, WheelMath.REFERENCE_DUTY);
            assertTrue(fill < WheelMath.DAY_TICKS / 4.0, "tier " + tier + " needs " + fill + " ticks");
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
