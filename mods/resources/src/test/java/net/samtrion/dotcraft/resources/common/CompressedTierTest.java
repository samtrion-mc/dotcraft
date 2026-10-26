package net.samtrion.dotcraft.resources.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link CompressedTier}. */
class CompressedTierTest {
    /** Each tier holds nine times the previous one. */
    @Test
    void countGrowsByFactorNine() {
        assertEquals(9L, CompressedTier.count(1));
        assertEquals(81L, CompressedTier.count(2));
        assertEquals(387_420_489L, CompressedTier.count(CompressedTier.MAX));
    }

    /** Hardness scales linearly with the tier. */
    @Test
    void hardnessScalesWithTier() {
        assertEquals(3f, CompressedTier.hardness(1.5f, 1));
        assertEquals(15f, CompressedTier.hardness(1.5f, 9));
    }

    /** Burn time grows by the block count; what exceeds an int is no fuel. */
    @Test
    void burnTimeScalesAndCaps() {
        assertEquals(144_000, CompressedTier.burnTime(16_000, 1));
        assertEquals(16_000 * 59_049, CompressedTier.burnTime(16_000, 5));
        assertEquals(0, CompressedTier.burnTime(16_000, 6));
        assertEquals(0, CompressedTier.burnTime(Integer.MAX_VALUE, 1));
        assertEquals(0, CompressedTier.burnTime(0, 5));
    }

    /** Every tier has its overlay texture; a missing one would show as the purple missing texture. */
    @Test
    void overlayTexturesExist() {
        for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
            String path = "/assets/dotcraftresources/textures/block/compressed_overlay_" + tier + ".png";
            assertNotNull(getClass().getResource(path), path);
        }
    }

    /** Tiers outside {@code 1..MAX} are rejected. */
    @Test
    void rejectsInvalidTier() {
        assertThrows(IllegalArgumentException.class, () -> CompressedTier.count(0));
        assertThrows(IllegalArgumentException.class, () -> CompressedTier.hardness(1f, CompressedTier.MAX + 1));
        assertNotEquals(0, CompressedTier.MAX);
    }
}
