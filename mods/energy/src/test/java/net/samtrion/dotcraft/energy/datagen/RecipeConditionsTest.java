package net.samtrion.dotcraft.energy.datagen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** A generated recipe must be dropped when an item of this mod it uses is switched off, else the reload has an unknown ingredient. */
class RecipeConditionsTest {
    private static final String PREFIX = "dotcraftenergy:";

    /** Collects the ids of this mod found as plain strings below {@code e}. */
    private static void ids(JsonElement e, Set<String> out) {
        if (e.isJsonObject()) e.getAsJsonObject().entrySet().forEach(x -> ids(x.getValue(), out));
        else if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> ids(x, out));
        else if (e.isJsonPrimitive() && e.getAsString().startsWith(PREFIX)) out.add(e.getAsString());
    }

    /** Every item of this mod in the ingredients of a battery tier recipe is also a registered condition of that recipe. */
    @Test
    void everyIngredientOfThisModIsGuardedByACondition() throws IOException {
        var dir = Path.of("src/generated/resources/data/dotcraftenergy/recipe/items");
        try (Stream<Path> files = Files.list(dir)) {
            var all = files.filter(f -> f.getFileName().toString().startsWith("battery_tier_")).toList();
            assertFalse(all.isEmpty());
            for (Path f : all) {
                JsonObject recipe = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                var used = new HashSet<String>();
                if (recipe.has("key")) ids(recipe.get("key"), used);
                if (recipe.has("ingredients")) ids(recipe.get("ingredients"), used);
                var guarded = new HashSet<String>();
                recipe.getAsJsonArray("neoforge:conditions").forEach(c -> guarded.add(c.getAsJsonObject().get("value").getAsString()));
                for (String id : used) assertTrue(guarded.contains(id), f.getFileName() + " uses " + id + " without a registered condition");
            }
        }
    }
}
