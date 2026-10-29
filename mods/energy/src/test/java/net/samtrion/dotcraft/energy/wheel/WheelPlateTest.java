package net.samtrion.dotcraft.energy.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests of the running surface and the activity factor of the mob wheels. */
class WheelPlateTest {
    /** A mob on the middle of the plate counts, one beside the block or on the discs does not. */
    @Test
    void footprint() {
        assertTrue(WheelPlate.carries(false, 0.5, WheelPlate.TOP, 0.5));
        assertTrue(WheelPlate.carries(false, 3 / 16.0, WheelPlate.TOP, 14 / 16.0));
        assertFalse(WheelPlate.carries(false, 2 / 16.0, WheelPlate.TOP, 0.5), "beside the tread");
        assertFalse(WheelPlate.carries(false, 0.5, WheelPlate.TOP, 1 / 16.0), "on the disc");
        assertFalse(WheelPlate.carries(false, 3.5, WheelPlate.TOP, 0.5), "three blocks away");
    }

    /** A wheel turned east or west has the footprint turned with it. */
    @Test
    void turned() {
        assertTrue(WheelPlate.carries(true, 2 / 16.0, WheelPlate.TOP, 0.5));
        assertFalse(WheelPlate.carries(true, 0.5, WheelPlate.TOP, 2 / 16.0));
        assertFalse(WheelPlate.carries(false, 2 / 16.0, WheelPlate.TOP, 0.5));
    }

    /** The feet must be on the plate: a mob on the floor beside it or one on a block above does not stand on it. */
    @Test
    void height() {
        assertTrue(WheelPlate.carries(false, 0.5, WheelPlate.TOP + 0.04, 0.5));
        assertFalse(WheelPlate.carries(false, 0.5, 1.0 + 1.0 / 16, 0.5));
        assertFalse(WheelPlate.carries(false, 0.5, 0.0, 0.5));
    }

    /** A running mob counts fully, a standing one half. */
    @Test
    void activity() {
        assertEquals(1.0, WheelMath.activity(true));
        assertEquals(WheelMath.STANDING_FACTOR, WheelMath.activity(false));
        assertTrue(WheelMath.STANDING_FACTOR > 0 && WheelMath.STANDING_FACTOR < 1);
    }
}
