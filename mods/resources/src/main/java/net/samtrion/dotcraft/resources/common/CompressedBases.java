package net.samtrion.dotcraft.resources.common;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.common.io.CharStreams;
import com.mojang.serialization.JsonOps;

import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.samtrion.dotcraft.resources.CraftResources;

/**
 * The list of all {@link CompressedBase}s, read from the source files {@code compressed/*.json} of this mod.
 * <p>
 * A source file belongs to one mod, so supporting another mod is one more file and no code:
 * <pre>
 * {
 *   "mod": "minecraft",
 *   "bases": [ { "id": "stone", "tool": "pickaxe", "tier": "wood" } ],
 *   "groups": [ { "each": [ { "tree": "oak" } ], "bases": [ { "id": "${tree}_planks", "tool": "axe" } ] } ]
 * }
 * </pre>
 * {@code groups} repeat their {@code bases} once per {@code each} entry and replace {@code ${key}} in all
 * strings. All bases are sorted by id, which is also the order in the creative tab; the order inside the
 * files does not matter.
 */
public final class CompressedBases {
    private static final String DIRECTORY = "compressed";

    private static @Nullable List<CompressedBase> all;

    private CompressedBases() {}

    /**
     * All bases of the shipped sources, loaded on first use.
     *
     * @return every base, also those of mods that are not loaded
     */
    public static synchronized List<CompressedBase> all() {
        if (all == null) {
            Map<String, String> sources = new TreeMap<>();
            JarContents contents = ModList.get().getModFileById(CraftResources.MODID).getFile().getContents();
            contents.visitContent(DIRECTORY, (path, resource) -> {
                if (path.endsWith(".json")) {
                    try (Reader reader = resource.bufferedReader()) {
                        sources.put(path, CharStreams.toString(reader));
                    } catch (IOException e) {
                        throw new UncheckedIOException("Cannot read " + path, e);
                    }
                }
            });
            all = parseAll(sources);
        }
        return all;
    }

    /**
     * The bases whose mod is loaded, i.e. the ones that are registered and generated.
     *
     * @return the usable bases
     */
    public static Stream<CompressedBase> enabled() {
        return all().stream().filter(CompressedBase::enabled);
    }

    /**
     * Reads all source files of a directory.
     *
     * @param directory the folder with the {@code *.json} sources
     * @return the bases sorted by id
     * @throws IOException              if a file cannot be read
     * @throws IllegalArgumentException if a source is malformed or an id appears twice
     */
    public static List<CompressedBase> load(Path directory) throws IOException {
        Map<String, String> sources = new TreeMap<>();
        try (Stream<Path> list = Files.list(directory)) {
            for (Path file : list.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                sources.put(file.getFileName().toString(), Files.readString(file));
            }
        }
        return parseAll(sources);
    }

    /**
     * Parses source files given by name and content.
     *
     * @param sources the file contents by file name
     * @return the bases sorted by id
     * @throws IllegalArgumentException if a source is malformed or an id appears twice
     */
    static List<CompressedBase> parseAll(Map<String, String> sources) {
        List<CompressedBase> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        new TreeMap<>(sources).forEach((name, text) -> {
            for (CompressedBase base : parse(name, JsonParser.parseString(text))) {
                if (!seen.add(base.id.toString())) throw new IllegalArgumentException(name + ": duplicate base " + base.id);
                result.add(base);
            }
        });
        result.sort(Comparator.comparing(b -> b.id.toString()));
        return List.copyOf(result);
    }

    /**
     * Turns one source file into bases: expands the groups and decodes every entry.
     *
     * @param name the file name, for error messages
     * @param json the parsed file
     * @return the bases of this source
     * @throws IllegalArgumentException if the source is malformed
     */
    static List<CompressedBase> parse(String name, JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        if (!root.has("mod")) throw new IllegalArgumentException(name + ": missing \"mod\"");
        String mod = root.get("mod").getAsString();

        List<JsonObject> entries = new ArrayList<>();
        if (root.has("bases")) root.getAsJsonArray("bases").forEach(e -> entries.add(e.getAsJsonObject().deepCopy()));
        if (root.has("groups")) {
            for (JsonElement group : root.getAsJsonArray("groups")) {
                JsonObject g = group.getAsJsonObject();
                for (JsonElement each : g.getAsJsonArray("each")) {
                    for (JsonElement template : g.getAsJsonArray("bases")) {
                        entries.add(substitute(template.getAsJsonObject(), each.getAsJsonObject()));
                    }
                }
            }
        }

        List<CompressedBase> bases = new ArrayList<>();
        for (JsonObject entry : entries) {
            entry.addProperty("mod", mod);
            bases.add(CompressedBase.CODEC.parse(JsonOps.INSTANCE, entry)
                    .getOrThrow(error -> new IllegalArgumentException(name + ": " + error + " in " + entry)));
        }
        return bases;
    }

    private static JsonObject substitute(JsonObject template, JsonObject values) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> field : template.entrySet()) {
            String text = field.getValue().getAsString();
            for (Map.Entry<String, JsonElement> value : values.entrySet()) {
                text = text.replace("${" + value.getKey() + "}", value.getValue().getAsString());
            }
            result.add(field.getKey(), new JsonPrimitive(text));
        }
        return result;
    }
}
