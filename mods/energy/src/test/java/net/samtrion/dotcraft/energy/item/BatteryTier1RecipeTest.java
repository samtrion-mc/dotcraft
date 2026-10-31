package net.samtrion.dotcraft.energy.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Pins the two recipes of the tier 1 battery in the generated data: {@code battery_tier_1} (two rechargeable potato batteries) and
 * {@code battery_tier_1_direct} (no potato battery). Cost unit: 1 nugget or redstone grain = 1, 1 ingot or redstone dust = 9.
 * A raw: 2 x (grain + copper nugget + zinc nugget) + 3 grains + 2 copper + 2 zinc ingots = 45 (and two poisonous potatoes).
 * B raw: 4 copper + 4 zinc ingots + 1 redstone dust = 81, so B is 1.8 times A.
 */
class BatteryTier1RecipeTest {
    private static final Map<String, Integer> UNITS = Map.of("#c:ingots/copper", 9, "#c:ingots/zinc", 9, "#c:dusts/redstone", 9,
            "#c:small_dusts/redstone", 1, "#c:nuggets/copper", 1, "#c:nuggets/zinc", 1);

    private static JsonObject recipe(String id) {
        var in = BatteryTier1RecipeTest.class.getResourceAsStream("/data/dotcraftenergy/recipe/items/" + id + ".json");
        assertNotNull(in, id);
        return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** The raw cost of a shaped recipe, a rechargeable potato battery counted as given. */
    private static int cost(JsonObject shaped, int potatoBatteryCost) {
        var key = shaped.getAsJsonObject("key");
        int total = 0;
        for (var row : shaped.getAsJsonArray("pattern"))
            for (char c : row.getAsString().toCharArray()) {
                if (c == ' ') continue;
                var ing = key.get(String.valueOf(c)).getAsString();
                total += ing.equals("dotcraftenergy:rechargeable_potato_battery") ? potatoBatteryCost : UNITS.get(ing);
            }
        return total;
    }

    /** Both recipes exist, share the startup switch and the result; B has no potato battery and costs at least 1.5 times A. */
    @Test
    void directRecipeIsMoreExpensive() {
        var a = recipe("battery_tier_1");
        var b = recipe("battery_tier_1_direct");
        assertEquals(a.get("neoforge:conditions"), b.get("neoforge:conditions"));
        assertEquals(a.get("result"), b.get("result"));
        assertFalse(b.getAsJsonObject("key").toString().contains("potato"));
        int potatoBattery = 0; // grain + copper nugget + zinc nugget (the poisonous potato is no metal and not counted)
        for (var i : recipe("rechargeable_potato_battery").getAsJsonArray("ingredients"))
            if (UNITS.containsKey(i.getAsString())) potatoBattery += UNITS.get(i.getAsString());
        assertEquals(3, potatoBattery);
        int costA = cost(a, potatoBattery);
        int costB = cost(b, potatoBattery);
        assertEquals(45, costA);
        assertEquals(81, costB);
        assertTrue(costB * 2 >= costA * 3, costB + " vs " + costA);
    }
}
