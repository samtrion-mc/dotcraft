package net.samtrion.dotcraft.energy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * A set of entity types written as config entries: {@code namespace:path} is one entity type, {@code #namespace:path} is an entity type
 * tag (a data pack tag, so data packs can extend a list). The entries are only parsed here, never
 * looked up in a registry, so a type or tag that does not exist (a mod that is not installed) simply matches nothing; a tag is
 * read when {@link #matches(EntityType)} asks, so it is always the tag of the current data packs.
 */
public final class EntityFilter {
    private final Set<Identifier> ids;
    private final Set<Identifier> tags;

    private EntityFilter(Set<Identifier> ids, Set<Identifier> tags) {
        this.ids = ids;
        this.tags = tags;
    }

    /**
     * Whether a config entry is well formed, the validator of the config lists.
     *
     * @param entry the entry, anything the config holds
     * @return {@code true} for {@code namespace:path} and {@code #namespace:path} with a valid resource id
     */
    public static boolean isValid(Object entry) {
        return entry instanceof String text && Identifier.tryParse(text.startsWith("#") ? text.substring(1) : text) != null && !text.equals("#") && !text.isEmpty() && !text.endsWith(":");
    }

    /**
     * Parses the entries of a config list; malformed entries are skipped (the validator keeps them out of the config anyway).
     *
     * @param entries the entries
     * @return the filter
     */
    public static EntityFilter parse(List<? extends String> entries) {
        var ids = new HashSet<Identifier>();
        var tags = new HashSet<Identifier>();
        for (String entry : entries) {
            if (!isValid(entry)) continue;
            if (entry.startsWith("#")) tags.add(Identifier.tryParse(entry.substring(1)));
            else ids.add(Identifier.tryParse(entry));
        }
        return new EntityFilter(Set.copyOf(ids), Set.copyOf(tags));
    }

    /**
     * The match without a registry: the core of {@link #matches(EntityType)}.
     *
     * @param id    the registry id of the entity type
     * @param inTag whether the type is in the entity type tag of the given id
     * @return {@code true} if the type is listed or in a listed tag
     */
    public boolean matches(Identifier id, Predicate<Identifier> inTag) {
        if (ids.contains(id)) return true;
        for (Identifier tag : tags) {
            if (inTag.test(tag)) return true;
        }
        return false;
    }

    /**
     * Whether an entity type is listed.
     *
     * @param type the type, registered
     * @return {@code true} if it is listed or in a listed tag
     */
    public boolean matches(EntityType<?> type) {
        return matches(BuiltInRegistries.ENTITY_TYPE.getKey(type),
                tag -> type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag)));
    }
}
