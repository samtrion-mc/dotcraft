package net.samtrion.dotcraft.jade.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.samtrion.dotcraft.jade.JadeMod;

/** The language files must carry every key of {@link JadeKeys}, in every language, and nothing of the energy mod's lines beyond them. */
class JadeKeysTest {
    private static final List<String> LOCALES = List.of("en_us", "de_de");
    private static final Pattern ARGUMENT = Pattern.compile("%\\d+\\$s");

    private static Map<String, String> lang(String locale) throws IOException {
        var path = "/assets/" + JadeMod.MODID + "/lang/" + locale + ".json";
        try (var in = JadeKeysTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            var map = new TreeMap<String, String>();
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            return map;
        }
    }

    private static long arguments(String text) {
        return ARGUMENT.matcher(text).results().map(r -> r.group()).distinct().count();
    }

    /** Every key is translated in every language. */
    @Test
    void everyKeyIsTranslated() throws IOException {
        for (String locale : LOCALES) {
            var lang = lang(locale);
            for (String key : JadeKeys.all()) assertTrue(lang.containsKey(key), locale + " " + key);
        }
    }

    /** The families have a provider each and the keys are distinct. */
    @Test
    void keysAreDistinctAndEveryFamilyHasAConfigEntry() {
        var all = JadeKeys.all();
        assertEquals(all.size(), new TreeSet<>(all).size());
        for (String family : JadeKeys.FAMILIES) assertTrue(all.contains(JadeKeys.config(family)), family);
    }

    /** All languages have the same keys, no energy line is left over, and a line uses the same number of arguments in each language. */
    @Test
    void languagesAgree() throws IOException {
        var first = lang(LOCALES.get(0));
        var used = new TreeSet<>(JadeKeys.all());
        for (String locale : LOCALES) {
            var lang = lang(locale);
            assertEquals(first.keySet(), lang.keySet(), locale);
            for (String key : lang.keySet()) {
                if (key.startsWith(JadeKeys.PREFIX) || key.startsWith("config.jade.plugin_" + JadeKeys.ENERGY)) assertTrue(used.contains(key), "unused " + key);
                assertEquals(arguments(first.get(key)), arguments(lang.get(key)), locale + " " + key);
            }
        }
    }
}
