package net.samtrion.dotcraft.energy.vibration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Unit tests for the hard cap of the vibration generators. */
class VibrationBucketTest {
    private static final long PER_TICK = 15;
    private static final long CAPACITY = 300;

    /** A new bucket is full, so the first events of a new generator are answered; it never holds more than the capacity. */
    @Test
    void startsFull() {
        var bucket = new VibrationBucket();
        assertEquals(120, bucket.take(1000, 120, PER_TICK, CAPACITY));
        assertEquals(120, bucket.take(1000, 120, PER_TICK, CAPACITY));
        assertEquals(60, bucket.take(1000, 120, PER_TICK, CAPACITY));
        assertEquals(0, bucket.take(1000, 120, PER_TICK, CAPACITY));
    }

    /** The bucket refills with the rate per tick, up to its capacity, and grants no more than it holds. */
    @Test
    void refillsAndGrantsPartly() {
        var bucket = new VibrationBucket(0, -1);
        bucket.take(0, 0, PER_TICK, CAPACITY);
        assertEquals(60, bucket.take(4, 100, PER_TICK, CAPACITY));
        assertEquals(0, bucket.take(4, 100, PER_TICK, CAPACITY));
        assertEquals(15, bucket.take(5, 100, PER_TICK, CAPACITY));
        assertEquals(CAPACITY, bucket.take(10_000, 10_000, PER_TICK, CAPACITY));
    }

    /** Many events in one tick share what the bucket holds. */
    @Test
    void burstIsLimitedToCapacity() {
        var bucket = new VibrationBucket();
        bucket.take(0, 0, PER_TICK, CAPACITY);
        long sum = 0;
        for (int i = 0; i < 100; i++) sum += bucket.take(500, 120, PER_TICK, CAPACITY);
        assertEquals(CAPACITY, sum);
    }

    /** A rate of 0 (a generator switched off by config) never gives anything. */
    @Test
    void zeroRate() {
        var bucket = new VibrationBucket();
        bucket.take(0, 0, 0, 0);
        assertEquals(0, bucket.take(100_000, 1000, 0, 0));
    }

    /** A clock that went backwards refills nothing and restarts. */
    @Test
    void clockGoingBackwards() {
        var bucket = new VibrationBucket(0, 5000);
        assertEquals(0, bucket.take(100, 50, PER_TICK, CAPACITY));
        assertEquals(15, bucket.take(101, 50, PER_TICK, CAPACITY));
    }

    /** A saved bucket restores; a lowered capacity or a negative value in a damaged save cannot make FE. */
    @Test
    void restoreAndShrink() {
        assertEquals(100, new VibrationBucket(100, 10).take(10, 1000, PER_TICK, CAPACITY));
        assertEquals(50, new VibrationBucket(300, 10).take(10, 1000, PER_TICK, 50));
        assertEquals(0, new VibrationBucket(-5, 10).take(10, 1000, PER_TICK, CAPACITY));
    }

    /** A huge gap or rate does not overflow into a bigger bucket. */
    @Test
    void noOverflow() {
        var bucket = new VibrationBucket(0, 0);
        assertEquals(CAPACITY, bucket.take(Long.MAX_VALUE / 2, Long.MAX_VALUE, Long.MAX_VALUE / 4, CAPACITY));
    }

    /**
     * The ceiling is provable: whatever the pattern of events, a whole day and every tick hammered with the loudest event never
     * gives more than {@link VibrationMath#maxYield}, and a random pattern never exceeds it either.
     */
    @Test
    void ceilingOverADay() {
        for (int tier = 1; tier <= 5; tier++) {
            int peak = net.samtrion.dotcraft.energy.EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            long refill = VibrationMath.refill(peak);
            var hammer = new VibrationBucket();
            var random = new VibrationBucket();
            var rnd = new Random(tier);
            hammer.take(0, 0, refill, VibrationMath.burst(peak));
            random.take(0, 0, refill, VibrationMath.burst(peak));
            long hammered = 0;
            long randomly = 0;
            for (int tick = 1; tick <= VibrationMath.DAY_TICKS; tick++) {
                for (int i = 0; i < 20; i++) hammered += hammer.take(tick, VibrationMath.eventEnergy(peak, 15), refill, VibrationMath.burst(peak));
                if (rnd.nextInt(3) == 0) {
                    for (int i = rnd.nextInt(30); i > 0; i--) randomly += random.take(tick, VibrationMath.eventEnergy(peak, 1 + rnd.nextInt(15)), refill, VibrationMath.burst(peak));
                }
            }
            long max = VibrationMath.maxYield(peak, VibrationMath.DAY_TICKS);
            assertTrue(hammered <= max, "tier " + tier + ": " + hammered + " > " + max);
            assertTrue(randomly <= max, "tier " + tier + ": " + randomly + " > " + max);
            // the hammer reaches the ceiling (within one loudest event per tick), so the cap is the real limit
            assertTrue(hammered >= refill * VibrationMath.DAY_TICKS - VibrationMath.eventEnergy(peak, 15), "tier " + tier + ": " + hammered);
        }
    }
}
