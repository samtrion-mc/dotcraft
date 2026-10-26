package net.samtrion.dotcraft.resources.common.conditions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.AlwaysCondition;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NeverCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;

/**
 * The standard NeoForge {@code "neoforge:conditions"} field of the material and form sources, read when the mod is
 * constructed. The registry of the condition types is filled only after that, so the sources are decoded with the
 * types that need nothing but the loaded mods, which are the NeoForge ones {@code neoforge:always}, {@code never},
 * {@code not}, {@code and}, {@code or} and {@code mod_loaded}, and are evaluated here. The conditions are the NeoForge
 * records, so the data generation writes them with the normal codec. Every other type, such as the tag, registry and
 * feature flag conditions, which need data that is not there yet, and the {@code dotcraftresources:config} condition,
 * which needs the config that is built from the sources, is rejected with an error that names the type.
 * <p>
 * While the data is generated (system property {@value #DATAGEN_PROPERTY}) all conditions count as satisfied, so all
 * data is produced and carries the conditions of its material and form, see {@code Material#conditions()}.
 */
public final class Conditions {
    /** System property that the data generation runs set; the conditions of the sources are then not evaluated. */
    public static final String DATAGEN_PROPERTY = "dotcraftresources.datagen";

    /** The key of the list in a source, {@code neoforge:conditions}. */
    private static final String KEY = "neoforge:conditions";

    /** Reads one condition of the types listed in the class description. */
    private static final Codec<ICondition> CODEC = Identifier.CODEC
            .flatXmap(Conditions::supported, Conditions::supported)
            .dispatch("type", Conditions::typeOf, type -> Conditions.TYPES.get(type));

    /** The types, by id; and, or and not read their children with {@link #CODEC}. */
    private static final Map<Identifier, MapCodec<? extends ICondition>> TYPES = Map.of(
            id("always"), AlwaysCondition.CODEC,
            id("never"), NeverCondition.CODEC,
            id("mod_loaded"), ModLoadedCondition.CODEC,
            id("not"), CODEC.fieldOf("value").xmap(NotCondition::new, NotCondition::value),
            id("and"), CODEC.listOf().fieldOf("values").xmap(AndCondition::new, AndCondition::children),
            id("or"), CODEC.listOf().fieldOf("values").xmap(OrCondition::new, OrCondition::values));

    /** The optional {@code neoforge:conditions} field of a source; empty when absent, and then not written. */
    public static final MapCodec<List<ICondition>> FIELD = CODEC.listOf().optionalFieldOf(KEY, List.of());

    /** Not instantiable. */
    private Conditions() {}

    /** The id of a NeoForge condition type. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("neoforge", path);
    }

    /** The id of the type of a condition, for writing. */
    private static Identifier typeOf(ICondition condition) {
        return id(switch (condition) {
            case AlwaysCondition a -> "always";
            case NeverCondition n -> "never";
            case ModLoadedCondition m -> "mod_loaded";
            case NotCondition n -> "not";
            case AndCondition a -> "and";
            case OrCondition o -> "or";
            default -> throw new IllegalArgumentException("the condition " + condition + " is not supported in a source");
        });
    }

    /** The id of a supported condition type; the error for another one names the type. */
    private static DataResult<Identifier> supported(Identifier type) {
        return TYPES.containsKey(type) ? DataResult.success(type) : DataResult.error(() -> "the condition type " + type
                + " cannot be used in a source, as the mod is constructed before tags, registries and the config exist; use one of "
                + TYPES.keySet().stream().map(Identifier::toString).sorted().toList());
    }

    /**
     * Tells whether the data generation runs.
     *
     * @return {@code true} if the system property {@value #DATAGEN_PROPERTY} is set
     */
    public static boolean datagen() {
        return Boolean.getBoolean(DATAGEN_PROPERTY);
    }

    /**
     * Tells whether a source is loaded: all its conditions hold with the mods that are loaded, or the data is generated.
     *
     * @param conditions the conditions of the source
     * @return {@code true} if the source is to be loaded
     */
    public static boolean loaded(List<ICondition> conditions) {
        return datagen() || test(conditions, mod -> ModList.get() != null && ModList.get().isLoaded(mod));
    }

    /**
     * Evaluates conditions of the supported types.
     *
     * @param conditions the conditions, all must hold
     * @param modLoaded  tells whether a mod is loaded
     * @return {@code true} if all hold
     * @throws IllegalArgumentException if a condition is of an unsupported type
     */
    public static boolean test(List<ICondition> conditions, Predicate<String> modLoaded) {
        return conditions.stream().allMatch(c -> test(c, modLoaded));
    }

    /** Evaluates one condition. */
    private static boolean test(ICondition condition, Predicate<String> modLoaded) {
        return switch (condition) {
            case AlwaysCondition a -> true;
            case NeverCondition n -> false;
            case ModLoadedCondition m -> modLoaded.test(m.modid());
            case NotCondition n -> !test(n.value(), modLoaded);
            case AndCondition a -> test(a.children(), modLoaded);
            case OrCondition o -> o.values().stream().anyMatch(c -> test(c, modLoaded));
            default -> throw new IllegalArgumentException("the condition " + condition + " is not supported in a source");
        };
    }

    /**
     * Joins condition lists without repeats, in order.
     *
     * @param lists the lists
     * @return the conditions of all lists
     */
    @SafeVarargs
    public static List<ICondition> merge(List<? extends ICondition>... lists) {
        LinkedHashSet<ICondition> all = new LinkedHashSet<>();
        for (List<? extends ICondition> list : lists) all.addAll(list);
        return List.copyOf(all);
    }
}
