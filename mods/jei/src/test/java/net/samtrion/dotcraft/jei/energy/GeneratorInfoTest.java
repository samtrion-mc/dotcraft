package net.samtrion.dotcraft.jei.energy;

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

import net.samtrion.dotcraft.jei.JeiMod;
import net.samtrion.dotcraft.jei.energy.GeneratorInfo.Family;

/** The language files must carry every info text of {@link GeneratorInfo}, in every language, with the right number of arguments. */
class GeneratorInfoTest {
    private static final List<String> LOCALES = List.of("en_us", "de_de");
    private static final Pattern ARGUMENT = Pattern.compile("%(\\d+)\\$s");

    private static Map<String, String> lang(String locale) throws IOException {
        var path = "/assets/" + JeiMod.MODID + "/lang/" + locale + ".json";
        try (var in = GeneratorInfoTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            var map = new TreeMap<String, String>();
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            return map;
        }
    }

    private static int arguments(String text) {
        var found = new TreeSet<Integer>();
        var m = ARGUMENT.matcher(text);
        while (m.find()) found.add(Integer.parseInt(m.group(1)));
        // the arguments must be 1..n without a gap
        for (int i = 1; i <= found.size(); i++) assertTrue(found.contains(i), text);
        return found.size();
    }

    /** Every family has all its lines, and every tier of a family with perks has one, in every language. */
    @Test
    void everyFamilyHasItsTexts() throws IOException {
        for (String locale : LOCALES) {
            var lang = lang(locale);
            assertTrue(lang.containsKey(GeneratorInfo.PERK_HEADER), locale);
            for (Family family : Family.values()) {
                for (int line = 1; line <= family.lines(); line++) {
                    assertTrue(lang.containsKey(family.lineKey(line)), locale + " " + family.lineKey(line));
                }
                if (!family.hasPerks()) continue;
                for (int tier = 1; tier <= GeneratorInfo.MAX_TIER; tier++) {
                    assertTrue(lang.containsKey(family.perkKey(tier)), locale + " " + family.perkKey(tier));
                }
            }
        }
    }

    /** The line texts use exactly as many arguments as the info page passes. */
    @Test
    void lineArgumentsMatch() throws IOException {
        for (String locale : LOCALES) {
            var lang = lang(locale);
            for (Family family : Family.values()) {
                for (int line = 1; line <= family.lines(); line++) {
                    assertEquals(family.arguments(line), arguments(lang.get(family.lineKey(line))), locale + " " + family.lineKey(line));
                }
            }
            assertEquals(2, arguments(lang.get(GeneratorInfo.PERK_HEADER)));
        }
    }

    /** All languages translate the same keys, and there are no keys the page does not use. */
    @Test
    void languagesHaveTheSameKeysAndNoStrays() throws IOException {
        var first = lang(LOCALES.get(0));
        for (String locale : LOCALES) assertEquals(first.keySet(), lang(locale).keySet(), locale);
        var expected = new TreeSet<String>();
        expected.add(GeneratorInfo.PERK_HEADER);
        for (Family family : Family.values()) {
            for (int line = 1; line <= family.lines(); line++) expected.add(family.lineKey(line));
            if (family.hasPerks()) for (int tier = 1; tier <= GeneratorInfo.MAX_TIER; tier++) expected.add(family.perkKey(tier));
        }
        assertEquals(expected, new TreeSet<>(first.keySet()));
    }

    /** The keys belong to the mod's own namespace. */
    @Test
    void keysUseTheModId() {
        assertEquals("info." + JeiMod.MODID + ".", GeneratorInfo.PREFIX);
    }
}
