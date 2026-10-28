package net.samtrion.dotcraft.jade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link JadeMod}. */
class JadeModTest {
    /** The mod id must follow the {@code dotcraft<name>} convention. */
    @Test
    void modIdFollowsConvention() {
        assertTrue(JadeMod.MODID.matches("dotcraft[a-z]+"), JadeMod.MODID);
    }
}
