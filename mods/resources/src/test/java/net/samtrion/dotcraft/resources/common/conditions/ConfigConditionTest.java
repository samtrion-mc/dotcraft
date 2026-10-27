package net.samtrion.dotcraft.resources.common.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/** Unit tests for the material and form switches of {@link ConfigCondition} and the config built from the materials. */
class ConfigConditionTest {
    private static final ICondition.IContext CONTEXT = ICondition.IContext.EMPTY;

    @AfterEach
    void unload() throws ReflectiveOperationException {
        var field = ModConfigSpec.class.getDeclaredField("loadedConfig");
        field.setAccessible(true);
        field.set(CraftResourcesConfig.SPEC, null);
        CraftResourcesConfig.MATERIALS_ENABLED.clearCache();
        for (Material m : Materials.all()) {
            CraftResourcesConfig.materialSwitch(m.id()).clearCache();
            for (Form f : m.forms()) CraftResourcesConfig.formSwitch(m.id(), f.id()).clearCache();
            m.alloy().ifPresent(a -> a.alternatives().forEach(alt -> CraftResourcesConfig.recipeSwitch(m.id(), alt).clearCache()));
            if (m.hostedForm().isPresent()) m.oreSpecs().forEach(o -> CraftResourcesConfig.oreSwitch(m.id(), o.id()).clearCache());
        }
    }

    /** See {@code MaxTierConditionTest}: the loaded-config type is not accessible, so the field is set reflectively. */
    private static void load() throws ReflectiveOperationException {
        Class<?> type = Class.forName("net.neoforged.fml.config.LoadedConfig");
        var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var field = ModConfigSpec.class.getDeclaredField("loadedConfig");
        field.setAccessible(true);
        field.set(CraftResourcesConfig.SPEC, constructor.newInstance(CommentedConfig.inMemory(), null, null));
    }

    /** Every material and every form it has gets a switch at {@code materials.<id>.<configKey>.enabled}, default on. */
    @Test
    void specHasASwitchPerMaterialAndForm() {
        for (Material m : Materials.all()) {
            var material = CraftResourcesConfig.materialSwitch(m.id());
            assertEquals(List.of("materials", m.id(), "enabled"), material.getPath());
            assertTrue(material.getDefault(), m.id());
            for (Form f : m.forms()) {
                var form = CraftResourcesConfig.formSwitch(m.id(), f.id());
                assertEquals(List.of("materials", m.id(), f.configKey(), "enabled"), form.getPath());
                assertTrue(form.getDefault(), m.id() + "." + f.id());
            }
        }
        assertEquals(List.of("materials", "tin", "gears", "enabled"), CraftResourcesConfig.formSwitch("tin", "gear").getPath());
        assertEquals(List.of("materials", "enabled"), CraftResourcesConfig.MATERIALS_ENABLED.getPath());
        assertEquals(Forms.all().stream().map(Form::configKey).distinct().count(), Forms.all().size());
    }

    /** Without a loaded config (datagen) everything known is on; an unknown material or a missing form is off. */
    @Test
    void defaultsWhenNotLoaded() {
        assertTrue(ConfigCondition.ofMaterial("tin").test(CONTEXT));
        assertTrue(ConfigCondition.ofForm("tin", "gear").test(CONTEXT));
        assertFalse(ConfigCondition.ofMaterial("nothing").test(CONTEXT));
        assertFalse(ConfigCondition.ofForm("coal", "gear").test(CONTEXT));
        assertTrue(new ConfigCondition(ConfigCondition.Switch.MATERIALS).test(CONTEXT));
    }

    /** A disabled form drops only that form, a disabled material all of its forms, and the master switch everything. */
    @Test
    void switchesApplyInLevels() throws ReflectiveOperationException {
        load();
        CraftResourcesConfig.formSwitch("tin", "gear").set(false);
        assertFalse(ConfigCondition.ofForm("tin", "gear").test(CONTEXT));
        assertTrue(ConfigCondition.ofForm("tin", "ingot").test(CONTEXT));
        assertTrue(ConfigCondition.ofForm("copper", "dust").test(CONTEXT));
        assertTrue(ConfigCondition.ofMaterial("tin").test(CONTEXT));

        CraftResourcesConfig.materialSwitch("tin").set(false);
        assertFalse(ConfigCondition.ofMaterial("tin").test(CONTEXT));
        assertFalse(ConfigCondition.ofForm("tin", "ingot").test(CONTEXT));
        assertTrue(ConfigCondition.ofMaterial("copper").test(CONTEXT));

        CraftResourcesConfig.MATERIALS_ENABLED.set(false);
        assertFalse(ConfigCondition.ofMaterial("copper").test(CONTEXT));
        assertFalse(ConfigCondition.ofForm("copper", "dust").test(CONTEXT));
        assertFalse(new ConfigCondition(ConfigCondition.Switch.MATERIALS).test(CONTEXT));
    }

    /** Every alternative of an alloy input gets a switch at {@code materials.<alloy>.recipes.<alternative>.enabled}, default on. */
    @Test
    void specHasASwitchPerAlloyAlternative() {
        for (Material m : Materials.all()) {
            for (String alternative : m.alloy().map(Material.Alloy::alternatives).orElse(java.util.Set.of())) {
                var recipe = CraftResourcesConfig.recipeSwitch(m.id(), alternative);
                assertEquals(List.of("materials", m.id(), "recipes", alternative, "enabled"), recipe.getPath());
                assertTrue(recipe.getDefault(), m.id() + "." + alternative);
            }
        }
        assertEquals(List.of("materials", "steel", "recipes", "charcoal", "enabled"), CraftResourcesConfig.recipeSwitch("steel", "charcoal").getPath());
        assertEquals(List.of("materials", "steel", "recipes", "coal", "enabled"), CraftResourcesConfig.recipeSwitch("steel", "coal").getPath());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> CraftResourcesConfig.recipeSwitch("bronze", "tin"));
    }

    /** A disabled alternative drops only its recipes; the material and the master switch decide too; an unlisted alternative is off. */
    @Test
    void recipeSwitchApplies() throws ReflectiveOperationException {
        assertTrue(ConfigCondition.ofRecipe("steel", "charcoal").test(CONTEXT));
        assertFalse(ConfigCondition.ofRecipe("steel", "iron").test(CONTEXT));
        assertFalse(ConfigCondition.ofRecipe("nothing", "coal").test(CONTEXT));
        load();
        CraftResourcesConfig.recipeSwitch("steel", "charcoal").set(false);
        assertFalse(ConfigCondition.ofRecipe("steel", "charcoal").test(CONTEXT));
        assertTrue(ConfigCondition.ofRecipe("steel", "coal").test(CONTEXT));
        CraftResourcesConfig.recipeSwitch("steel", "charcoal").set(true);
        CraftResourcesConfig.materialSwitch("steel").set(false);
        assertFalse(ConfigCondition.ofRecipe("steel", "coal").test(CONTEXT));
    }

    /** Every ore entry of a material with the ore form gets a switch at {@code materials.<id>.ores.<entry>.enabled}, default on. */
    @Test
    void specHasASwitchPerOreEntry() {
        for (Material m : Materials.all()) {
            if (m.hostedForm().isEmpty()) continue;
            for (Material.OreSpec spec : m.oreSpecs()) {
                var ore = CraftResourcesConfig.oreSwitch(m.id(), spec.id());
                assertEquals(List.of("materials", m.id(), "ores", spec.id(), "enabled"), ore.getPath());
                assertTrue(ore.getDefault(), m.id() + "." + spec.id());
            }
            // the switch of the ore form is the sibling of the entry tables
            assertEquals(List.of("materials", m.id(), "ores", "enabled"), CraftResourcesConfig.formSwitch(m.id(), m.hostedForm().get().id()).getPath());
        }
        assertEquals(List.of("materials", "tin", "ores", "ore_0", "enabled"), CraftResourcesConfig.oreSwitch("tin", "ore_0").getPath());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> CraftResourcesConfig.oreSwitch("tin", "surface"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> CraftResourcesConfig.oreSwitch("bronze", "ore_0"));
    }

    /** A disabled entry fails only its own condition; the material and the master switch decide too; an unknown entry is off. */
    @Test
    void oreSwitchApplies() throws ReflectiveOperationException {
        assertTrue(ConfigCondition.ofOre("tin", "ore_0").test(CONTEXT));
        assertFalse(ConfigCondition.ofOre("tin", "surface").test(CONTEXT));
        assertFalse(ConfigCondition.ofOre("nothing", "ore_0").test(CONTEXT));
        load();
        CraftResourcesConfig.oreSwitch("tin", "ore_0").set(false);
        assertFalse(ConfigCondition.ofOre("tin", "ore_0").test(CONTEXT));
        assertTrue(ConfigCondition.ofOre("zinc", "ore_0").test(CONTEXT));
        assertTrue(ConfigCondition.ofForm("tin", "ore").test(CONTEXT));
        CraftResourcesConfig.oreSwitch("tin", "ore_0").set(true);
        assertTrue(ConfigCondition.ofOre("tin", "ore_0").test(CONTEXT));
        CraftResourcesConfig.materialSwitch("tin").set(false);
        assertFalse(ConfigCondition.ofOre("tin", "ore_0").test(CONTEXT));
    }

    /** Without a loaded config every tier that has a hammer is on; a form that no material has is off. */
    @Test
    void toolSwitchDefaultsWhenNotLoaded() {
        for (ToolTier tier : List.of(ToolTier.NONE, ToolTier.STONE, ToolTier.IRON, ToolTier.DIAMOND, ToolTier.NETHERITE)) {
            assertTrue(ConfigCondition.ofTool("hammer", tier).test(CONTEXT), tier.name());
        }
        assertFalse(ConfigCondition.ofTool("nothing", ToolTier.NONE).test(CONTEXT));
    }

    /**
     * The hammer condition of a tier holds while some hammer form of that tier or a better one is on: switching off a hammer
     * drops only the tiers it alone satisfied (steel counts as iron), the last hammer drops them all, and so does the master switch.
     */
    @Test
    void toolSwitchFollowsTheHammerForms() throws ReflectiveOperationException {
        load();
        CraftResourcesConfig.formSwitch("netherite", "hammer").set(false);
        assertFalse(ConfigCondition.ofTool("hammer", ToolTier.NETHERITE).test(CONTEXT));
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.DIAMOND).test(CONTEXT));
        CraftResourcesConfig.formSwitch("diamond", "hammer").set(false);
        assertFalse(ConfigCondition.ofTool("hammer", ToolTier.DIAMOND).test(CONTEXT));
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.IRON).test(CONTEXT));
        CraftResourcesConfig.formSwitch("iron", "hammer").set(false);
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.IRON).test(CONTEXT), "the steel hammer is of the iron tier");
        CraftResourcesConfig.materialSwitch("steel").set(false);
        assertFalse(ConfigCondition.ofTool("hammer", ToolTier.IRON).test(CONTEXT));
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.STONE).test(CONTEXT));
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.NONE).test(CONTEXT));
        CraftResourcesConfig.formSwitch("stone", "hammer").set(false);
        assertFalse(ConfigCondition.ofTool("hammer", ToolTier.NONE).test(CONTEXT), "no hammer form is left");
        CraftResourcesConfig.formSwitch("stone", "hammer").set(true);
        assertTrue(ConfigCondition.ofTool("hammer", ToolTier.NONE).test(CONTEXT));
        CraftResourcesConfig.MATERIALS_ENABLED.set(false);
        assertFalse(ConfigCondition.ofTool("hammer", ToolTier.NONE).test(CONTEXT));
    }

    /** The JSON shape: the global switches stay as they were, the new ones add {@code material} and {@code form}. */
    @Test
    void codec() {
        var codec = ConfigCondition.CODEC.codec();
        for (ConfigCondition c : List.of(new ConfigCondition(ConfigCondition.Switch.MATERIALS), new ConfigCondition(ConfigCondition.Switch.WORLDGEN),
                ConfigCondition.ofMaterial("tin"), ConfigCondition.ofForm("tin", "gear"), ConfigCondition.ofRecipe("steel", "charcoal"),
                ConfigCondition.ofOre("tin", "surface"), ConfigCondition.ofTool("hammer", ToolTier.IRON))) {
            var json = codec.encodeStart(JsonOps.INSTANCE, c).getOrThrow();
            assertEquals(c, codec.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
        assertEquals(JsonParser.parseString("{\"switch\":\"materials\"}"),
                codec.encodeStart(JsonOps.INSTANCE, new ConfigCondition(ConfigCondition.Switch.MATERIALS)).getOrThrow());
        assertEquals(JsonParser.parseString("{\"switch\":\"material_form\",\"material\":\"tin\",\"form\":\"gear\"}"),
                codec.encodeStart(JsonOps.INSTANCE, ConfigCondition.ofForm("tin", "gear")).getOrThrow());
        assertEquals(JsonParser.parseString("{\"switch\":\"material_ore\",\"material\":\"tin\",\"ore\":\"surface\"}"),
                codec.encodeStart(JsonOps.INSTANCE, ConfigCondition.ofOre("tin", "surface")).getOrThrow());
        assertEquals(JsonParser.parseString("{\"switch\":\"tool\",\"form\":\"hammer\",\"tier\":\"iron\"}"),
                codec.encodeStart(JsonOps.INSTANCE, ConfigCondition.ofTool("hammer", ToolTier.IRON)).getOrThrow());
    }

    /** A switch needs exactly its fields. */
    @Test
    void codecRejectsMissingOrSurplusFields() {
        var codec = ConfigCondition.CODEC.codec();
        for (String json : List.of("{\"switch\":\"material\"}", "{\"switch\":\"material_form\",\"material\":\"tin\"}",
                "{\"switch\":\"materials\",\"material\":\"tin\"}", "{\"switch\":\"material\",\"material\":\"tin\",\"form\":\"gear\"}",
                "{\"switch\":\"material_recipe\",\"material\":\"steel\"}", "{\"switch\":\"material\",\"material\":\"tin\",\"recipe\":\"coal\"}",
                "{\"switch\":\"material_ore\",\"material\":\"tin\"}", "{\"switch\":\"material\",\"material\":\"tin\",\"ore\":\"surface\"}",
                "{\"switch\":\"tool\",\"form\":\"hammer\"}", "{\"switch\":\"tool\",\"tier\":\"iron\"}",
                "{\"switch\":\"tool\",\"form\":\"hammer\",\"tier\":\"mithril\"}",
                "{\"switch\":\"tool\",\"form\":\"hammer\",\"tier\":\"iron\",\"material\":\"tin\"}",
                "{\"switch\":\"material_form\",\"material\":\"tin\",\"form\":\"gear\",\"tier\":\"iron\"}",
                "{\"switch\":\"nope\"}")) {
            assertTrue(codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(), json);
        }
    }
}
