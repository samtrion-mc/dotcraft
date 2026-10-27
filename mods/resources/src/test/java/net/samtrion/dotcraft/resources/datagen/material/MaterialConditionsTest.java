package net.samtrion.dotcraft.resources.datagen.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.conditions.ConfigCondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;

/**
 * Unit tests that the data of a material and form with {@code neoforge:conditions} carries them: the data generation
 * counts the conditions as met (so every file is made) and the recipe plugins add them to the config conditions.
 */
class MaterialConditionsTest {
    private static final ICondition MATERIAL_MOD = new ModLoadedCondition("materialmod");
    private static final ICondition FORM_MOD = new ModLoadedCondition("formmod");

    private List<Form> forms;
    private List<Material> materials;

    /** Loads the shipped forms with a condition on the ingot, and three materials, one of them with a condition, as the data generation does. */
    @BeforeEach
    void load(@TempDir Path dir) throws Exception {
        System.setProperty(Conditions.DATAGEN_PROPERTY, "true");
        Path formDir = Files.createDirectory(dir.resolve("forms"));
        try (var files = Files.list(Path.of(MaterialConditionsTest.class.getResource("/forms").toURI()))) {
            for (Path file : files.toList()) {
                String text = Files.readString(file);
                if (file.getFileName().toString().equals("ingot.json")) {
                    text = text.replaceFirst("[{]", "{\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"formmod\"}],");
                }
                Files.writeString(formDir.resolve(file.getFileName().toString()), text);
            }
        }
        forms = Forms.load(formDir);
        Path materialDir = Files.createDirectory(dir.resolve("materials"));
        String head = "{\"tier\":\"stone\",\"color\":\"#000000\",\"name\":{\"en_us\":\"M\",\"de_de\":\"M\"},\"forms\":[\"ingot\",\"dust\",\"grains\"],\"id\":\"";
        Files.writeString(materialDir.resolve("t.json"), head + "t\",\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"materialmod\"}]}");
        Files.writeString(materialDir.resolve("p.json"), head + "p\"}");
        Files.writeString(materialDir.resolve("a.json"), head + "a\",\"alloy\":{\"inputs\":[{\"material\":\"t\"},{\"material\":\"p\"}]}}");
        materials = Materials.load(materialDir, forms);
    }

    @AfterEach
    void clear() {
        System.clearProperty(Conditions.DATAGEN_PROPERTY);
    }

    private Form form(String id) {
        return Forms.find(forms, id);
    }

    private Material material(String id) {
        return materials.stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
    }

    /** While the data is generated every material and form is made, whatever its conditions, and keeps them. */
    @Test
    void datagenLoadsEverything() {
        assertEquals(List.of("a", "p", "t"), materials.stream().map(Material::id).toList());
        assertEquals(List.of(FORM_MOD), form("ingot").conditions());
        assertEquals(List.of(MATERIAL_MOD), material("t").conditions());
        assertEquals(List.of(MATERIAL_MOD, FORM_MOD), material("t").conditions(form("ingot")));
        assertEquals(List.of(MATERIAL_MOD), material("t").conditions(form("dust")));
        assertEquals(List.of(FORM_MOD), material("p").conditions(form("ingot")));
        assertTrue(material("p").conditions(form("dust")).isEmpty());
    }

    /** A recipe between forms carries the conditions of its material and of both forms, after the config conditions. */
    @Test
    void conversionRecipesCarryConditions() {
        List<ICondition> smelt = MaterialRecipePlugin.conditions(material("t"), form("dust"), form("ingot"), null);
        assertEquals(List.of(new ConfigCondition(ConfigCondition.Switch.MATERIALS), ConfigCondition.ofForm("t", "dust"),
                ConfigCondition.ofForm("t", "ingot"), MATERIAL_MOD, FORM_MOD), smelt);
        assertEquals(List.of(new ConfigCondition(ConfigCondition.Switch.MATERIALS), ConfigCondition.ofForm("p", "dust"),
                ConfigCondition.ofForm("p", "ingot"), FORM_MOD), MaterialRecipePlugin.conditions(material("p"), form("dust"), form("ingot"), null));
        // the form that is only used also counts, and the result of an external item has no form
        assertEquals(List.of(new ConfigCondition(ConfigCondition.Switch.MATERIALS), ConfigCondition.ofForm("p", "ingot"), FORM_MOD),
                MaterialRecipePlugin.conditions(material("p"), form("ingot"), null, null));
    }

    /** Without any {@code neoforge:conditions} the recipes carry the config conditions only, as before. */
    @Test
    void withoutConditionsNothingIsAdded() {
        assertEquals(List.of(new ConfigCondition(ConfigCondition.Switch.MATERIALS), ConfigCondition.ofForm("p", "dust"),
                ConfigCondition.ofForm("p", "grains")), MaterialRecipePlugin.conditions(material("p"), form("dust"), form("grains"), null));
    }

    /** An alloy recipe carries the conditions of the alloy, of its form and of every input, so it is dropped where an input is left out. */
    @Test
    void alloyRecipesCarryConditionsOfTheInputs() {
        Material alloy = material("a");
        var ingot = MaterialAlloyRecipePlugin.combinations(alloy, form("ingot"), materials).get(0);
        List<ICondition> conditions = MaterialAlloyRecipePlugin.conditions(alloy, form("ingot"), ingot);
        assertTrue(conditions.contains(MATERIAL_MOD), "input material t");
        assertTrue(conditions.contains(FORM_MOD), "form ingot of the alloy and of the inputs");
        assertEquals(1, conditions.stream().filter(FORM_MOD::equals).count());
        assertTrue(conditions.contains(new ConfigCondition(ConfigCondition.Switch.MATERIALS)));
        var dust = MaterialAlloyRecipePlugin.combinations(alloy, form("dust"), materials).get(0);
        List<ICondition> dustConditions = MaterialAlloyRecipePlugin.conditions(alloy, form("dust"), dust);
        assertTrue(dustConditions.contains(MATERIAL_MOD));
        assertFalse(dustConditions.contains(FORM_MOD));
        assertEquals(dustConditions.size(), dustConditions.stream().distinct().count());
    }
}
