package net.samtrion.dotcraft.rei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ReiMod}. */
class ReiModTest {
    /** The mod id must follow the {@code dotcraft<name>} convention. */
    @Test
    void modIdFollowsConvention() {
        assertTrue(ReiMod.MODID.matches("dotcraft[a-z]+"), ReiMod.MODID);
    }
}
