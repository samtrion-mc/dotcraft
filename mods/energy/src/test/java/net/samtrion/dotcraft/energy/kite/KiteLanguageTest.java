package net.samtrion.dotcraft.energy.kite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.resources.common.Locales;

/** The texts of the kite generator: every message and tooltip exists in all languages and takes the same arguments. */
class KiteLanguageTest {
    /** A format argument such as {@code %s} or {@code %2$s}. */
    private static final Pattern ARGUMENT = Pattern.compile("%(\\d+\\$)?[sd]");

    /** Loads a language file of the mod. */
    private static JsonObject lang(String locale) throws IOException {
        var path = "/assets/" + EnergyMod.MODID + "/lang/" + locale + ".json";
        try (var in = KiteLanguageTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no language file " + locale);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The arguments a text takes, in a stable order. */
    private static Set<String> arguments(String text) {
        var found = new TreeSet<String>();
        ARGUMENT.matcher(text).results().forEach(m -> found.add(m.group()));
        return found;
    }

    /** Every limit has a status text and every tier a perk tooltip, in every language. */
    @Test
    void everyLimitAndTierHasATextInEveryLanguage() throws IOException {
        for (String locale : Locales.SUPPORTED) {
            var lang = lang(locale);
            for (var limit : KiteMath.Limit.values()) {
                var key = "message." + EnergyMod.MODID + ".kite.limit." + limit.name().toLowerCase(Locale.ROOT);
                assertTrue(lang.has(key), locale + " lacks " + key);
            }
            for (int tier = 1; tier <= 5; tier++) assertTrue(lang.has("tooltip." + EnergyMod.MODID + ".kite.perk." + tier), locale + " lacks perk " + tier);
        }
    }

    /** A translation that drops or renames a format argument would show a wrong or missing number in the overlay. */
    @Test
    void translationsTakeTheSameArguments() throws IOException {
        var first = lang(Locales.SUPPORTED.get(0));
        for (String locale : Locales.SUPPORTED) {
            var other = lang(locale);
            for (var key : first.keySet()) {
                if (!key.contains(".kite.")) continue;
                assertEquals(arguments(first.get(key).getAsString()), arguments(other.get(key).getAsString()), locale + " " + key);
            }
        }
    }
}
