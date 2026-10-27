package net.samtrion.dotcraft.resources.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/** Unit tests for {@link Materials} and {@link Material}: the source format and the shipped sources. */
class MaterialsTest {
    /** The shipped forms, which the sources of the tests refer to. */
    private static final List<Form> FORMS = forms();
    /** The forms a material without a {@code forms} list has: all but the tool forms. */
    private static final List<Form> DEFAULT_FORMS = FORMS.stream().filter(f -> f.tool().isEmpty()).toList();

    /** Loads the shipped forms from the test classpath. */
    private static List<Form> forms() {
        try {
            return Forms.load(Path.of(MaterialsTest.class.getResource("/forms").toURI()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Looks up a shipped form by id. */
    private static Form form(String id) {
        return Forms.find(FORMS, id);
    }

    /**
     * Parses a source that must be the only material, with the tier {@code stone} unless it has one.
     *
     * @param json the source
     * @return the material
     */
    private static Material only(String json) {
        if (!json.contains("\"tier\"")) json = json.replaceFirst("[{]", "{\"tier\":\"stone\",");
        String id = json.replaceAll(".*?\"id\":\"([a-z_]+)\".*", "$1");
        List<Material> materials = Materials.parseAll(Map.of(id + ".json", json), FORMS);
        assertEquals(1, materials.size());
        return materials.get(0);
    }

    /**
     * Parses an alloy together with plain stub materials for its inputs, as the alloy inputs must exist.
     *
     * @param json   the source of the alloy
     * @param inputs the ids of the input materials to add as stubs
     * @return the alloy, the one material of the sources that has an {@code alloy}
     */
    private static Material alloyOf(String json, String... inputs) {
        Map<String, String> sources = new java.util.HashMap<>();
        for (String input : inputs) {
            sources.put(input + ".json", "{\"id\":\"" + input + "\",\"tier\":\"stone\",\"color\":\"#000000\"}");
        }
        String id = json.replaceAll(".*?\"id\":\"([a-z_]+)\".*", "$1");
        sources.put(id + ".json", json.replaceFirst("[{]", "{\"tier\":\"stone\","));
        return Materials.parseAll(sources, FORMS).stream().filter(m -> m.alloy().isPresent()).findFirst().orElseThrow();
    }

    /** A material {@code m} with this {@code ore} field, see {@link #ore}. */
    private static Material onlyOre(String field) {
        return only("{\"id\":\"m\",\"color\":\"#000000\"," + field + "}");
    }

    /** Loads the shipped materials from the test classpath. */
    private static List<Material> shipped() throws Exception {
        return Materials.load(Path.of(MaterialsTest.class.getResource("/materials").toURI()), FORMS);
    }

    /** Only id, color and tier are required; the rest has defaults. */
    @Test
    void defaults() {
        Material m = only("{\"id\":\"m\",\"color\":\"#336699\"}");
        assertEquals(0xFF336699, m.color());
        assertEquals(ToolTier.STONE, m.tier());
        assertEquals(DEFAULT_FORMS, m.forms());
        assertEquals(List.of(Material.OreSpec.DEFAULT), m.oreSpecs());
        assertEquals("ore_0", m.oreSpecs().get(0).id());
        assertTrue(m.alloy().isEmpty());
        assertThrows(IllegalStateException.class, () -> m.displayName("en_us"));
    }

    /** The tier is required, and {@code none} is a value of its own. */
    @Test
    void tierIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("m.json", "{\"id\":\"m\",\"color\":\"#000000\"}"), FORMS));
        assertEquals(ToolTier.NONE, only("{\"id\":\"m\",\"tier\":\"none\",\"color\":\"#000000\"}").tier());
    }

    /** Every shipped material declares its tier explicitly; the vanilla parts match their vanilla tool, coal and charcoal need none. */
    @Test
    void shippedTiers() throws Exception {
        Map<String, ToolTier> expected = Map.of("wood", ToolTier.WOOD, "stone", ToolTier.STONE, "copper", ToolTier.STONE,
                "iron", ToolTier.IRON, "gold", ToolTier.WOOD, "diamond", ToolTier.DIAMOND, "netherite", ToolTier.NETHERITE,
                "coal", ToolTier.NONE, "charcoal", ToolTier.NONE);
        Path dir = Path.of(MaterialsTest.class.getResource("/materials").toURI());
        try (var files = java.nio.file.Files.list(dir)) {
            for (Path file : files.toList()) {
                assertTrue(java.nio.file.Files.readString(file).contains("\"tier\""), file + " declares no tier");
            }
        }
        for (Material m : shipped()) {
            if (expected.containsKey(m.id())) assertEquals(expected.get(m.id()), m.tier(), m.id());
        }
    }

    /** Stone and wood add only gear to the vanilla materials, and stone its tools. */
    @Test
    void stoneAndWood() throws Exception {
        for (String id : List.of("stone", "wood")) {
            Material m = shipped().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            assertEquals(id.equals("stone") ? List.of(form("gear"), form("hammer")) : List.of(form("gear")), m.forms(), id);
            assertTrue(m.isExternal(), id);
        }
    }

    /** Paths of items, blocks and ores follow one scheme. */
    @Test
    void paths() {
        Material m = only("{\"id\":\"dark_tin\",\"color\":\"#000000\",\"name\":{\"en_us\":\"Dark Tin\",\"de_de\":\"Dunkelzinn\"}}");
        assertEquals("dark_tin_ingot", m.path(form("ingot")));
        assertEquals("dark_tin_grains", m.path(form("grains")));
        assertEquals("dark_tin_dust_block", m.path(form("dust_block")));
        assertEquals("dark_tin_ore", m.orePath(OreHost.STONE, OreRichness.NORMAL));
        assertEquals("poor_deepslate_dark_tin_ore", m.orePath(OreHost.DEEPSLATE, OreRichness.POOR));
        assertEquals("rich_nether_dark_tin_ore", m.orePath(OreHost.NETHER, OreRichness.RICH));
        assertEquals("Dunkelzinn", m.displayName("de_de"));
        assertThrows(IllegalArgumentException.class, () -> m.path(form("ore")));
    }

    /** Every host has all richnesses; the forms are listed in creative tab order. */
    @Test
    void oreVariantsAndOrder() {
        Material m = only("{\"id\":\"m\",\"color\":\"#000000\",\"ore\":[{\"hosts\":[\"end\",\"stone\"]}]}");
        assertEquals(6, m.oreVariants().size());
        assertEquals(new Material.OreVariant(OreHost.END, OreRichness.POOR), m.oreVariants().get(0));
        assertEquals(form("raw"), m.forms().get(0));
        assertEquals(form("ore"), m.forms().get(m.forms().size() - 1));
        assertTrue(only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ingot\"]}").oreVariants().isEmpty());
    }

    /** The drop counts of the ore richnesses: poor half, rich double. */
    @Test
    void richnessDrops() {
        assertEquals(0, OreRichness.POOR.minDrops());
        assertEquals(1, OreRichness.POOR.maxDrops());
        assertEquals(1, OreRichness.NORMAL.minDrops());
        assertEquals(2, OreRichness.RICH.minDrops());
        assertEquals(2, OreRichness.RICH.maxDrops());
    }

    /** The ore entries are read and validated. */
    @Test
    void oreSpec() {
        Material m = onlyOre(ore("{\"veinSize\":5,\"count\":3,\"minY\":-10,\"maxY\":40,\"biomeTag\":\"c:is_overworld\"}"));
        var ore = m.oreSpecs().get(0);
        assertEquals(5, ore.veinSize());
        assertEquals(3, ore.count());
        assertEquals(-10, ore.minY());
        assertEquals(40, ore.maxY());
        assertEquals("c:is_overworld", ore.biomeTag().orElseThrow());
        assertEquals(OreHost.DEFAULTS, ore.hosts());
        assertEquals(List.of(OreRichness.values()), ore.richness());
        assertTrue(ore.exposure().isEmpty());
        assertInvalid(ore("{\"minY\":50,\"maxY\":10}"));
        assertInvalid(ore("{\"veinSize\":0}"));
        assertInvalid(ore("{\"count\":257}"));
        assertInvalid(ore("{\"hosts\":[\"stone\",\"stone\"]}"));
        assertInvalid(ore("{\"hosts\":[]}"));
        assertInvalid(ore("{\"hosts\":[\"granite\"]}"));
        assertInvalid(ore("{\"richness\":[]}"));
        assertInvalid(ore("{\"richness\":[\"poor\",\"poor\"]}"));
        assertInvalid(ore("{\"richness\":[\"lush\"]}"));
    }

    /** The {@code ore} field is an array of at least one entry, with unique valid ids that are named by position where missing. */
    @Test
    void oreArray() {
        assertInvalid("\"ore\":{\"hosts\":[\"stone\"]}");
        assertInvalid("\"ore\":[]");
        assertInvalid(ore("{\"id\":\"a\"}", "{\"id\":\"a\",\"hosts\":[\"end\"]}"));
        assertInvalid(ore("{\"id\":\"enabled\"}"));
        assertInvalid(ore("{\"id\":\"Bad Id\"}"));
        Material m = onlyOre(ore("{\"hosts\":[\"stone\"]}", "{\"id\":\"deep\",\"hosts\":[\"deepslate\"]}", "{\"hosts\":[\"end\"]}"));
        assertEquals(List.of("ore_0", "deep", "ore_2"), m.oreSpecs().stream().map(Material.OreSpec::id).toList());
    }

    /** The ore blocks are the union of the hosts and richnesses of all entries. */
    @Test
    void oreVariantsAreTheUnionOfTheEntries() {
        Material m = onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"stone\"],\"richness\":[\"normal\"]}",
                "{\"id\":\"b\",\"hosts\":[\"stone\",\"basalt\"],\"richness\":[\"poor\",\"rich\"],\"minY\":0,\"maxY\":10}"));
        assertEquals(List.of(new Material.OreVariant(OreHost.STONE, OreRichness.POOR), new Material.OreVariant(OreHost.STONE, OreRichness.NORMAL),
                new Material.OreVariant(OreHost.STONE, OreRichness.RICH), new Material.OreVariant(OreHost.BASALT, OreRichness.POOR),
                new Material.OreVariant(OreHost.BASALT, OreRichness.RICH)), m.oreVariants());
        assertEquals("poor_basalt_m_ore", m.orePath(OreHost.BASALT, OreRichness.POOR));
        assertEquals("blackstone_m_ore", m.orePath(OreHost.BLACKSTONE, OreRichness.NORMAL));
        assertEquals("rich_blackstone_m_ore", m.orePath(OreHost.BLACKSTONE, OreRichness.RICH));
    }

    /** Without {@code dimensions} the entry generates where its hosts occur; blackstone and basalt also in the overworld. */
    @Test
    void derivedDimensions() {
        assertEquals(List.of(OreDimension.OVERWORLD), spec("{\"hosts\":[\"stone\",\"deepslate\"]}").dimensions());
        assertEquals(List.of(OreDimension.NETHER), spec("{\"hosts\":[\"nether\"]}").dimensions());
        assertEquals(List.of(OreDimension.END), spec("{\"hosts\":[\"end\"]}").dimensions());
        assertEquals(List.of(OreDimension.OVERWORLD, OreDimension.NETHER), spec("{\"hosts\":[\"blackstone\"]}").dimensions());
        assertEquals(List.of(OreDimension.OVERWORLD, OreDimension.NETHER), spec("{\"hosts\":[\"basalt\"]}").dimensions());
        assertEquals(List.of(OreDimension.OVERWORLD, OreDimension.NETHER, OreDimension.END), spec("{\"hosts\":[\"end\",\"stone\",\"nether\"]}").dimensions());
        assertEquals(List.of(OreDimension.OVERWORLD, OreDimension.NETHER, OreDimension.END), Material.OreSpec.DEFAULT.dimensions());
        var basalt = spec("{\"hosts\":[\"basalt\",\"end\"],\"dimensions\":[\"overworld\"]}");
        assertEquals(List.of(OreHost.BASALT), basalt.hosts(OreDimension.OVERWORLD));
        assertTrue(basalt.hosts(OreDimension.END).isEmpty());
        assertEquals(List.of(OreHost.STONE), spec("{\"hosts\":[\"stone\",\"nether\"]}").hosts(OreDimension.OVERWORLD));
    }

    /** A dimension needs a host that occurs in it. */
    @Test
    void dimensionAndHostMustFit() {
        assertInvalid(ore("{\"hosts\":[\"stone\"],\"dimensions\":[\"nether\"]}"));
        assertInvalid(ore("{\"hosts\":[\"stone\",\"deepslate\"],\"dimensions\":[\"overworld\",\"end\"]}"));
        assertInvalid(ore("{\"hosts\":[\"end\"],\"dimensions\":[]}"));
        assertInvalid(ore("{\"dimensions\":[\"overworld\",\"overworld\"]}"));
        assertInvalid(ore("{\"dimensions\":[\"aether\"]}"));
        onlyOre(ore("{\"hosts\":[\"stone\",\"nether\"],\"dimensions\":[\"nether\"]}"));
        onlyOre(ore("{\"hosts\":[\"basalt\"],\"dimensions\":[\"overworld\"]}"));
    }

    /** The exposure is one value or a list of {@code air}, {@code lava} and {@code water}. */
    @Test
    void exposure() {
        assertEquals(List.of(OreExposure.AIR), spec("{\"exposure\":\"air\"}").exposure());
        assertEquals(List.of(OreExposure.LAVA, OreExposure.WATER), spec("{\"exposure\":[\"lava\",\"water\"]}").exposure());
        assertTrue(spec("{}").exposure().isEmpty());
        assertInvalid(ore("{\"exposure\":\"steam\"}"));
        assertInvalid(ore("{\"exposure\":[\"air\",\"vacuum\"]}"));
        assertInvalid(ore("{\"exposure\":[\"air\",\"air\"]}"));
        assertInvalid(ore("{\"exposure\":5}"));
    }

    /** Entries of one material may share hosts and richnesses when their height, biomes or exposure differ, but not when identical. */
    @Test
    void identicalEntriesAreRejected() {
        assertInvalid(ore("{\"id\":\"a\",\"hosts\":[\"stone\"]}", "{\"id\":\"b\",\"hosts\":[\"stone\",\"deepslate\"]}"));
        assertInvalid(ore("{\"id\":\"a\",\"hosts\":[\"stone\"],\"exposure\":\"air\"}", "{\"id\":\"b\",\"hosts\":[\"stone\"],\"exposure\":[\"air\"],\"count\":1}"));
        // the surface entry of the description next to the normal one
        onlyOre(ore("{\"id\":\"deep\",\"hosts\":[\"stone\"],\"minY\":-16,\"maxY\":112}",
                "{\"id\":\"surface\",\"hosts\":[\"stone\"],\"richness\":[\"poor\"],\"minY\":40,\"maxY\":120,\"count\":6,\"veinSize\":6,\"exposure\":\"air\"}"));
        onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"stone\"],\"richness\":[\"poor\"]}", "{\"id\":\"b\",\"hosts\":[\"stone\"],\"richness\":[\"rich\"]}"));
        onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"stone\"]}", "{\"id\":\"b\",\"hosts\":[\"deepslate\"]}"));
        onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"stone\"]}", "{\"id\":\"b\",\"hosts\":[\"stone\"],\"minY\":0}"));
        onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"stone\"]}", "{\"id\":\"b\",\"hosts\":[\"stone\"],\"biomeTag\":\"c:is_forest\"}"));
        // hosts that never meet in a dimension do not collide
        onlyOre(ore("{\"id\":\"a\",\"hosts\":[\"basalt\"],\"dimensions\":[\"overworld\"]}", "{\"id\":\"b\",\"hosts\":[\"basalt\"],\"dimensions\":[\"nether\"]}"));
    }

    /** The first ore entry of a source, parsed with the defaults of the other fields. */
    private static Material.OreSpec spec(String entry) {
        return onlyOre(ore(entry)).oreSpecs().get(0);
    }

    /** The {@code ore} field of a source with these entries. */
    private static String ore(String... entries) {
        return "\"ore\":[" + String.join(",", entries) + "]";
    }

    /** Asserts that a material with this field in its source fails to load. */
    private static void assertInvalid(String field) {
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\"," + field + "}"));
    }

    /** An alloy has no raw and ore forms by default and needs at least two inputs. */
    @Test
    void alloy() {
        Material m = alloyOf("{\"id\":\"bronze\",\"color\":\"#CD7F32\",\"alloy\":{\"inputs\":[{\"material\":\"copper\",\"count\":3},{\"material\":\"tin\"}]}}", "copper", "tin");
        assertFalse(m.has("raw"));
        assertFalse(m.has("ore"));
        assertTrue(m.has("dust_block"));
        assertEquals(List.of(new Material.Alloy.Input("copper", 3), new Material.Alloy.Input("tin", 1)), m.alloy().orElseThrow().inputs());
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"copper\"}]}}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"id\":\"minecraft:iron_ingot\"},{\"material\":\"t\"}]}}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\",\"count\":10},{\"material\":\"t\"}]}}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"forms\":[\"raw\",\"ingot\"],\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":\"t\"}]}}"));
    }

    /** The inputs of an alloy are checked against all materials: unknown, the alloy itself, and more than 9 items in all. */
    @Test
    void alloyInputsAreCrossChecked() {
        var unknown = assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("a.json",
                "{\"id\":\"a\",\"tier\":\"stone\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"ghost\"},{\"material\":\"a\"}]}}"), FORMS));
        assertTrue(unknown.getMessage().contains("unknown alloy input material ghost"), unknown.getMessage());
        var self = assertThrows(IllegalArgumentException.class, () -> alloyOf(
                "{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":\"a\"}]}}", "c"));
        assertTrue(self.getMessage().contains("itself"), self.getMessage());
        var alternative = assertThrows(IllegalArgumentException.class, () -> alloyOf(
                "{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":[\"t\",\"ghost\"]}]}}", "c", "t"));
        assertTrue(alternative.getMessage().contains("ghost"), alternative.getMessage());
        var many = assertThrows(IllegalArgumentException.class, () -> alloyOf(
                "{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\",\"count\":5},{\"material\":\"t\",\"count\":5}]}}", "c", "t"));
        assertTrue(many.getMessage().contains("total 10"), many.getMessage());
        alloyOf("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\",\"count\":5},{\"material\":\"t\",\"count\":4}]}}", "c", "t");
    }

    /** An input lists alternative materials, as an array or as one id; the alternatives are the switches of the alloy. */
    @Test
    void alloyAlternatives() {
        Material m = alloyOf("{\"id\":\"steel\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"iron\"},{\"material\":[\"coal\",\"charcoal\"],\"count\":2}]}}", "iron", "coal", "charcoal");
        var inputs = m.alloy().orElseThrow().inputs();
        assertEquals(new Material.Alloy.Input("iron", 1), inputs.get(0));
        assertEquals(new Material.Alloy.Input(List.of("coal", "charcoal"), 2), inputs.get(1));
        assertFalse(inputs.get(0).hasAlternatives());
        assertTrue(inputs.get(1).hasAlternatives());
        assertEquals(Set.of("coal", "charcoal"), m.alloy().orElseThrow().alternatives());
        assertTrue(alloyOf("{\"id\":\"b\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":\"t\"}]}}", "c", "t").alloy().orElseThrow().alternatives().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":[]}]}}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"a\",\"color\":\"#000000\",\"alloy\":{\"inputs\":[{\"material\":\"c\"},{\"material\":[\"t\",\"t\"]}]}}"));
    }

    /** An external iron with one source without a mod. */
    private static final String IRON = "{\"id\":\"iron\",\"color\":\"#D8D8D8\",\"external\":[{\"forms\":{\"ingot\":{\"tag\":\"c:ingots/iron\","
            + "\"item\":\"minecraft:iron_ingot\"},\"block\":{\"tag\":\"c:storage_blocks/iron\"}}}]}";

    /** An external material only adds parts; the external metal provides ingot, nugget, blocks and ores. */
    @Test
    void external() {
        Material m = only(IRON);
        assertEquals(List.of(form("dust"), form("plate"), form("gear")), m.forms());
        assertTrue(m.isExternal());
        var source = m.external().get(0);
        assertTrue(source.mod().isEmpty());
        assertEquals("minecraft:iron_ingot", source.item("ingot").orElseThrow().toString());
        assertTrue(source.item("block").isEmpty());
        assertTrue(source.item("nugget").isEmpty());
        assertEquals("c:ingots/iron", source.tag("ingot").orElseThrow().toString());
        assertEquals("c:storage_blocks/iron", source.tag("block").orElseThrow().toString());
        assertTrue(m.oreVariants().isEmpty());
        assertFalse(only("{\"id\":\"n\",\"color\":\"#000000\"}").isExternal());
        assertTrue(only("{\"id\":\"n\",\"color\":\"#000000\",\"external\":[{}]}").externalItem("ingot").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"n\",\"color\":\"#000000\",\"external\":[{\"forms\":{\"ingot\":{\"item\":\"minecraft:iron_ingot\"}}}]}"));
        String head = "{\"id\":\"m\",\"color\":\"#000000\",";
        assertThrows(IllegalArgumentException.class, () -> only(head + "\"forms\":[\"ingot\"],\"external\":[{}]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + "\"forms\":[\"dust\",\"grains\"],\"external\":[{}]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + "\"ore\":[{}],\"external\":[{}]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + "\"external\":[{\"forms\":{\"cake\":{\"tag\":\"c:cakes\"}}}]}"));
        only(head + "\"forms\":[\"dust\",\"grains\"],\"external\":[{\"forms\":{\"nugget\":{\"tag\":\"c:nuggets/iron\",\"item\":\"minecraft:iron_nugget\"}}}]}");
    }

    /** A source of a mod: the mod id is validated, only the first source may lack a mod, mods do not repeat. */
    @Test
    void externalModSources() {
        String head = "{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"dust\"],\"external\":[";
        String vanilla = "{\"forms\":{\"ingot\":{\"tag\":\"c:ingots/m\",\"item\":\"minecraft:iron_ingot\"}}}";
        String fake = "{\"mod\":\"fakemod\",\"forms\":{\"ingot\":{\"tag\":\"fakemod:ingots/m\",\"item\":\"fakemod:m_ingot\"}}}";
        Material m = only(head + vanilla + "," + fake + "]}");
        assertEquals(2, m.external().size());
        assertEquals("fakemod", m.external().get(1).mod().orElseThrow());
        assertEquals("minecraft:iron_ingot", m.externalItem("ingot").orElseThrow().item("ingot").orElseThrow().toString());
        assertEquals("fakemod", only(head + fake + "," + vanilla.replace("{\"forms\"", "{\"mod\":\"other\",\"forms\"") + "]}").externalItem("ingot").orElseThrow().mod().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> only(head + fake + "," + vanilla + "]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + fake + "," + fake + "]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + vanilla + "," + vanilla + "]}"));
        assertThrows(IllegalArgumentException.class, () -> only(head + fake.replace("fakemod\",\"forms", "Fake Mod\",\"forms") + "]}"));
    }

    /** Forms depend on each other where recipes need it. */
    @Test
    void formRules() {
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ore\",\"ingot\"]}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"raw_block\",\"ingot\"]}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ingot\"],\"ore\":[{}]}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"cake\"]}"));
    }

    /** Tool forms are never default forms, need a {@code tools} entry, and get the durability of the tier unless the entry sets one. */
    @Test
    void toolForms() throws Exception {
        Map<String, Material> byId = shipped().stream().collect(Collectors.toMap(Material::id, m -> m));
        Form hammer = form("hammer");
        Form sandpaper = form("sandpaper");
        for (String id : List.of("stone", "iron", "steel", "diamond", "netherite")) assertTrue(byId.get(id).has(hammer), id);
        for (String id : List.of("sand", "iron", "diamond")) assertTrue(byId.get(id).has(sandpaper), id);
        assertFalse(byId.get("tin").has(hammer));
        assertFalse(byId.get("bronze").has(sandpaper));
        assertEquals(131, byId.get("stone").toolDurability(hammer));
        assertEquals(250, byId.get("iron").toolDurability(hammer));
        assertEquals(1561, byId.get("diamond").toolDurability(hammer));
        assertEquals(2031, byId.get("netherite").toolDurability(hammer));
        assertEquals(66, byId.get("sand").toolDurability(sandpaper));
        int steel = byId.get("steel").toolDurability(hammer);
        assertTrue(steel > 250 && steel < 1561, "steel hammer " + steel);
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ingot\",\"hammer\"]}"));
        assertThrows(IllegalArgumentException.class, () -> only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ingot\"],\"tools\":{\"hammer\":{\"ingredient\":\"c:ingots/m\"}}}"));
        Material m = only("{\"id\":\"m\",\"color\":\"#000000\",\"forms\":[\"ingot\",\"hammer\"],\"tools\":{\"hammer\":{\"ingredient\":\"c:ingots/m\",\"durability\":7}}}");
        assertEquals(7, m.toolDurability(hammer));
    }

    /** Tag names of the storage blocks of the three forms differ. */
    @Test
    void tagNames() {
        Material m = only("{\"id\":\"tin\",\"color\":\"#000000\"}");
        assertEquals("tin", form("block").tagName(m));
        assertEquals("raw_tin", form("raw_block").tagName(m));
        assertEquals("tin_dust", form("dust_block").tagName(m));
        assertEquals("small_dusts", form("grains").tagFolder());
    }

    /** Broken sources fail with the file name instead of being skipped. */
    @Test
    void brokenSourcesFail() {
        var noColor = assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("x.json", "{\"id\":\"x\",\"tier\":\"stone\"}"), FORMS));
        assertTrue(noColor.getMessage().contains("x.json"));
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("x.json", "{\"id\":\"x\",\"tier\":\"stone\",\"color\":\"red\"}"), FORMS));
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("x.json", "{\"id\":\"X Y\",\"tier\":\"stone\",\"color\":\"#000000\"}"), FORMS));
        var renamed = assertThrows(IllegalArgumentException.class,
                () -> Materials.parseAll(Map.of("x.json", "{\"id\":\"y\",\"tier\":\"stone\",\"color\":\"#000000\"}"), FORMS));
        assertTrue(renamed.getMessage().contains("named after"));
    }

    /** The shipped sources load, have unique ids and the expected key materials. */
    @Test
    void shippedSourcesAreValid() throws Exception {
        List<Material> materials = shipped();
        Set<String> ids = materials.stream().map(Material::id).collect(Collectors.toSet());
        assertEquals(materials.size(), ids.size());
        assertTrue(ids.contains("tin"));
        Material tin = materials.stream().filter(m -> m.id().equals("tin")).findFirst().orElseThrow();
        assertEquals(DEFAULT_FORMS, tin.forms());
        assertEquals(12, tin.oreVariants().size());
        assertEquals("Zinn", tin.displayName("de_de"));
        for (String id : List.of("aluminum", "lead", "zinc", "nickel", "silver", "platinum", "iridium", "osmium", "uranium")) {
            assertTrue(ids.contains(id), id);
            assertTrue(materials.stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow().has("ore"), id);
        }
        for (String id : List.of("bronze", "brass", "steel", "invar", "electrum", "constantan")) {
            Material alloy = materials.stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
            assertTrue(alloy.alloy().isPresent(), id);
            assertFalse(alloy.has("ore"), id);
            assertEquals(List.of("invar", "steel", "electrum").contains(id) ? ToolTier.IRON : ToolTier.STONE, alloy.tier(), id);
        }
        for (String id : List.of("iron", "copper", "gold", "diamond", "netherite", "stone", "wood")) {
            Material part = materials.stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
            assertTrue(part.isExternal(), id);
            assertFalse(part.has("ingot") || part.has("block"), id);
            assertEquals(part.has("grains"), part.external().get(0).item("nugget").isPresent(), id);
        }
    }

    /** Coal and charcoal are dust only, have no cook recipes, and name their vanilla items and blocks; "enabled" is no material id. */
    @Test
    void coalAndCharcoal() throws Exception {
        assertTrue(only("{\"id\":\"m\",\"color\":\"#000000\"}").cook());
        assertFalse(only("{\"id\":\"m\",\"color\":\"#000000\",\"cook\":false}").cook());
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("enabled.json", "{\"id\":\"enabled\",\"tier\":\"stone\",\"color\":\"#000000\"}"), FORMS));
        for (String id : List.of("coal", "charcoal")) {
            Material m = shipped().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
            assertEquals(List.of(form("dust")), m.forms(), id);
            assertFalse(m.cook(), id);
            assertEquals("minecraft:" + id, m.external().get(0).item("ingot").orElseThrow().toString());
        }
        Material charcoal = shipped().stream().filter(x -> x.id().equals("charcoal")).findFirst().orElseThrow();
        assertEquals("c:storage_blocks/charcoal", charcoal.external().get(0).tag("block").orElseThrow().toString());
        Material steel = shipped().stream().filter(x -> x.id().equals("steel")).findFirst().orElseThrow();
        assertEquals(Set.of("coal", "charcoal"), steel.alloy().orElseThrow().alternatives());
    }

    /** Every shipped material is named in every supported locale, and only in those. */
    @Test
    void shippedMaterialsAreNamedInEveryLocale() throws Exception {
        for (Material m : shipped()) {
            assertEquals(Set.copyOf(Locales.SUPPORTED), m.names().keySet(), m.id());
            for (String locale : Locales.SUPPORTED) assertFalse(m.displayName(locale).isBlank(), m.id() + " " + locale);
        }
    }

    /** A missing name fails the loading of the sources, an unsupported locale fails the parsing. */
    @Test
    void namesAreCompleteAndSupported() {
        String head = "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\",\"name\":";
        List<Material> partial = Materials.parseAll(Map.of("m.json", head + "{\"en_us\":\"M\"}}"), FORMS);
        assertThrows(IllegalArgumentException.class, () -> Materials.complete(partial));
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("m.json", head + "{\"fr_fr\":\"M\"}}"), FORMS));
        Materials.complete(Materials.parseAll(Map.of("m.json", head + "{\"en_us\":\"M\",\"de_de\":\"M\"}}"), FORMS));
    }

    private static final String MOD = "\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]";

    /** Parses sources whose conditions hold with the given mods loaded. */
    private static List<Material> withMods(Map<String, String> sources, List<Form> forms, List<Form> excluded, String... mods) {
        return Materials.parseAll(sources, forms, excluded, conditions -> Conditions.test(conditions, Set.of(mods)::contains));
    }

    /** A material whose condition fails is not loaded; it is when the condition holds; the others are not affected. */
    @Test
    void conditionsLeaveOutMaterials() {
        Map<String, String> sources = Map.of(
                "a.json", "{\"id\":\"a\",\"tier\":\"stone\",\"color\":\"#000000\"," + MOD + "}",
                "b.json", "{\"id\":\"b\",\"tier\":\"stone\",\"color\":\"#000000\"}");
        assertEquals(List.of("b"), withMods(sources, FORMS, List.of()).stream().map(Material::id).toList());
        List<Material> both = withMods(sources, FORMS, List.of(), "fakemod");
        assertEquals(List.of("a", "b"), both.stream().map(Material::id).toList());
        assertEquals(1, both.get(0).conditions().size());
        assertTrue(both.get(1).conditions().isEmpty());
        assertEquals(2, Materials.parseAll(sources, FORMS).size());
    }

    /** The conditions of a form of a material are those of the material, then those of the form, without repeats. */
    @Test
    void conditionsOfMaterialAndForm() {
        String formJson = "{\"id\":\"cog\",\"kind\":\"item\",\"order\":1,\"configKey\":\"cogs\",\"tag\":{\"parent\":\"x\"},"
                + "\"name\":{\"en_us\":\"%s X\",\"de_de\":\"%s-X\"},\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"formmod\"},"
                + "{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]}";
        List<Form> forms = Forms.parseAll(Map.of("cog.json", formJson));
        Material m = Materials.parseAll(Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\"," + MOD + "}"), forms).get(0);
        assertEquals(List.of(new ModLoadedCondition("fakemod"), new ModLoadedCondition("formmod")), m.conditions(forms.get(0)));
        Material plain = Materials.parseAll(Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\"}"), forms).get(0);
        assertEquals(forms.get(0).conditions(), plain.conditions(forms.get(0)));
        assertTrue(plain.conditions().isEmpty());
        assertTrue(Materials.parseAll(Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\"}"), List.of(form("ingot"))).get(0)
                .conditions(form("ingot")).isEmpty());
    }

    /** A source may list a left out form, which it then lacks, and keep its ore settings; an unknown form still fails. */
    @Test
    void leftOutFormsAreSkipped() {
        List<Form> loaded = FORMS.stream().filter(f -> !f.id().equals("ore") && !f.id().equals("raw") && !f.id().equals("raw_block")).toList();
        List<Form> excluded = FORMS.stream().filter(f -> !loaded.contains(f)).toList();
        Map<String, String> sources = Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\",\"forms\":[\"raw\",\"ingot\",\"ore\"],"
                + "\"ore\":[{\"hosts\":[\"stone\"]}]}");
        Material m = Materials.parseAll(sources, loaded, excluded, conditions -> true).get(0);
        assertEquals(List.of("ingot"), m.forms().stream().map(Form::id).toList());
        // an external source may name a left out form as well, but still not a form that does not exist
        Material external = Materials.parseAll(Map.of("e.json", "{\"id\":\"e\",\"tier\":\"stone\",\"color\":\"#000000\",\"forms\":[\"dust\"],"
                + "\"external\":[{\"forms\":{\"raw\":{\"tag\":\"c:raw_materials/e\"}}}]}"), loaded, excluded, conditions -> true).get(0);
        assertEquals(List.of("dust"), external.forms().stream().map(Form::id).toList());
        assertEquals(List.of("m"), Materials.parseAll(Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\"}"), loaded, excluded,
                conditions -> true).stream().map(Material::id).toList());
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("m.json",
                "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\",\"forms\":[\"nope\"]}"), loaded, excluded, conditions -> true));
        // ore settings need a hosted form: without one anywhere they fail, as before
        assertThrows(IllegalArgumentException.class, () -> Materials.parseAll(Map.of("m.json",
                "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\",\"ore\":[{\"hosts\":[\"stone\"]}]}"), loaded, List.of(), conditions -> true));
        assertEquals(List.of("m"), Materials.parseAll(Map.of("m.json",
                "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\",\"ore\":[{\"hosts\":[\"stone\"]}]}"), loaded, excluded, conditions -> true)
                .stream().map(Material::id).toList());
    }

    /** An unsupported condition fails the source with the type in the message, also for a material that is not loaded anyway. */
    @Test
    void unsupportedConditionFails() {
        var e = assertThrows(IllegalArgumentException.class, () -> withMods(Map.of("m.json", "{\"id\":\"m\",\"tier\":\"stone\",\"color\":\"#000000\","
                + "\"neoforge:conditions\":[{\"type\":\"neoforge:registered\",\"value\":\"minecraft:stone\"}]}"), FORMS, List.of()));
        assertTrue(e.getMessage().contains("neoforge:registered"), e.getMessage());
    }
}
