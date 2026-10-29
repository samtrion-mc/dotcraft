package net.samtrion.dotcraft.energy.current;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.samtrion.dotcraft.energy.EnergyMod;

/** Unit tests for the data files of the current generators: the tags and the block models. */
class CurrentDataTest {
    private static JsonObject read(String path) throws IOException {
        try (var in = CurrentDataTest.class.getResourceAsStream("/" + path)) {
            assertNotNull(in, "missing data file " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static HashSet<String> values(String path) throws IOException {
        var set = new HashSet<String>();
        read(path).getAsJsonArray("values").forEach(e -> set.add(e.getAsString()));
        return set;
    }

    /** Ocean and river are the prime biomes, through the vanilla tags, so modded oceans count too. */
    @Test
    void primeBiomesAreOceanAndRiver() throws IOException {
        var values = values("data/" + EnergyMod.MODID + "/tags/worldgen/biome/current_prime.json");
        assertEquals(java.util.Set.of("#minecraft:is_ocean", "#minecraft:is_river"), values);
    }

    /** The fauna tag lists the aquatic animals: fish, squids, axolotl, dolphin and more, but nothing hostile and no duplicates. */
    @Test
    void faunaTag() throws IOException {
        var path = "data/" + EnergyMod.MODID + "/tags/entity_type/current_fauna.json";
        var array = read(path).getAsJsonArray("values");
        var values = values(path);
        assertEquals(array.size(), values.size(), "duplicates in the fauna tag");
        for (String needed : new String[] {"minecraft:cod", "minecraft:salmon", "minecraft:pufferfish", "minecraft:tropical_fish", "minecraft:squid",
                "minecraft:axolotl", "minecraft:dolphin"}) {
            assertTrue(values.contains(needed), needed + " is missing");
        }
        assertTrue(values.size() >= CurrentMath.FAUNA_TYPES, "fewer types than the cap would ever use");
        assertTrue(!values.contains("minecraft:guardian") && !values.contains("minecraft:drowned"), "a hostile mob in the fauna tag");
    }

    /** Every tier has a block model that uses its own rotor and frame texture, and the textures exist. */
    @Test
    void everyTierHasModelAndTextures() throws IOException {
        for (int tier = 1; tier <= CurrentMath.MAX_TIER; tier++) {
            var name = "current_generator_tier_" + tier; // not CurrentBlocks.name: that class needs the Minecraft registries
            var model = read("assets/" + EnergyMod.MODID + "/models/block/" + name + ".json");
            assertEquals("dotcraftenergy:block/current_generator", model.get("parent").getAsString());
            for (String part : new String[] {"rotor", "frame"}) {
                var texture = model.getAsJsonObject("textures").get(part).getAsString();
                assertEquals("dotcraftenergy:block/" + name + "_" + part, texture);
                try (var in = CurrentDataTest.class.getResourceAsStream("/assets/" + EnergyMod.MODID + "/textures/block/" + name + "_" + part + ".png")) {
                    assertNotNull(in, "missing texture " + texture);
                }
            }
        }
    }
}
