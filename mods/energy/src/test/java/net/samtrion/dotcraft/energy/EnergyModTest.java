package net.samtrion.dotcraft.energy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link EnergyMod}. */
class EnergyModTest {
    /** The mod id must follow the {@code dotcraft_<name>} convention used by the scaffold. */
    @Test
    void modIdFollowsConvention() {
        assertTrue(EnergyMod.MODID.matches("dotcraft_[a-z0-9_]+"), EnergyMod.MODID);
    }
}
