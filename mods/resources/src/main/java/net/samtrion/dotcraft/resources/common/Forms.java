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
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;

/**
 * The list of all {@link Form}s, read from the source files {@code forms/*.json} of this mod. A new form is one
 * more file and no code; the file name is the id of the form. The order is the {@link Form#order()}. A form whose
 * {@code neoforge:conditions} fail (see {@link Conditions#loaded}) is left out together with every form that needs it.
 */
public final class Forms {
    private static final String DIRECTORY = "forms";

    private static @Nullable Loaded all;

    /** Not instantiable. */
    private Forms() {}

    /**
     * The forms of the shipped sources that are loaded, read on first use.
     *
     * @return the forms, in creative tab order
     */
    public static List<Form> all() {
        return loaded().forms();
    }

    /**
     * The forms of the shipped sources that are not loaded, because a condition fails or a form they need is
     * not loaded; a material source may still list them.
     *
     * @return the left out forms
     */
    static List<Form> excluded() {
        return loaded().excluded();
    }

    /** Reads the shipped sources on first use. */
    private static synchronized Loaded loaded() {
        if (all == null) all = complete(parse(Sources.classpath(DIRECTORY), Conditions::loaded));
        return all;
    }

    /**
     * Looks up a form of the shipped sources.
     *
     * @param id the id of the form, e.g. {@code ingot}
     * @return the form
     * @throws IllegalArgumentException if there is no such form
     */
    public static Form get(String id) {
        return find(all(), id);
    }

    /**
     * Looks up a form in a list.
     *
     * @param forms the forms
     * @param id    the id of the form
     * @return the form
     * @throws IllegalArgumentException if there is no such form
     */
    public static Form find(List<Form> forms, String id) {
        return forms.stream().filter(f -> f.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown form \"" + id + "\""));
    }

    /**
     * Reads all source files of a directory and checks that every form is translated into every supported locale.
     *
     * @param directory the folder with the {@code *.json} sources
     * @return the forms in creative tab order
     * @throws IOException              if a file cannot be read
     * @throws IllegalArgumentException if a source is malformed or a translation is missing
     */
    public static List<Form> load(Path directory) throws IOException {
        return complete(parse(Sources.directory(directory), Conditions::loaded)).forms();
    }

    /**
     * The forms that are loaded and the ids of those that are not.
     *
     * @param forms    the forms whose conditions hold and whose required forms are loaded, in creative tab order
     * @param excluded the other forms, in creative tab order
     */
    record Loaded(List<Form> forms, List<Form> excluded) {}

    /** Checks that every loaded form has a name for every supported locale. */
    private static Loaded complete(Loaded loaded) {
        loaded.forms().forEach(Form::requireLocales);
        return loaded;
    }

    /**
     * Parses source files given by name and content and checks that they refer to each other correctly.
     *
     * @param sources the file contents by file name
     * @return the forms in creative tab order
     * @throws IllegalArgumentException if a source is malformed, its id differs from the file name, an id, order or configKey
     *                                  appears twice, a form refers to an unknown form or more than one form is hosted
     */
    static List<Form> parseAll(Map<String, String> sources) {
        return parse(sources, conditions -> true).forms();
    }

    /**
     * Parses source files like {@link #parseAll(Map)} and leaves out the forms whose conditions do not hold, and every
     * form that {@code requires} a form that is left out or names it as the {@code drop} of its block. The references
     * between forms are checked for all of them, so a source cannot refer to a form that does not exist; a conversion
     * from a form that is left out stays, as the recipes only are made of forms a material has.
     *
     * @param sources the file contents by file name
     * @param loaded  tells whether the {@code neoforge:conditions} of a source hold
     * @return the forms that are loaded and those that are not
     * @throws IllegalArgumentException as {@link #parseAll(Map)}, or if a condition is of a type that is not supported
     */
    static Loaded parse(Map<String, String> sources, Predicate<List<ICondition>> loaded) {
        List<Form> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        Set<String> configKeys = new HashSet<>();
        new TreeMap<>(sources).forEach((name, text) -> {
            Form form = Form.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(text))
                    .getOrThrow(error -> new IllegalArgumentException(name + ": " + error));
            if (!name.equals(form.id() + ".json")) {
                throw new IllegalArgumentException(name + ": the file must be named after the id \"" + form.id() + "\"");
            }
            if (!ids.add(form.id())) throw new IllegalArgumentException(name + ": duplicate form " + form.id());
            if (!orders.add(form.order())) throw new IllegalArgumentException(name + ": duplicate order " + form.order());
            if (!configKeys.add(form.configKey())) throw new IllegalArgumentException(name + ": duplicate configKey " + form.configKey());
            result.add(form);
        });
        result.sort(Comparator.comparingInt(Form::order));
        // ponytail: one hosted form, the material ore settings and Material#oreVariants assume it; lift if a second one shows up
        if (result.stream().filter(Form::hosted).count() > 1) throw new IllegalArgumentException("at most one form can be hosted");
        for (Form form : result) {
            for (String other : form.requires()) require(ids, form, other);
            for (Form.Conversion c : form.conversions()) {
                if (c.kind() == Form.Conversion.Kind.ALLOY) continue;
                require(ids, form, c.from());
                if (c.kind() == Form.Conversion.Kind.PACK && find(result, c.from()).hosted()) {
                    throw new IllegalArgumentException(form.id() + ": a \"pack\" cannot be from the hosted form " + c.from());
                }
            }
            form.block().flatMap(Form.BlockSpec::drop).ifPresent(drop -> require(ids, form, drop));
        }
        Map<String, Form> byId = new HashMap<>();
        result.forEach(f -> byId.put(f.id(), f));
        List<Form> effective = result.stream().map(f -> f.withConditions(effective(f, byId, new HashSet<>()))).toList();
        return new Loaded(effective.stream().filter(f -> loaded.test(f.conditions())).toList(),
                effective.stream().filter(f -> !loaded.test(f.conditions())).toList());
    }

    /**
     * The conditions that decide whether a form is loaded: its own, those of every form it {@code requires} and those
     * of the {@code drop} form of its block, resolved transitively. The data generation writes them into the data of the
     * form, so what is left out at runtime because of another form is left out of the data as well.
     *
     * @param form the form
     * @param byId all forms by id
     * @param seen the ids already resolved on this walk, so that a cycle ends
     * @return the conditions without repeats
     */
    private static List<ICondition> effective(Form form, Map<String, Form> byId, Set<String> seen) {
        List<ICondition> all = new ArrayList<>(form.conditions());
        if (!seen.add(form.id())) return all;
        for (String other : form.requires()) all.addAll(effective(byId.get(other), byId, seen));
        form.block().flatMap(Form.BlockSpec::drop).ifPresent(drop -> all.addAll(effective(byId.get(drop), byId, seen)));
        return Conditions.merge(all);
    }

    /** Fails if the form id is unknown. */
    private static void require(Set<String> ids, Form form, String other) {
        if (!ids.contains(other)) throw new IllegalArgumentException(form.id() + ": unknown form \"" + other + "\"");
    }
}
