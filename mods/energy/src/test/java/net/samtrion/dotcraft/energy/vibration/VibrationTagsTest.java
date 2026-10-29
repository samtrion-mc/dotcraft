package net.samtrion.dotcraft.energy.vibration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.samtrion.dotcraft.energy.EnergyMod;

/** The curated game event tag is a data file: it holds the shocks that need work and none that is cheap to repeat. */
class VibrationTagsTest {
    private static Set<String> values() throws IOException {
        var path = "/data/" + EnergyMod.MODID + "/tags/game_event/vibration_sources.json";
        try (var in = VibrationTagsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no tag file");
            var values = new HashSet<String>();
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("values")
                    .forEach(e -> values.add(e.getAsString()));
            return values;
        }
    }

    /** The shocks that count are in the tag, with no duplicates. */
    @Test
    void holdsTheShocks() throws IOException {
        var values = values();
        for (String event : new String[] {"block_destroy", "block_place", "explode", "entity_die", "entity_damage", "lightning_strike"}) {
            assertTrue(values.contains("minecraft:" + event), event);
        }
        assertEquals(8, values.size());
    }

    /** What is cheap to repeat is not in the tag (what a machine can still loop is capped by the bucket, see VibrationMathTest): containers, doors and levers, note blocks, pistons, mounts, equipment, fluids, teleports, shots. */
    @Test
    void leavesOutLoopableEvents() throws IOException {
        var values = values();
        for (String event : new String[] {"container_open", "container_close", "block_open", "block_close", "block_activate", "block_deactivate",
                "block_attach", "block_detach", "block_change", "note_block_play", "instrument_play", "entity_mount", "entity_dismount",
                "entity_interact", "entity_place", "equip", "unequip", "shear", "eat", "drink", "fluid_place", "fluid_pickup", "teleport",
                "projectile_shoot", "projectile_land", "swim", "splash", "flap", "prime_fuse", "jukebox_play", "shriek", "sculk_sensor_tendrils_clicking"}) {
            assertFalse(values.contains("minecraft:" + event), event + " must not be a source");
        }
        for (int i = 1; i <= 15; i++) assertFalse(values.contains("minecraft:resonate_" + i), "resonance of sculk sensors is no source");
    }
}
