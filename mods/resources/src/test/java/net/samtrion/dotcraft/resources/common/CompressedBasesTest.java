package net.samtrion.dotcraft.resources.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.resources.tool.ToolTier;
import net.samtrion.dotcraft.resources.tool.ToolType;

/** Unit tests for {@link CompressedBases}: the source format and the shipped sources. */
class CompressedBasesTest {
    private static CompressedBase only(String json) {
        List<CompressedBase> bases = CompressedBases.parseAll(Map.of("test.json", json));
        assertEquals(1, bases.size());
        return bases.get(0);
    }

    private static List<CompressedBase> shipped() throws Exception {
        return CompressedBases.load(Path.of(CompressedBasesTest.class.getResource("/compressed").toURI()));
    }

    /** Defaults: no tool, no tier, a plain cube with textures derived from the id. */
    @Test
    void defaults() {
        CompressedBase base = only("{\"mod\":\"minecraft\",\"bases\":[{\"id\":\"dirt\"}]}");
        assertEquals("minecraft:dirt", base.id.toString());
        assertEquals(ToolType.NONE, base.tool);
        assertEquals(ToolTier.NONE, base.need);
        assertEquals(CompressedLook.CUBE, base.look);
        assertEquals("minecraft:block/dirt", base.sideTexture().toString());
        assertEquals("minecraft:block/dirt", base.endTexture().toString());
    }

    /** A source of another mod keeps that mod's namespace for ids and relative textures. */
    @Test
    void foreignSourceUsesItsNamespace() {
        CompressedBase base = only("{\"mod\":\"somemod\",\"bases\":[{\"id\":\"ruby\",\"tool\":\"pickaxe\",\"tier\":\"iron\","
                + "\"look\":\"log\",\"side\":\"block/ruby_side\",\"end\":\"minecraft:block/stone\"}]}");
        assertEquals("somemod", base.modId());
        assertEquals(ToolType.PICKAXE, base.tool);
        assertEquals(ToolTier.IRON, base.need);
        assertEquals("somemod:block/ruby_side", base.sideTexture().toString());
        assertEquals("minecraft:block/stone", base.endTexture().toString());
        assertEquals("compressed/somemod/ruby_3", base.path(3));
        assertEquals("compress/somemod/ruby_3", base.recipePath("compress", 3));
    }

    /** Looks derive their textures from the id. */
    @Test
    void looksDeriveTextures() {
        CompressedBase log = only("{\"mod\":\"m\",\"bases\":[{\"id\":\"oak_log\",\"look\":\"log\"}]}");
        assertEquals("m:block/oak_log", log.sideTexture().toString());
        assertEquals("m:block/oak_log_top", log.endTexture().toString());
        CompressedBase hay = only("{\"mod\":\"m\",\"bases\":[{\"id\":\"hay\",\"look\":\"sided\"}]}");
        assertEquals("m:block/hay_side", hay.sideTexture().toString());
        assertEquals("m:block/hay_top", hay.endTexture().toString());
    }

    /** The bottom texture defaults to the top and can be set on its own, but not for rotating blocks. */
    @Test
    void bottomTexture() {
        CompressedBase plain = only("{\"mod\":\"m\",\"bases\":[{\"id\":\"hay\",\"look\":\"sided\"}]}");
        assertEquals(plain.endTexture(), plain.bottomTexture());
        CompressedBase podzol = only("{\"mod\":\"m\",\"bases\":[{\"id\":\"p\",\"look\":\"sided\",\"bottom\":\"minecraft:block/dirt\"}]}");
        assertEquals("m:block/p_top", podzol.endTexture().toString());
        assertEquals("minecraft:block/dirt", podzol.bottomTexture().toString());
        var rotating = assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of(
                "r.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"l\",\"look\":\"log\",\"bottom\":\"block/x\"}]}")));
        assertTrue(rotating.getMessage().contains("r.json"));
    }

    /** A grass block has a tint (default plains green), a derived overlay and needs a bottom texture. */
    @Test
    void grassBlocks() {
        CompressedBase grass = only("{\"mod\":\"m\",\"bases\":[{\"id\":\"turf\",\"look\":\"grass\",\"bottom\":\"block/mud\"}]}");
        assertEquals(0xFF91BD59, grass.tint());
        assertEquals("m:block/turf_side", grass.sideTexture().toString());
        assertEquals("m:block/turf_top", grass.endTexture().toString());
        assertEquals("m:block/turf_side_overlay", grass.tintOverlayTexture().toString());
        assertEquals(0xFF112233, only("{\"mod\":\"m\",\"bases\":[{\"id\":\"t\",\"look\":\"grass\",\"bottom\":\"b\",\"tint\":\"#112233\"}]}").tint());
        assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of(
                "g.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"t\",\"look\":\"grass\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of(
                "g.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"t\",\"tint\":\"#112233\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of(
                "g.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"t\",\"look\":\"grass\",\"bottom\":\"b\",\"tint\":\"green\"}]}")));
    }

    /** A group repeats its bases per entry and replaces every placeholder. */
    @Test
    void groupsExpand() {
        List<CompressedBase> bases = CompressedBases.parseAll(Map.of("t.json", "{\"mod\":\"m\",\"groups\":[{\"each\":["
                + "{\"t\":\"a\",\"k\":\"log\"},{\"t\":\"b\",\"k\":\"stem\"}],\"bases\":["
                + "{\"id\":\"${t}_planks\"},{\"id\":\"${t}_${k}\",\"look\":\"log\",\"side\":\"block/${t}_${k}\"}]}]}"));
        assertEquals(List.of("m:a_log", "m:a_planks", "m:b_planks", "m:b_stem"),
                bases.stream().map(b -> b.id.toString()).toList());
        assertEquals("m:block/b_stem", bases.get(3).sideTexture().toString());
    }

    /** All bases are sorted by id, whatever file or order they come from. */
    @Test
    void basesAreSortedById() {
        List<CompressedBase> bases = CompressedBases.parseAll(Map.of(
                "b.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"zinc\"},{\"id\":\"apple\"}]}",
                "a.json", "{\"mod\":\"a\",\"bases\":[{\"id\":\"mango\"}]}"));
        assertEquals(List.of("a:mango", "m:apple", "m:zinc"), bases.stream().map(b -> b.id.toString()).toList());
    }

    /** Broken sources fail with the file name instead of being skipped. */
    @Test
    void brokenSourcesFail() {
        var noMod = assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of("x.json", "{\"bases\":[]}")));
        assertTrue(noMod.getMessage().contains("x.json"));
        var badTier = assertThrows(IllegalArgumentException.class,
                () -> CompressedBases.parseAll(Map.of("y.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"a\",\"tier\":\"mithril\"}]}")));
        assertTrue(badTier.getMessage().contains("y.json"));
        var duplicate = assertThrows(IllegalArgumentException.class, () -> CompressedBases.parseAll(Map.of(
                "a.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"a\"}]}", "b.json", "{\"mod\":\"m\",\"bases\":[{\"id\":\"a\"}]}")));
        assertTrue(duplicate.getMessage().contains("duplicate"));
    }

    /** The shipped sources load, have unique ids and the expected key bases. */
    @Test
    void shippedSourcesAreValid() throws Exception {
        List<CompressedBase> bases = shipped();
        Set<String> ids = bases.stream().map(b -> b.id.toString()).collect(Collectors.toSet());
        assertEquals(bases.size(), ids.size());
        assertEquals(230, bases.size());
        assertTrue(ids.containsAll(Set.of("minecraft:stone", "minecraft:oak_log", "minecraft:stripped_warped_hyphae", "minecraft:terracotta", "minecraft:lime_concrete", "minecraft:black_concrete_powder", "minecraft:red_wool", "minecraft:podzol", "minecraft:grass_block")));
        CompressedBase stone = bases.stream().filter(b -> b.id.getPath().equals("stone")).findFirst().orElseThrow();
        assertEquals(ToolType.PICKAXE, stone.tool);
        assertEquals(ToolTier.WOOD, stone.need);
        CompressedBase wood = bases.stream().filter(b -> b.id.getPath().equals("oak_wood")).findFirst().orElseThrow();
        assertEquals("minecraft:block/oak_log", wood.sideTexture().toString());
        assertEquals("minecraft:block/oak_log", wood.endTexture().toString());
    }
}
