package net.samtrion.dotcraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.samtrion.dotcraft.resources.common.Locales;

/** All supported languages must translate the same keys. */
class LanguageParityTest {
    private static Set<String> keys(String locale) throws IOException {
        var path = "/assets/" + EnergyMod.MODID + "/lang/" + locale + ".json";
        try (var in = LanguageParityTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no language file for the supported locale " + locale);
            return new TreeSet<>(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet());
        }
    }

    /** Every supported locale has the keys of the first one. */
    @Test
    void allSupportedLocalesHaveTheSameKeys() throws IOException {
        var first = keys(Locales.SUPPORTED.get(0));
        assertFalse(first.isEmpty());
        for (String locale : Locales.SUPPORTED) assertEquals(first, keys(locale), locale);
    }

    /** Every translated string takes the same placeholders (count and kind: tooltip arguments such as peak, refill, radius) in all supported locales. */
    @Test
    void placeholdersMatchInAllLocales() throws IOException {
        var reference = strings(Locales.SUPPORTED.get(0));
        for (String locale : Locales.SUPPORTED) {
            var other = strings(locale);
            for (var entry : reference.entrySet()) {
                assertEquals(placeholders(entry.getValue()), placeholders(other.get(entry.getKey())), locale + ": " + entry.getKey());
            }
        }
    }

    /** The kinds of the placeholders of a string, sorted, without their position index. */
    private static java.util.List<String> placeholders(String text) {
        return Pattern.compile("%(?:[0-9]+[$])?([sd])").matcher(text).results().map(r -> r.group(1)).sorted().toList();
    }

    private static Map<String, String> strings(String locale) throws IOException {
        try (var in = LanguageParityTest.class.getResourceAsStream("/assets/" + EnergyMod.MODID + "/lang/" + locale + ".json")) {
            assertNotNull(in, locale);
            var map = new java.util.TreeMap<String, String>();
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()
                    .forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            return map;
        }
    }
}
