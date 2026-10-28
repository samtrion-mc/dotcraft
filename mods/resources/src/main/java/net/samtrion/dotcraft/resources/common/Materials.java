package net.samtrion.dotcraft.resources.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;

/**
 * The list of all {@link Material}s, read from the source files {@code materials/*.json} of this mod. A new
 * material is one more file and no code; the file name is the id of the material. The order is by id. Every
 * material must be named in every supported locale, see {@link Locales}. A material whose {@code neoforge:conditions} fail
 * (see {@link Conditions#loaded}) is left out.
 */
public final class Materials {
    private static final String DIRECTORY = "materials";

    private static @Nullable List<Material> all;
    private static @Nullable Map<String, List<Material>> byForm;

    /** Not instantiable. */
    private Materials() {}

    /**
     * All materials of the shipped sources, loaded on first use.
     *
     * @return every material, sorted by id
     */
    public static synchronized List<Material> all() {
        if (all == null) all = complete(parseAll(Sources.classpath(DIRECTORY), Forms.all(), Forms.excluded(), Conditions::loaded));
        return all;
    }

    /**
     * The materials that have a form, from an index built once, so that a loop over the forms does not search every
     * material for every form.
     *
     * @param form the form
     * @return the materials with the form, sorted by id
     */
    public static synchronized List<Material> withForm(Form form) {
        if (byForm == null) {
            Map<String, List<Material>> index = new HashMap<>();
            for (Material material : all()) {
                for (Form f : material.forms()) index.computeIfAbsent(f.id(), k -> new ArrayList<>()).add(material);
            }
            byForm = index;
        }
        return byForm.getOrDefault(form.id(), List.of());
    }

    /**
     * Reads all source files of a directory and checks that every material is named in every supported locale.
     *
     * @param directory the folder with the {@code *.json} sources
     * @param forms     the forms the materials refer to
     * @return the materials sorted by id
     * @throws IOException              if a file cannot be read
     * @throws IllegalArgumentException if a source is malformed, an id appears twice or a name is missing
     */
    public static List<Material> load(Path directory, List<Form> forms) throws IOException {
        return complete(parseAll(Sources.directory(directory), forms, List.of(), Conditions::loaded));
    }

    /**
     * Checks that every material has a name for every supported locale.
     *
     * @param materials the materials
     * @return the same materials
     * @throws IllegalArgumentException if a name is missing
     */
    static List<Material> complete(List<Material> materials) {
        materials.forEach(m -> Locales.requireAll(m.id(), "\"name\"", m.names().keySet()));
        return materials;
    }

    /**
     * Parses source files given by name and content.
     *
     * @param sources the file contents by file name
     * @param forms   the forms the materials refer to
     * @return the materials sorted by id
     * @throws IllegalArgumentException if a source is malformed, its id differs from the file name or an id appears twice
     */
    static List<Material> parseAll(Map<String, String> sources, List<Form> forms) {
        return parseAll(sources, forms, List.of(), conditions -> true);
    }

    /**
     * Parses source files like {@link #parseAll(Map, List)} and leaves out the materials whose conditions do not hold.
     *
     * @param sources  the file contents by file name
     * @param forms    the forms the materials refer to that are loaded
     * @param excluded the forms that are not, which a material may list and which it then does not get
     * @param loaded   tells whether the {@code neoforge:conditions} of a source hold
     * @return the materials that are loaded, sorted by id
     * @throws IllegalArgumentException as {@link #parseAll(Map, List)}, or if an alloy input is unknown, the alloy itself or the inputs total more than 9 items, or if a condition is of a type that is not supported
     */
    static List<Material> parseAll(Map<String, String> sources, List<Form> forms, List<Form> excluded,
                                   Predicate<List<ICondition>> loaded) {
        Codec<Material> codec = Material.codec(forms, excluded);
        List<Material> result = new ArrayList<>();
        List<Material> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        new TreeMap<>(sources).forEach((name, text) -> {
            Material material = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(text))
                    .getOrThrow(error -> new IllegalArgumentException(name + ": " + error));
            if (!name.equals(material.id() + ".json")) {
                throw new IllegalArgumentException(name + ": the file must be named after the id \"" + material.id() + "\"");
            }
            if (!seen.add(material.id())) throw new IllegalArgumentException(name + ": duplicate material " + material.id());
            if (material.id().equals("enabled")) throw new IllegalArgumentException(name + ": \"enabled\" is the master switch in the config, not a material id");
            all.add(material);
            if (loaded.test(material.conditions())) result.add(material);
        });
        checkAlloys(all);
        result.sort(Comparator.comparing(Material::id));
        return List.copyOf(result);
    }

    /**
     * Checks the alloy inputs against all materials, also those that are not loaded: every input material exists, is not
     * the alloy itself, and the counts of the inputs fit the 9 slots of a crafting grid.
     *
     * @param materials every parsed material
     * @throws IllegalArgumentException if an input is unknown, is the alloy or the counts total more than 9
     */
    private static void checkAlloys(List<Material> materials) {
        Set<String> ids = new HashSet<>();
        materials.forEach(m -> ids.add(m.id()));
        for (Material material : materials) {
            if (material.alloy().isEmpty()) continue;
            int total = 0;
            for (Material.Alloy.Input input : material.alloy().get().inputs()) {
                total += input.count();
                for (String id : input.materials()) {
                    if (!ids.contains(id)) throw new IllegalArgumentException(material.id() + ": unknown alloy input material " + id);
                    if (id.equals(material.id())) throw new IllegalArgumentException(material.id() + ": an alloy cannot be an input of itself");
                }
            }
            if (total > 9) throw new IllegalArgumentException(material.id() + ": the alloy inputs total " + total + " items, at most 9");
        }
    }
}
