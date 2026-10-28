package net.samtrion.dotcraft.jei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link JeiMod}. */
class JeiModTest {
    /** The mod id must follow the {@code dotcraft<name>} convention used by the scaffold. */
    @Test
    void modIdFollowsConvention() {
        assertTrue(JeiMod.MODID.matches("dotcraft[a-z]+"), JeiMod.MODID);
    }
}
