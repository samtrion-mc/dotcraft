package net.samtrion.dotcraft.resources.datagen.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;
import net.samtrion.dotcraft.resources.tool.ToolTier;

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
            assertTrue(generated(tag) || known.contains(tag) || tag.startsWith("minecraft:"), "undefined tag " + tag);
        }
    }

    /** Every material recipe by its id below the material folder, e.g. {@code tin/dust_from_raw_crushing}. */
    private static Map<String, JsonObject> recipes() throws IOException {
        Path root = GENERATED.resolve("dotcraftresources/recipe/material");
        Map<String, JsonObject> result = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.filter(x -> x.toString().endsWith(".json")).toList()) {
                String id = root.relativize(p).toString().replace('\\', '/');
                result.put(id.substring(0, id.length() - ".json".length()), JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            }
        }
        return result;
    }

    /** The hammer tag a recipe uses ({@code c:tools/hammer} or {@code dotcraftresources:hammers/<tier>}, without the {@code #}), or {@code null}. */
    private static String hammerTag(JsonObject recipe) {
        List<JsonElement> list = new ArrayList<>();
        ingredients(recipe, list);
        for (JsonElement e : list) {
            String tag = e.getAsString().substring(1);
            if (tag.equals("c:tools/hammer") || tag.startsWith("dotcraftresources:hammers/")) return tag;
        }
        return null;
    }

    /** The material of a recipe id of {@link #recipes()}. */
    private static Material materialOf(String id) {
        String material = id.substring(0, id.indexOf('/'));
        return Materials.all().stream().filter(m -> m.id().equals(material)).findFirst().orElseThrow();
    }

    /** The tiers that have a hammer are those of the hammer materials, steel counting as iron. */
    @Test
    void hammerTiers() {
        assertEquals(List.of(ToolTier.STONE, ToolTier.IRON, ToolTier.DIAMOND, ToolTier.NETHERITE), ResourceBlockTags.hammerTiers());
        assertEquals(ToolTier.IRON, Materials.all().stream().filter(m -> m.id().equals("steel")).findFirst().orElseThrow().tier());
    }

    /** A material below every hammer takes any hammer, else the lowest hammer tier that is at least its tier. */
    @Test
    void hammerTagOfATier() {
        for (ToolTier tier : List.of(ToolTier.NONE, ToolTier.WOOD, ToolTier.GOLD)) assertEquals(ResourceBlockTags.HAMMERS, ResourceBlockTags.hammers(tier), tier.name());
        assertEquals("dotcraftresources:hammers/stone", ResourceBlockTags.hammers(ToolTier.STONE).location().toString());
        assertEquals("dotcraftresources:hammers/iron", ResourceBlockTags.hammers(ToolTier.COPPER).location().toString());
        assertEquals("dotcraftresources:hammers/iron", ResourceBlockTags.hammers(ToolTier.IRON).location().toString());
        assertEquals("dotcraftresources:hammers/diamond", ResourceBlockTags.hammers(ToolTier.DIAMOND).location().toString());
        assertEquals("dotcraftresources:hammers/netherite", ResourceBlockTags.hammers(ToolTier.NETHERITE).location().toString());
    }

    /** The tag of a tier holds the hammers of that tier and of every better one: stone takes all, diamond only diamond and netherite. */
    @Test
    void hammerTierTagsHoldTheBetterHammers() throws Exception {
        Map<ToolTier, Set<String>> expected = Map.of(
                ToolTier.STONE, Set.of("stone", "iron", "steel", "diamond", "netherite"),
                ToolTier.IRON, Set.of("iron", "steel", "diamond", "netherite"),
                ToolTier.DIAMOND, Set.of("diamond", "netherite"),
                ToolTier.NETHERITE, Set.of("netherite"));
        for (ToolTier tier : ResourceBlockTags.hammerTiers()) {
            Path file = GENERATED.resolve("dotcraftresources/tags/item/hammers/" + tier.getSerializedName() + ".json");
            Set<String> actual = new HashSet<>();
            for (JsonElement e : JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("values")) {
                String entry = e.isJsonObject() ? e.getAsJsonObject().get("id").getAsString() : e.getAsString();
                assertTrue(entry.startsWith("#c:tools/hammer/"), file + " has " + entry);
                actual.add(entry.substring("#c:tools/hammer/".length()));
            }
            assertEquals(expected.get(tier), actual, tier.name());
            Set<String> byTier = new HashSet<>();
            for (Material m : Materials.all()) if (m.has("hammer") && m.tier().canMine(tier)) byTier.add(m.id());
            assertEquals(byTier, actual, tier.name());
        }
    }

    /**
     * Every crush and forge recipe uses the hammer tag of the tier of its material and carries the tool condition of that
     * tier, so a stone hammer cannot work a diamond or netherite material.
     */
    @Test
    void hammerRecipesNeedTheTierOfTheMaterial() throws Exception {
        int hammered = 0;
        for (var entry : recipes().entrySet()) {
            String tag = hammerTag(entry.getValue());
            if (tag == null) continue;
            hammered++;
            ToolTier tier = materialOf(entry.getKey()).tier();
            assertEquals(ResourceBlockTags.hammers(tier).location().toString(), tag, entry.getKey());
            boolean tool = false;
            for (JsonElement c : entry.getValue().getAsJsonArray("neoforge:conditions")) {
                JsonObject condition = c.getAsJsonObject();
                if (!condition.has("switch") || !condition.get("switch").getAsString().equals("tool")) continue;
                tool = true;
                assertEquals("hammer", condition.get("form").getAsString(), entry.getKey());
                assertEquals(tier.getSerializedName(), condition.get("tier").getAsString(), entry.getKey());
            }
            assertTrue(tool, entry.getKey() + " lacks the tool condition");
        }
        assertTrue(hammered > 10, "too few hammer recipes: " + hammered);
        Map<String, JsonObject> recipes = recipes();
        assertEquals("dotcraftresources:hammers/stone", hammerTag(recipes.get("tin/dust_from_raw_crushing")));
        assertEquals("dotcraftresources:hammers/stone", hammerTag(recipes.get("tin/plate_from_ingot_forging")));
        assertEquals("dotcraftresources:hammers/iron", hammerTag(recipes.get("iron/plate_from_ingot_forging")));
    }

    /** A hammer and one raw material crush to two dust, three ingots and a hammer forge one plate. */
    @Test
    void hammerRecipes() throws Exception {
        Path root = GENERATED.resolve("dotcraftresources/recipe/material");
        for (String id : List.of("tin", "zinc", "aluminum")) {
            String json = Files.readString(root.resolve(id + "/dust_from_raw_crushing.json"));
            assertTrue(json.contains("#dotcraftresources:hammers/") && json.contains("\"count\": 2"), id);
        }
        for (String file : List.of("iron/plate_from_ingot_forging", "tin/plate_from_ingot_forging")) {
            String json = Files.readString(root.resolve(file + ".json"));
            assertTrue(json.contains("minecraft:crafting_shaped") && json.contains("#dotcraftresources:hammers/"), file);
        }
        assertTrue(Files.readString(root.resolve("tin/plate_from_ingot_forging.json")).contains("\"III\""));
    }

    /**
     * The recipe ids follow one scheme: a crush is {@code _crushing} and a forge {@code _forging} (the recipes with a hammer), a
     * shaped or shapeless conversion is just the form, and no id ends with {@code _shaped} or {@code _shapeless}.
     */
    @Test
    void recipeIdScheme() throws Exception {
        for (var entry : recipes().entrySet()) {
            String name = entry.getKey().substring(entry.getKey().indexOf('/') + 1);
            assertFalse(name.endsWith("_shaped") || name.endsWith("_shapeless"), entry.getKey());
            if (hammerTag(entry.getValue()) == null) continue;
            String type = entry.getValue().get("type").getAsString();
            if (name.contains("_from_")) {
                assertTrue(name.contains(type.equals("minecraft:crafting_shaped") ? "_forging" : "_crushing"), entry.getKey());
            }
        }
        for (String id : List.of("iron/hammer", "iron/sandpaper", "tin/gear")) {
            assertTrue(recipes().containsKey(id), "missing " + id);
        }
    }

    /** The tools have a recipe each, made of tags only (and so covered by the tag check above). */
    @Test
    void toolRecipesExist() throws Exception {
        Path root = GENERATED.resolve("dotcraftresources/recipe/material");
        for (String file : List.of("stone/hammer", "iron/hammer", "steel/hammer", "diamond/hammer", "netherite/hammer",
                "sand/sandpaper", "iron/sandpaper", "diamond/sandpaper")) {
            Path recipe = root.resolve(file + ".json");
            assertTrue(Files.exists(recipe), "missing " + recipe);
            collect(recipe, new HashSet<>());
        }
        assertFalse(Files.exists(root.resolve("tin/hammer.json")));
    }
}
