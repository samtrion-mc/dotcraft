package net.samtrion.dotcraft.energy.vibration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the weights, the perks and the economy anchor of the vibration generators. */
class VibrationMathTest {
    private static final EnergyCommonConfig.Battery[] BATTERIES = {EnergyCommonConfig.BATTERY_TIER_1, EnergyCommonConfig.BATTERY_TIER_2,
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
            assertTrue(VibrationMath.minFrequency(tier) <= VibrationMath.minFrequency(tier - 1));
        }
        assertFalse(VibrationMath.pierces(3));
        assertTrue(VibrationMath.pierces(4));
        assertTrue(VibrationMath.pierces(5));
    }

    /** What a tier hears: only the loud events at tier 1, everything with a frequency at tier 5, never frequency 0. */
    @Test
    void hearing() {
        assertFalse(VibrationMath.hears(1, 7));
        assertTrue(VibrationMath.hears(1, 8));
        assertTrue(VibrationMath.hears(1, 15));
        assertFalse(VibrationMath.hears(4, 1));
        assertTrue(VibrationMath.hears(5, 1));
        for (int tier = 1; tier <= 5; tier++) {
            assertFalse(VibrationMath.hears(tier, 0));
            assertFalse(VibrationMath.hears(tier, 16));
            assertTrue(VibrationMath.hears(tier, 12), "a block break is heard by every tier");
        }
    }

    /**
     * How many events of the tag each tier hears, with the vanilla frequencies (step 1, hit_ground 2, entity_damage 7, block_destroy 12,
     * block_place 13, lightning_strike 14, entity_die 15, explode 15): tier 2 unlocks damage, tier 3 landings, tier 5 steps, and tier 4
     * adds the wool instead, as the class Javadoc says.
     */
    @Test
    void eventsPerTier() {
        int[] frequencies = {1, 2, 7, 12, 13, 14, 15, 15};
        int[] heard = {5, 6, 7, 7, 8};
        for (int tier = 1; tier <= 5; tier++) {
            int t = tier;
            assertEquals(heard[tier - 1], java.util.Arrays.stream(frequencies).filter(f -> VibrationMath.hears(t, f)).count(), "tier " + tier);
        }
    }

    /** The ceiling is the refill per tick plus one burst; the refill is the duty of the tier times the peak; both buckets refill with the peak. */
    @Test
    void ceiling() {
        assertEquals(300, VibrationMath.burst(15));
        assertEquals(3, VibrationMath.refill(1, 15));
        assertEquals(1, VibrationMath.refill(1, 1));
        assertEquals(0, VibrationMath.refill(1, 0));
        assertEquals(25, VibrationMath.refill(2, 75));
        assertEquals(249, VibrationMath.refill(3, 560));
        assertEquals(3_111, VibrationMath.refill(4, 5_600));
        assertEquals(37_333, VibrationMath.refill(5, 56_000));
        assertEquals(300 + 3 * 24000L, VibrationMath.maxYield(1, 15, 24000));
        assertEquals(VibrationMath.burst(75), VibrationMath.maxYield(2, 75, 0));
        for (int tier = 1; tier <= 5; tier++) {
            int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            assertEquals(peak, VibrationMath.refill(tier, peak) + VibrationMath.bonusRefill(tier, peak), "tier " + tier);
            assertTrue(tier == 1 || VibrationMath.duty(tier) > VibrationMath.duty(tier - 1), "the duty grows with the tier");
            assertTrue(VibrationMath.duty(tier) < 1);
            assertEquals(5 * VibrationMath.burst(peak), VibrationMath.bonusBurst(peak));
            assertTrue(VibrationMath.bossEventEnergy(peak, 15) <= VibrationMath.burst(peak) + VibrationMath.bonusBurst(peak), "a boss event fits both buckets");
        }
        assertEquals(5, VibrationMath.BOSS_FACTOR);
        assertEquals(750, VibrationMath.bossEventEnergy(15, 15));
        assertEquals(5 * VibrationMath.eventEnergy(75, 12), VibrationMath.bossEventEnergy(75, 12));
        assertEquals(300 + 1500 + 15 * 100L, VibrationMath.absoluteMaxYield(15, 100));
    }

    /** The FE a stream of one event every {@code interval} ticks makes over one MC day through the bucket of a peak. */
    private static long dayOf(int tier, int peak, int frequency, int interval) {
        var bucket = new VibrationBucket();
        long made = 0;
        for (int tick = 0; tick < VibrationMath.DAY_TICKS; tick += interval) {
            made += bucket.take(tick, VibrationMath.eventEnergy(peak, frequency), VibrationMath.refill(tier, peak), VibrationMath.burst(peak));
        }
        return made;
    }

    /**
     * The economy anchor: the peaks follow the battery ladder 20 / 75 / 560 / 5600 / 56000 FE/t as the burst power, none below 20. The
     * best typical case (a block broken every 10 ticks, which the bucket limits, or any loop that fires every tick, which it caps) makes
     * 3.6 / 6 / 8 / 10 / 12 batteries of the tier's own battery per MC day, pinned per tier within 0.5 of 4 / 6 / 8 / 10 / 12 and rising
     * smoothly. Light play (an event every 100 ticks) makes a good deal less, and damage dealt every 8 ticks sits at or below the ceiling.
     */
    @Test
    void dailyYieldMatchesBattery() {
        double[] targets = {2.4, 6, 8, 10, 12};
        double previous = 0;
        for (int tier = 1; tier <= 5; tier++) {
            int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            double capacity = BATTERIES[tier - 1].capacity();
            double ceiling = VibrationMath.dailyCeiling(tier, peak) / capacity;
            assertEquals(targets[tier - 1], ceiling, 0.5, "ceiling, tier " + tier + ": " + ceiling);
            assertTrue(ceiling > previous + 1.5, "the yield rises with the tier, tier " + tier + ": " + ceiling);
            previous = ceiling;
            assertTrue(VibrationMath.maxYield(tier, peak, VibrationMath.DAY_TICKS) / capacity < targets[tier - 1] + 0.5, "tier " + tier);
            double mining = dayOf(tier, peak, 12, 10) / capacity;
            double fighting = dayOf(tier, peak, 7, 8) / capacity;
            double light = dayOf(tier, peak, 12, 100) / capacity;
            double machine = dayOf(tier, peak, 15, 1) / capacity;
            assertEquals(targets[tier - 1], mining, 0.5, "mining, tier " + tier);
            assertEquals(ceiling, mining, 0.3, "mining, tier " + tier);
            assertTrue(fighting > 1 && fighting <= ceiling + 0.3, "fighting, tier " + tier + ": " + fighting);
            assertTrue(light > (tier == 1 ? 0.5 : 1) && light < ceiling / 2, "light play, tier " + tier + ": " + light);
            assertEquals(ceiling, machine, 0.3, "machine, tier " + tier);
            assertTrue(machine * capacity <= VibrationMath.maxYield(tier, peak, VibrationMath.DAY_TICKS), "machine, tier " + tier);
            assertTrue(capacity / peak >= VibrationMath.DAY_TICKS / 25.0, "tier " + tier);
            assertTrue(peak >= 20);
        }
        assertEquals(20, EnergyCommonConfig.VIBRATION_GENERATORS.get(0).peak());
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
