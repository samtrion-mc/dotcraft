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

    /** The ceiling is the refill per tick plus one burst. */
    @Test
    void ceiling() {
        assertEquals(300, VibrationMath.burst(15));
        assertEquals(4, VibrationMath.refill(15));
        assertEquals(1, VibrationMath.refill(1));
        assertEquals(0, VibrationMath.refill(0));
        assertEquals(300 + 4 * 24000L, VibrationMath.maxYield(15, 24000));
        assertEquals(VibrationMath.burst(75), VibrationMath.maxYield(75, 0));
    }

    /** The FE a stream of one event every {@code interval} ticks makes over one MC day through the bucket of a peak. */
    private static long dayOf(int peak, int frequency, int interval) {
        var bucket = new VibrationBucket();
        long made = 0;
        for (int tick = 0; tick < VibrationMath.DAY_TICKS; tick += interval) {
            made += bucket.take(tick, VibrationMath.eventEnergy(peak, frequency), VibrationMath.refill(peak), VibrationMath.burst(peak));
        }
        return made;
    }

    /**
     * The economy anchor: the peaks follow the battery ladder 15 / 75 / 560 / 5600 / 56000 FE/t, none below 15. The ceiling (a player
     * busy all day, or a machine looping an event) is below 6 batteries a day, the order of a panel (2..15 with the peak of its day
     * curve). Real event streams through the bucket: a block broken every 10 ticks or damage dealt every 8 ticks sits at the ceiling,
     * an event every 100 ticks makes less than half of it, and a machine that fires every tick cannot pass it.
     */
    @Test
    void dailyYieldMatchesBattery() {
        for (int tier = 1; tier <= 5; tier++) {
            int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            double capacity = BATTERIES[tier - 1].capacity();
            double ceiling = VibrationMath.dailyCeiling(peak) / capacity;
            assertTrue(ceiling > 4 && ceiling < 6, "tier " + tier + ": " + ceiling);
            assertTrue(VibrationMath.maxYield(peak, VibrationMath.DAY_TICKS) / capacity < 6, "tier " + tier);
            double mining = dayOf(peak, 12, 10) / capacity;
            double fighting = dayOf(peak, 7, 8) / capacity;
            double light = dayOf(peak, 12, 100) / capacity;
            double machine = dayOf(peak, 15, 1) / capacity;
            assertEquals(ceiling, mining, 0.3, "mining, tier " + tier);
            assertEquals(ceiling, fighting, 0.3, "fighting, tier " + tier);
            assertTrue(light > 1 && light < ceiling / 2, "light play, tier " + tier + ": " + light);
            assertEquals(ceiling, machine, 0.3, "machine, tier " + tier);
            assertTrue(machine * capacity <= VibrationMath.maxYield(peak, VibrationMath.DAY_TICKS), "machine, tier " + tier);
            assertTrue(capacity / peak >= VibrationMath.DAY_TICKS / 25.0, "tier " + tier);
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
