package net.samtrion.dotcraft.energy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

/** Parsing and matching of the entity lists of the common config, without any registry. */
class EntityFilterTest {
    private static Identifier id(String text) {
        return Identifier.parse(text);
    }

    /** Ids and tags are valid; malformed entries and non-strings are not. */
    @Test
    void validatesEntries() {
        for (String ok : new String[] {"minecraft:pig", "pig", "#minecraft:zombies", "mod:some/thing"}) assertTrue(EntityFilter.isValid(ok), ok);
        for (Object bad : new Object[] {"", "#", "Bad Id", "#Bad:Tag", 5, null}) assertFalse(EntityFilter.isValid(bad), String.valueOf(bad));
    }

    /** An id entry matches that type only; a tag entry asks for the tag; an unknown id matches nothing; an empty list matches nothing. */
    @Test
    void matchesIdsAndTags() {
        var filter = EntityFilter.parse(List.of("minecraft:pig", "#minecraft:zombies", "nomod:ghost", "garbage entry"));
        assertTrue(filter.matches(id("minecraft:pig"), t -> false));
        assertFalse(filter.matches(id("minecraft:cow"), t -> false));
        assertTrue(filter.matches(id("minecraft:cow"), t -> t.equals(id("minecraft:zombies"))));
        assertFalse(filter.matches(id("minecraft:cow"), t -> t.equals(id("minecraft:skeletons"))));
        assertFalse(EntityFilter.parse(List.of()).matches(id("minecraft:pig"), t -> true));
    }
}
