package net.samtrion.dotcraft.resources.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ToolTier}. */
class ToolTierTest {
    /** The order is strict: none, wood, stone, copper, gold, iron, diamond, netherite. */
    @Test
    void orderIsStrict() {
        List<ToolTier> expected = List.of(ToolTier.NONE, ToolTier.WOOD, ToolTier.STONE, ToolTier.COPPER, ToolTier.GOLD,
                ToolTier.IRON, ToolTier.DIAMOND, ToolTier.NETHERITE);
        assertEquals(expected, List.of(ToolTier.values()));
        for (int i = 0; i < expected.size(); i++) assertEquals(i, expected.get(i).rank());
    }

    /** Netherite is told apart from diamond: only netherite mines what needs netherite. */
    @Test
    void netheriteIsDistinctFromDiamond() {
        assertTrue(ToolTier.NETHERITE.canMine(ToolTier.NETHERITE));
        assertFalse(ToolTier.DIAMOND.canMine(ToolTier.NETHERITE));
        assertTrue(ToolTier.NETHERITE.canMine(ToolTier.DIAMOND));
    }

    /** Gold sits between copper and iron. */
    @Test
    void goldBetweenCopperAndIron() {
        assertTrue(ToolTier.GOLD.canMine(ToolTier.COPPER));
        assertFalse(ToolTier.GOLD.canMine(ToolTier.IRON));
        assertTrue(ToolTier.IRON.canMine(ToolTier.GOLD));
    }

    /** A block that needs a tier is off limits for exactly the materials below it. */
    @Test
    void insufficientListsTheMaterialsBelow() {
        assertEquals(List.of(), ToolTier.NONE.insufficient().toList());
        assertEquals(List.of(), ToolTier.WOOD.insufficient().toList());
        assertEquals(List.of(ToolTier.WOOD, ToolTier.STONE, ToolTier.COPPER, ToolTier.GOLD, ToolTier.IRON, ToolTier.DIAMOND),
                ToolTier.NETHERITE.insufficient().toList());
    }

    /** Every material has its own needs tag; stone, iron and diamond keep vanilla's, wood, gold and netherite use NeoForge's. */
    @Test
    void needsTags() {
        assertNull(ToolTier.NONE.needsTag());
        assertEquals("neoforge:needs_wood_tool", ToolTier.WOOD.needsTag().location().toString());
        assertEquals("neoforge:needs_gold_tool", ToolTier.GOLD.needsTag().location().toString());
        assertEquals("neoforge:needs_netherite_tool", ToolTier.NETHERITE.needsTag().location().toString());
        assertEquals("dotcraft_resources:needs_copper_tool", ToolTier.COPPER.needsTag().location().toString());
        assertEquals("minecraft:needs_iron_tool", ToolTier.IRON.needsTag().location().toString());
        assertEquals(ToolTier.materials().count(), ToolTier.materials().map(ToolTier::needsTag).distinct().count());
    }

    /** Only gold contradicts vanilla: it blocks stone blocks although gold ranks above stone. */
    @Test
    void onlyGoldContradictsVanilla() {
        for (ToolTier material : ToolTier.materials().toList()) {
            List<ToolTier> expected = material == ToolTier.GOLD ? List.of(ToolTier.STONE) : List.of();
            assertEquals(expected, material.contradictions().toList(), material.name());
        }
    }

    /** Tiers 1-3 keep the base requirement, 4-6 climb one material, 7-9 two; gold is skipped (wood: wood, stone, copper). */
    @Test
    void progressedClimbsEveryThreeTiers() {
        List<ToolTier> expected = List.of(ToolTier.WOOD, ToolTier.WOOD, ToolTier.WOOD, ToolTier.STONE, ToolTier.STONE,
                ToolTier.STONE, ToolTier.COPPER, ToolTier.COPPER, ToolTier.COPPER);
        for (int tier = 1; tier <= 9; tier++) assertEquals(expected.get(tier - 1), ToolTier.WOOD.progressed(tier), "tier " + tier);
    }

    /** Blocks without a requirement stay free, the top is capped at netherite and gold climbs from copper. */
    @Test
    void progressedEdges() {
        for (int tier = 1; tier <= 9; tier++) assertEquals(ToolTier.NONE, ToolTier.NONE.progressed(tier));
        assertEquals(ToolTier.GOLD, ToolTier.GOLD.progressed(3));
        assertEquals(ToolTier.IRON, ToolTier.GOLD.progressed(4));
        assertEquals(ToolTier.NETHERITE, ToolTier.DIAMOND.progressed(4));
        assertEquals(ToolTier.NETHERITE, ToolTier.NETHERITE.progressed(9));
    }
}
