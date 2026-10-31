package net.samtrion.dotcraft.resources.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.common.Locales;

/** All supported languages must translate the same keys. */
class LanguageParityTest {
    private static Set<String> keys(String locale) throws IOException {
        var path = "/assets/" + CraftResources.MODID + "/lang/" + locale + ".json";
        try (var in = LanguageParityTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no language file for the supported locale " + locale);
            return new TreeSet<>(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet());
        }
    }

    @Test
    void allSupportedLocalesHaveTheSameKeys() throws IOException {
        var first = keys(Locales.SUPPORTED.get(0));
        assertFalse(first.isEmpty());
        for (String locale : Locales.SUPPORTED) assertEquals(first, keys(locale), locale);
    }

    /** The format placeholders ({@code %s}, {@code %1$s}, ...) of every key match between the supported locales. */
    @Test
    void allSupportedLocalesUseTheSamePlaceholders() throws IOException {
        var first = placeholders(Locales.SUPPORTED.get(0));
        for (String locale : Locales.SUPPORTED) assertEquals(first, placeholders(locale), locale);
    }

    private static java.util.Map<String, java.util.List<String>> placeholders(String locale) throws IOException {
        var path = "/assets/" + CraftResources.MODID + "/lang/" + locale + ".json";
        try (var in = LanguageParityTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no language file for the supported locale " + locale);
            var result = new java.util.TreeMap<String, java.util.List<String>>();
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet().forEach(e -> {
                var found = new java.util.ArrayList<String>();
                var m = java.util.regex.Pattern.compile("%(?:\d+\$)?[a-z]").matcher(e.getValue().getAsString());
                while (m.find()) found.add(m.group());
                java.util.Collections.sort(found);
                result.put(e.getKey(), found);
            });
            return result;
        }
    }
}
