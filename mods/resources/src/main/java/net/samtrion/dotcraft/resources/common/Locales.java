package net.samtrion.dotcraft.resources.common;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * The one place that lists the supported languages. The language providers, the material and form sources
 * and the tests all derive from {@link #SUPPORTED}, so adding a language here makes every material and form
 * that lacks a translation fail the validation.
 */
public final class Locales {
    /** English (US). */
    public static final String EN_US = "en_us";
    /** German. */
    public static final String DE_DE = "de_de";
    /** All supported locales, as in the language file names. */
    public static final List<String> SUPPORTED = List.of(EN_US, DE_DE);

    /** Not instantiable. */
    private Locales() {}

    /**
     * Codec of a map by locale that rejects locales that are not supported.
     *
     * @param value codec of the values
     * @param <V>   the value type
     * @return the codec
     */
    public static <V> Codec<Map<String, V>> keyed(Codec<V> value) {
        return Codec.unboundedMap(Codec.STRING, value).validate(map -> {
            for (String locale : map.keySet()) {
                if (!SUPPORTED.contains(locale)) {
                    return DataResult.error(() -> "unsupported locale \"" + locale + "\", supported: " + SUPPORTED);
                }
            }
            return DataResult.success(map);
        });
    }

    /**
     * Fails unless a map by locale has an entry for every supported locale.
     *
     * @param owner what the map belongs to, for the message
     * @param what  what the entries are, for the message
     * @param keys  the locales of the map
     * @throws IllegalArgumentException if a supported locale is missing
     */
    public static void requireAll(String owner, String what, Set<String> keys) {
        Set<String> missing = new TreeSet<>(SUPPORTED);
        missing.removeAll(keys);
        if (!missing.isEmpty()) throw new IllegalArgumentException(owner + ": missing " + what + " for the locales " + missing);
    }
}
