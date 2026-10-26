package net.samtrion.dotcraft.resources.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.samtrion.dotcraft.resources.CraftResources;

/** The main languages must translate the same keys. */
class LanguageParityTest {
    private static Set<String> keys(String locale) throws IOException {
        var path = "/assets/" + CraftResources.MODID + "/lang/" + locale + ".json";
        try (var in = LanguageParityTest.class.getResourceAsStream(path)) {
            return new TreeSet<>(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet());
        }
    }

    @Test
    void germanAndEnglishHaveTheSameKeys() throws IOException {
        var en = keys("en_us");
        assertFalse(en.isEmpty());
        assertEquals(en, keys("de_de"));
    }
}
