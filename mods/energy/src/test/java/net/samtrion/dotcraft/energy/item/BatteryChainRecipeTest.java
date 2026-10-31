package net.samtrion.dotcraft.energy.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Pins the upgrade chain of the batteries in the generated data: every tier from 2 to 5 is crafted from the battery of the tier
 * below, and every other ingredient is an item tag (never a raw item id).
 */
class BatteryChainRecipeTest {
    private static JsonObject recipe(int tier) {
        var in = BatteryChainRecipeTest.class.getResourceAsStream("/data/dotcraftenergy/recipe/items/battery_tier_" + tier + ".json");
        assertNotNull(in, "battery_tier_" + tier);
        return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** Each recipe yields an empty battery of its own tier, is switched by that tier and consumes the previous tier twice. */
    @Test
    void eachTierUpgradesThePreviousOne() {
        for (int tier = 2; tier <= 5; tier++) {
            var r = recipe(tier);
            var id = "dotcraftenergy:battery_tier_" + tier;
            assertEquals(id, r.getAsJsonObject("result").get("id").getAsString());
            assertEquals(0, r.getAsJsonObject("result").getAsJsonObject("components").get("dotcraftenergy:energy").getAsInt());
            assertEquals(id, r.getAsJsonArray("neoforge:conditions").get(0).getAsJsonObject().get("value").getAsString());
            var pattern = String.join("", new String[] {r.getAsJsonArray("pattern").get(0).getAsString(),
                    r.getAsJsonArray("pattern").get(1).getAsString(), r.getAsJsonArray("pattern").get(2).getAsString()});
            assertEquals(2, pattern.chars().filter(c -> c == 'B').count(), "tier " + tier);
            assertEquals("dotcraftenergy:battery_tier_" + (tier - 1), r.getAsJsonObject("key").get("B").getAsString());
        }
    }

    /** Apart from the previous battery, every ingredient is a tag, so another mod's metals can stand in. */
    @Test
    void otherIngredientsAreTags() {
        for (int tier = 2; tier <= 5; tier++)
            for (var e : recipe(tier).getAsJsonObject("key").entrySet())
                if (!e.getKey().equals("B")) assertTrue(e.getValue().getAsString().startsWith("#"), "tier " + tier + " " + e.getKey());
    }
}
