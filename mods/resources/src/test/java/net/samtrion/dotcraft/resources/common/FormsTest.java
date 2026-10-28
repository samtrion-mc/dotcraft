package net.samtrion.dotcraft.resources.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.resources.common.conditions.Conditions;

/** Unit tests for {@link Forms} and {@link Form}: the source format and the shipped sources. */
class FormsTest {
    private static final String ITEM = "{\"id\":\"%1$s\",\"kind\":\"item\",\"order\":%2$d,\"configKey\":\"%1$ss\",\"tag\":{\"parent\":\"x\"},"
            + "\"name\":{\"en_us\":\"%%s X\",\"de_de\":\"%%s-X\"}%3$s}";

    /**
     * Loads the shipped forms from the test classpath.
     *
     * @return the forms in creative tab order
     * @throws Exception if the sources cannot be read
     */
    private static List<Form> shipped() throws Exception {
        return Forms.load(Path.of(FormsTest.class.getResource("/forms").toURI()));
    }

    /**
     * Parses one item form source with the given extra JSON fields.
     *
     * @param id    the id of the form
     * @param order the order of the form
     * @param extra more fields, starting with a comma, or empty
     * @return the parsed forms
     */
    private static List<Form> parse(String id, int order, String extra) {
        return Forms.parseAll(Map.of(id + ".json", String.format(ITEM, id, order, extra)));
    }

    /** The shipped forms load in creative tab order with unique ids, and are named in every locale. */
    @Test
    void shippedSourcesAreValid() throws Exception {
        List<Form> forms = shipped();
        assertEquals(List.of("raw", "raw_block", "ingot", "nugget", "grains", "block", "dust", "dust_block", "plate", "gear", "ore", "hammer", "sandpaper"),
                forms.stream().map(Form::id).toList());
        assertEquals(List.of("raw", "raw_blocks", "ingots", "nuggets", "grains", "blocks", "dusts", "dust_blocks", "plates", "gears", "ores", "hammers", "sandpapers"),
                forms.stream().map(Form::configKey).toList());
        assertEquals(forms.size(), forms.stream().map(Form::configKey).distinct().count());
        for (Form f : forms) {
            assertEquals(Set.copyOf(Locales.SUPPORTED), f.names().keySet(), f.id());
            assertEquals(f.isBlock(), f.block().isPresent(), f.id());
        }
        assertEquals(List.of("ore"), forms.stream().filter(Form::hosted).map(Form::id).toList());
        assertEquals("%s_dust_block", Forms.find(forms, "dust_block").pathPattern());
        assertEquals("Rich Deepslate Tin Ore", Forms.find(forms, "ore").displayName("en_us", "Tin", OreHost.DEEPSLATE, OreRichness.RICH));
        assertEquals("Tin-Barren", Forms.find(forms, "ingot").displayName("de_de", "Tin"));
    }

    /** A tool form has a durability above 0 and is no block; its recipe is a shaped or shapeless conversion with {@code #} for the material. */
    @Test
    void toolSpec() throws Exception {
        Form hammer = Forms.find(shipped(), "hammer");
        assertEquals(1.0F, hammer.tool().orElseThrow().durability());
        assertEquals(List.of(" # ", " |#", "|  "), hammer.conversions().get(0).pattern());
        assertEquals(Form.Conversion.Kind.SHAPED, hammer.conversions().get(0).kind());
        Form sandpaper = Forms.find(shipped(), "sandpaper");
        assertEquals(Form.Conversion.Kind.SHAPELESS, sandpaper.conversions().get(0).kind());
        assertEquals(List.of("#PS"), sandpaper.conversions().get(0).pattern());
        assertTrue(Forms.find(shipped(), "gear").tool().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"tool\":{\"durability\":0}"));
        parse("cog", 5, ",\"tool\":{},\"conversions\":[{\"kind\":\"shaped\",\"ratio\":1,\"pattern\":[\"#|\"],\"key\":{\"|\":\"c:rods/wooden\"}}]");
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"conversions\":[{\"kind\":\"shaped\",\"ratio\":1,\"pattern\":[\"#|\"]}]"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"conversions\":[{\"kind\":\"shapeless\",\"ratio\":1,\"pattern\":[\"##\",\"##\"]}]"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"conversions\":[{\"from\":\"cog\",\"kind\":\"forge\",\"ratio\":4}]"));
    }

    /** Only id, kind, order, tag and name are required; template and path default to the id. */
    @Test
    void defaults() {
        Form f = parse("cog", 5, "").get(0);
        assertEquals("cog", f.template());
        assertEquals("%s_cog", f.pathPattern());
        assertTrue(f.tinted());
        assertFalse(f.mined());
        assertEquals(Form.ExternalRule.OPTIONAL, f.external());
        assertEquals("x", f.tagFolder());
    }

    /** A form with variants picks a stable one per material, and the shipped nugget textures and models exist for each. */
    @Test
    void variants() throws Exception {
        Form f = parse("cog", 5, ",\"variants\":4").get(0);
        assertEquals(f.modelOf("tin"), f.modelOf("tin"));
        assertEquals(4, java.util.stream.IntStream.range(0, 200).mapToObj(i -> f.modelOf("m" + i)).distinct().count());
        assertEquals("cog", parse("cog", 5, "").get(0).modelOf("tin"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"variants\":0"));
        Form nugget = Forms.find(shipped(), "nugget");
        for (int i = 0; i < nugget.variants(); i++) {
            for (String dir : new String[] {"textures", "models"}) {
                String file = "/assets/dotcraftresources/" + dir + "/item/material/nugget_" + i + (dir.equals("models") ? ".json" : ".png");
                assertTrue(FormsTest.class.getResource(file) != null, "missing " + file);
            }
        }
    }

    /** Broken sources fail instead of being skipped. */
    @Test
    void brokenSourcesFail() {
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"block\":{\"tool\":\"pickaxe\"}"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"path\":\"cog\""));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"requires\":[\"nope\"]"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"conversions\":[{\"from\":\"nope\",\"kind\":\"smelt\"}]"));
        assertThrows(IllegalArgumentException.class, () -> parse("cog", 5, ",\"conversions\":[{\"from\":\"cog\",\"kind\":\"smelt\"}]"));
        assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of("x.json", String.format(ITEM, "cog", 5, ""))));
        assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of(
                "a.json", String.format(ITEM, "a", 5, ""), "b.json", String.format(ITEM, "b", 5, ""))));
    }

    /** The config key must be given, well formed, not the reserved {@code enabled}, and unique. */
    @Test
    void configKeys() {
        assertEquals("cogs", parse("cog", 5, "").get(0).configKey());
        String noKey = String.format(ITEM, "cog", 5, "").replace("\"configKey\":\"cogs\",", "");
        assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of("cog.json", noKey)));
        for (String bad : new String[] {"Cogs", "enabled", "co-gs", "1cogs", ""}) {
            String json = String.format(ITEM, "cog", 5, "").replace("\"cogs\"", "\"" + bad + "\"");
            assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of("cog.json", json)), bad);
        }
        String second = String.format(ITEM, "bolt", 6, "").replace("\"bolts\"", "\"cogs\"");
        var duplicate = assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of(
                "cog.json", String.format(ITEM, "cog", 5, ""), "bolt.json", second)));
        assertTrue(duplicate.getMessage().contains("duplicate configKey"), duplicate.getMessage());
    }

    /** A missing translation or an unsupported locale fails. */
    @Test
    void namesAreCompleteAndSupported() {
        String partial = "{\"id\":\"cog\",\"kind\":\"item\",\"order\":1,\"configKey\":\"cogs\",\"tag\":{\"parent\":\"x\"},\"name\":{\"en_us\":\"%s\"}}";
        List<Form> forms = Forms.parseAll(Map.of("cog.json", partial));
        assertThrows(IllegalArgumentException.class, () -> forms.forEach(Form::requireLocales));
        assertThrows(IllegalArgumentException.class, () -> Forms.parseAll(Map.of("cog.json", partial.replace("en_us", "fr_fr"))));
    }

    private static final String MOD = "\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]";

    /**
     * Parses sources whose conditions hold with the loaded mods of the test, like with the mods of the game.
     *
     * @param sources the file contents by file name
     * @param mods    the ids of the mods that count as loaded
     * @return the forms that are loaded and those that are not
     */
    private static Forms.Loaded parseWithMods(Map<String, String> sources, String... mods) {
        return Forms.parse(sources, conditions -> Conditions.test(conditions, Set.of(mods)::contains));
    }

    /** A form whose condition fails is left out with the forms that need it; it is there when the condition holds. */
    @Test
    void conditionsLeaveOutForms() {
        Map<String, String> sources = Map.of(
                "a.json", String.format(ITEM, "a", 1, "," + MOD),
                "b.json", String.format(ITEM, "b", 2, ",\"requires\":[\"a\"]"),
                "c.json", String.format(ITEM, "c", 3, ",\"conversions\":[{\"from\":\"a\",\"kind\":\"smelt\"}]"),
                "d.json", String.format(ITEM, "d", 4, ""));
        Forms.Loaded without = parseWithMods(sources);
        assertEquals(List.of("c", "d"), without.forms().stream().map(Form::id).toList());
        assertEquals(List.of("a", "b"), without.excluded().stream().map(Form::id).toList());
        Forms.Loaded with = parseWithMods(sources, "fakemod");
        assertEquals(List.of("a", "b", "c", "d"), with.forms().stream().map(Form::id).toList());
        assertTrue(with.excluded().isEmpty());
        assertEquals(1, with.forms().get(0).conditions().size());
        assertEquals(with.forms().get(0).conditions(), with.forms().get(1).conditions());
        assertTrue(with.forms().get(2).conditions().isEmpty());
        assertTrue(with.forms().get(3).conditions().isEmpty());
    }

    /** The conditions of a form it needs are its conditions too, also through other forms. */
    @Test
    void conditionsOfNeededFormsAreInherited() {
        Map<String, String> sources = Map.of(
                "a.json", String.format(ITEM, "a", 1, "," + MOD),
                "b.json", String.format(ITEM, "b", 2, ",\"requires\":[\"a\"]"),
                "c.json", String.format(ITEM, "c", 3, ",\"requires\":[\"b\"]"),
                "d.json", String.format(ITEM, "d", 4, ""));
        Forms.Loaded loaded = parseWithMods(sources, "fakemod");
        assertEquals(loaded.forms().get(0).conditions(), loaded.forms().get(1).conditions());
        assertEquals(loaded.forms().get(0).conditions(), loaded.forms().get(2).conditions());
        assertTrue(loaded.forms().get(3).conditions().isEmpty());
    }

    /** A form that names a left out form as the drop of its block is left out too, and a form without conditions is unchanged. */
    @Test
    void dropOfLeftOutFormLeavesOutTheBlock() {
        String ore = "{\"id\":\"ore\",\"kind\":\"block\",\"order\":2,\"configKey\":\"ores\",\"tag\":{\"parent\":\"ores\"},"
                + "\"name\":{\"en_us\":\"%s Ore\",\"de_de\":\"%s-Erz\"},\"block\":{\"tool\":\"pickaxe\",\"loot\":\"ore\",\"drop\":\"raw\","
                + "\"hosted\":{\"richness\":{\"en_us\":{},\"de_de\":{}},\"host\":{\"en_us\":{},\"de_de\":{}}}}}";
        Map<String, String> sources = Map.of("raw.json", String.format(ITEM, "raw", 1, "," + MOD), "ore.json", ore);
        assertTrue(parseWithMods(sources).forms().isEmpty());
        assertEquals(2, parseWithMods(sources, "fakemod").forms().size());
    }

    /** References are checked also for left out forms, and a condition that cannot be evaluated at construction fails the source. */
    @Test
    void conditionErrorsFail() {
        assertThrows(IllegalArgumentException.class, () -> parseWithMods(Map.of(
                "a.json", String.format(ITEM, "a", 1, ",\"requires\":[\"nope\"]," + MOD))));
        var e = assertThrows(IllegalArgumentException.class, () -> parseWithMods(Map.of("a.json", String.format(ITEM, "a", 1,
                ",\"neoforge:conditions\":[{\"type\":\"neoforge:tag_empty\",\"tag\":\"c:x\"}]"))));
        assertTrue(e.getMessage().contains("neoforge:tag_empty"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> parseWithMods(Map.of("a.json", String.format(ITEM, "a", 1,
                ",\"neoforge:conditions\":[{\"type\":\"dotcraftresources:config\",\"switch\":\"materials\"}]"))));
    }
}
