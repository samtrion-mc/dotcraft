package net.samtrion.dotcraft.resources.datagen.material;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;

/**
 * The generated material recipes use item tags as ingredients, never a direct item or block, and every tag they use
 * is defined: by our own generated tags, or as a convention tag of NeoForge.
 */
class MaterialRecipeTagsTest {
    private static final Path GENERATED = Path.of("src/generated/resources/data");

    /** The ingredients of a recipe: {@code key} values, {@code ingredients} entries and the {@code ingredient}. */
    private static void ingredients(JsonObject recipe, List<JsonElement> out) {
        if (recipe.has("ingredient")) out.add(recipe.get("ingredient"));
        if (recipe.has("key")) recipe.getAsJsonObject("key").entrySet().forEach(e -> out.add(e.getValue()));
        if (recipe.has("ingredients")) for (JsonElement e : recipe.getAsJsonArray("ingredients")) out.add(e);
    }

    /** Collects the tag ids ({@code #ns:path} without the {@code #}) of a recipe, failing on a direct item. */
    private static void collect(Path file, Set<String> tags) throws IOException {
        JsonObject recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        List<JsonElement> list = new ArrayList<>();
        ingredients(recipe, list);
        assertFalse(list.isEmpty(), file + " has no ingredient");
        for (JsonElement e : list) {
            assertTrue(e.isJsonPrimitive() && e.getAsString().startsWith("#"), file + " has a direct ingredient: " + e);
            tags.add(e.getAsString().substring(1));
        }
    }

    /** The ids of the item tags of the {@code Tags.Items} of NeoForge. */
    private static Set<String> neoForgeTags() throws IllegalAccessException {
        Set<String> ids = new HashSet<>();
        for (Field f : Tags.Items.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == TagKey.class) ids.add(((TagKey<?>) f.get(null)).location().toString());
        }
        return ids;
    }

    /** Tells whether a tag is defined by a generated tag file of this mod or its {@code c} tags. */
    private static boolean generated(String tag) {
        String[] part = tag.split(":");
        return Files.exists(GENERATED.resolve(part[0] + "/tags/item/" + part[1] + ".json"));
    }

    /** No material recipe has a direct item, and all tags used exist. */
    @Test
    void materialRecipesUseOnlyKnownTags() throws Exception {
        Set<String> tags = new HashSet<>();
        int files = 0;
        try (Stream<Path> walk = Files.walk(GENERATED.resolve("dotcraftresources/recipe/material"))) {
            for (Path p : walk.filter(x -> x.toString().endsWith(".json")).toList()) {
                collect(p, tags);
                files++;
            }
        }
        assertTrue(files > 100, "too few recipes: " + files);
        Set<String> known = neoForgeTags();
        for (String tag : tags) {
            assertTrue(generated(tag) || known.contains(tag), "undefined tag " + tag);
        }
    }

    /** The tools have a recipe each, made of tags only (and so covered by the tag check above). */
    @Test
    void toolRecipesExist() throws Exception {
        Path root = GENERATED.resolve("dotcraftresources/recipe/material");
        for (String file : List.of("stone/hammer", "iron/hammer", "steel/hammer", "diamond/hammer", "netherite/hammer",
                "stone/sandpaper", "iron/sandpaper", "diamond/sandpaper")) {
            Path recipe = root.resolve(file + ".json");
            assertTrue(Files.exists(recipe), "missing " + recipe);
            collect(recipe, new HashSet<>());
        }
        assertFalse(Files.exists(root.resolve("tin/hammer.json")));
    }
}
