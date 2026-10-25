package net.samtrion.dotcraft.resources;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link CraftResources}. */
class CraftResourcesTest {
    /** The mod id must follow the {@code dotcraft_<name>} convention used by the scaffold. */
    @Test
    void modIdFollowsConvention() {
        assertTrue(CraftResources.MODID.matches("dotcraft_[a-z0-9_]+"), CraftResources.MODID);
    }

    /** The shared logger is available to the other mods. */
    @Test
    void loggerIsAvailable() {
        assertNotNull(CraftResources.LOGGER);
    }
}
