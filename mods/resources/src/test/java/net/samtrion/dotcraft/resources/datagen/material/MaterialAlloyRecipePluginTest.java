package net.samtrion.dotcraft.resources.datagen.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.Materials;

/** Unit tests of the alloy input tags: per form, no two crafting recipes may have the same ingredient tags. */
class MaterialAlloyRecipePluginTest {
    private static List<Form> forms;

    private static Form form(String id) throws Exception {
        return Forms.find(forms(), id);
    }

    private static List<Form> forms() throws Exception {
        if (forms == null) forms = Forms.load(java.nio.file.Path.of(MaterialAlloyRecipePluginTest.class.getResource("/forms").toURI()));
        return forms;
    }

    private static List<Material> load() throws Exception {
        return Materials.load(java.nio.file.Path.of(MaterialAlloyRecipePluginTest.class.getResource("/materials").toURI()), forms());
    }

    /** The ingredients of each recipe of a form (one per combination of options), or an empty list when the form is skipped. */
    private static List<Map<String, Integer>> ingredients(Material alloy, Form form, List<Material> all) {
        List<Map<String, Integer>> recipes = new java.util.ArrayList<>();
        for (var choice : MaterialAlloyRecipePlugin.combinations(alloy, form, all)) {
            Map<String, Integer> items = new TreeMap<>();
            var inputs = alloy.alloy().orElseThrow().inputs();
            for (int i = 0; i < inputs.size(); i++) items.merge(choice.get(i).tag().location().toString(), inputs.get(i).count(), Integer::sum);
            recipes.add(items);
        }
        return recipes;
    }

    /** No two recipes of any alloy and form have the same ingredients, and steel resolves iron and coal or charcoal per form. */
    @Test
    void alloyRecipesDoNotCollide() throws Exception {
        List<Material> all = load();
        Map<Map<String, Integer>, String> seen = new HashMap<>();
        int recipes = 0;
        for (Material m : all) {
            if (m.alloy().isEmpty()) continue;
            for (Form form : forms()) {
                if (form.mix().isEmpty() || !m.has(form)) continue;
                for (Map<String, Integer> items : ingredients(m, form, all)) {
                    recipes++;
                    String old = seen.put(items, m.id() + "/" + form.id());
                    assertNull(old, m.id() + "/" + form.id() + " collides with " + old);
                }
            }
        }
        assertTrue(recipes > 0);
        Material steel = all.stream().filter(m -> m.id().equals("steel")).findFirst().orElseThrow();
        assertEquals(List.of(Map.of("c:dusts/iron", 1, "c:dusts/coal", 1), Map.of("c:dusts/iron", 1, "c:dusts/charcoal", 1)),
                ingredients(steel, form("dust"), all));
        assertEquals(List.of(Map.of("c:ingots/iron", 1, "dotcraftresources:coal", 1), Map.of("c:ingots/iron", 1, "dotcraftresources:charcoal", 1)),
                ingredients(steel, form("ingot"), all));
        assertEquals(List.of(Map.of("c:storage_blocks/iron", 1, "c:storage_blocks/coal", 1), Map.of("c:storage_blocks/iron", 1, "c:storage_blocks/charcoal", 1)),
                ingredients(steel, form("block"), all));
        assertTrue(ingredients(steel, form("grains"), all).isEmpty());
        assertTrue(ingredients(steel, form("dust_block"), all).isEmpty());
    }

    /** Alternatives and several sources of a mod each give their own option; a source of a mod is marked by its mod. */
    @Test
    void alternativesAndModSources(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        String ingot = "{\"forms\":{\"ingot\":{\"tag\":\"c:ingots/m\",\"item\":\"minecraft:iron_ingot\"}}}";
        String fake = "{\"mod\":\"fakemod\",\"forms\":{\"ingot\":{\"tag\":\"fakemod:ingots/m\",\"item\":\"fakemod:m_ingot\"}}}";
        String head = "{\"tier\":\"stone\",\"color\":\"#000000\",\"name\":{\"en_us\":\"M\",\"de_de\":\"M\"},\"id\":\"";
        Map<String, String> sources = Map.of(
                "m.json", head + "m\",\"external\":[" + ingot + "," + fake + "]}",
                "n.json", head + "n\",\"forms\":[\"ingot\"]}",
                "a.json", head + "a\",\"alloy\":{\"inputs\":[{\"material\":[\"m\",\"n\"]},{\"material\":\"n\",\"count\":2}]}}");
        for (var e : sources.entrySet()) java.nio.file.Files.writeString(dir.resolve(e.getKey()), e.getValue());
        List<Material> all = Materials.load(dir, forms());
        Material alloy = all.stream().filter(x -> x.id().equals("a")).findFirst().orElseThrow();
        var options = MaterialAlloyRecipePlugin.options(alloy, alloy.alloy().orElseThrow().inputs().get(0), form("ingot"), all);
        assertEquals(List.of("c:ingots/m", "fakemod:ingots/m", "c:ingots/n"),
                options.stream().map(o -> o.tag().location().toString()).toList());
        assertNull(options.get(0).source().mod().orElse(null));
        assertEquals("fakemod", options.get(1).source().mod().orElseThrow());
        assertNull(options.get(2).source());
        assertEquals(3, MaterialAlloyRecipePlugin.combinations(alloy, form("ingot"), all).size());
    }

    /** An unknown input material fails loudly. */
    @Test
    void unknownMaterialFails() throws Exception {
        List<Material> all = load();
        Material steel = all.stream().filter(m -> m.id().equals("steel")).findFirst().orElseThrow();
        assertThrows(IllegalStateException.class, () -> MaterialAlloyRecipePlugin.options(steel,
                new Material.Alloy.Input("nope", 1), form("dust"), all));
    }
}
