package net.samtrion.dotcraft.resources.common.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.common.conditions.AlwaysCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

/** Unit tests for {@link Conditions}: the {@code neoforge:conditions} field of the sources, read before the registries exist. */
class ConditionsTest {
    /** The mods of the tests: only this one is loaded. */
    private static final Set<String> MODS = Set.of("presentmod");

    private static List<ICondition> parse(String json) {
        return Conditions.FIELD.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"neoforge:conditions\":" + json + "}"))
                .getOrThrow(IllegalArgumentException::new);
    }

    private static boolean test(String json) {
        return Conditions.test(parse(json), MODS::contains);
    }

    /** A source without the field has no conditions, which hold. */
    @Test
    void absentFieldIsEmpty() {
        List<ICondition> none = Conditions.FIELD.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow(IllegalArgumentException::new);
        assertTrue(none.isEmpty());
        assertTrue(Conditions.test(none, mod -> false));
    }

    /** {@code mod_loaded} holds for a loaded mod only; all conditions of the list must hold. */
    @Test
    void modLoaded() {
        assertTrue(test("[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"presentmod\"}]"));
        assertFalse(test("[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]"));
        assertFalse(test("[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"presentmod\"},{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]"));
        assertEquals(List.of(new ModLoadedCondition("fakemod")), parse("[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}]"));
    }

    /** The logic conditions nest, and the constants are read. */
    @Test
    void logic() {
        assertTrue(test("[{\"type\":\"neoforge:always\"}]"));
        assertFalse(test("[{\"type\":\"neoforge:never\"}]"));
        assertTrue(test("[{\"type\":\"neoforge:not\",\"value\":{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"}}]"));
        assertTrue(test("[{\"type\":\"neoforge:or\",\"values\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"},{\"type\":\"neoforge:mod_loaded\",\"modid\":\"presentmod\"}]}]"));
        assertFalse(test("[{\"type\":\"neoforge:and\",\"values\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"},{\"type\":\"neoforge:mod_loaded\",\"modid\":\"presentmod\"}]}]"));
        assertTrue(test("[{\"type\":\"neoforge:and\",\"values\":[{\"type\":\"neoforge:not\",\"value\":{\"type\":\"neoforge:never\"}},{\"type\":\"neoforge:always\"}]}]"));
    }

    /** Conditions that need tags, registries, feature flags or the config are rejected with an error that names the type. */
    @Test
    void unsupportedTypesAreRejected() {
        for (String type : new String[] {"neoforge:tag_empty", "neoforge:registered", "neoforge:feature_flags_enabled",
                "dotcraftresources:config", "dotcraftresources:max_tier", "nomod:unknown"}) {
            var e = assertThrows(IllegalArgumentException.class, () -> parse("[{\"type\":\"" + type + "\",\"tag\":\"c:x\"}]"), type);
            assertTrue(e.getMessage().contains(type), e.getMessage());
            assertTrue(e.getMessage().contains("cannot be used in a source"), e.getMessage());
        }
        assertThrows(IllegalArgumentException.class, () -> parse(
                "[{\"type\":\"neoforge:not\",\"value\":{\"type\":\"neoforge:tag_empty\",\"tag\":\"c:x\"}}]"));
    }

    /** Written back, the conditions give the source again, and an empty list is not written. */
    @Test
    void roundTrip() {
        String json = "[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"fakemod\"},{\"type\":\"neoforge:not\",\"value\":{\"type\":\"neoforge:always\"}}]";
        List<ICondition> parsed = parse(json);
        JsonElement written = Conditions.FIELD.codec().encodeStart(JsonOps.INSTANCE, parsed).getOrThrow(IllegalArgumentException::new);
        assertEquals(JsonParser.parseString("{\"neoforge:conditions\":" + json + "}"), written);
        assertEquals(JsonParser.parseString("{}"), Conditions.FIELD.codec().encodeStart(JsonOps.INSTANCE, List.of()).getOrThrow(IllegalArgumentException::new));
    }

    /** Merging keeps the order and drops repeats. */
    @Test
    void mergeDropsRepeats() {
        ICondition mod = new ModLoadedCondition("fakemod");
        ConfigCondition config = new ConfigCondition(ConfigCondition.Switch.MATERIALS);
        assertEquals(List.of(config, mod, AlwaysCondition.INSTANCE), Conditions.merge(List.of(config, mod), List.of(mod, AlwaysCondition.INSTANCE), List.of(config)));
        assertTrue(Conditions.merge(List.of(), List.of()).isEmpty());
    }

    /** The data generation counts every condition as met, so that all data is made. */
    @Test
    void datagenMeetsEverything() {
        List<ICondition> never = parse("[{\"type\":\"neoforge:never\"}]");
        assertFalse(Conditions.loaded(never));
        System.setProperty(Conditions.DATAGEN_PROPERTY, "true");
        try {
            assertTrue(Conditions.loaded(never));
        } finally {
            System.clearProperty(Conditions.DATAGEN_PROPERTY);
        }
    }
}
