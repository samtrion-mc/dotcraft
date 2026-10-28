package net.samtrion.dotcraft.energy;

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
}
