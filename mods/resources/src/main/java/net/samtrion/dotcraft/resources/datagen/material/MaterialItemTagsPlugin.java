package net.samtrion.dotcraft.resources.datagen.material;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.samtrion.dotcraft.resources.api.ItemTagsPlugin;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The convention item tags of the material items ({@code c:ingots/tin}, {@code c:small_dusts/tin},
 * {@code c:storage_blocks/tin_dust}, {@code c:ores/tin}, ...) with their parents, and for ores
 * {@code c:ores_in_ground} and {@code c:ore_rates}; the same names as the block tags of the block forms. A tag file has no
 * conditions, so the entries of a material or form with {@code neoforge:conditions} (see {@link Conditions}) are optional:
 * they are skipped where the item is not registered.
 */
public final class MaterialItemTagsPlugin implements ItemTagsPlugin {
    @Override
    public void addTags(Function<TagKey<Item>, TagAppender<Item, Item>> tags) {
        for (Material material : Materials.all()) {
            for (Form form : material.forms()) {
                boolean optional = !material.conditions(form).isEmpty();
                if (form.hosted()) {
                    for (var variant : material.oreVariants()) {
                        Item ore = ResourceItems.ore(material, variant.host(), variant.richness()).get();
                        add(tags.apply(MaterialTags.item(form, material)), ore, optional);
                        add(tags.apply(MaterialTags.itemGround(variant.host())), ore, optional);
                        add(tags.apply(MaterialTags.itemRate(variant.richness())), ore, optional);
                    }
                } else {
                    add(tags.apply(MaterialTags.item(form, material)), ResourceItems.item(material, form).get(), optional);
                }
                TagAppender<Item, Item> parent = tags.apply(MaterialTags.itemParent(form));
                if (optional) parent.addOptionalTag(MaterialTags.item(form, material));
                else parent.addTag(MaterialTags.item(form, material));
            }
        }
    }

    /**
     * Adds an item to a tag, as an optional entry where the material or form has conditions.
     *
     * @param tag      the tag
     * @param item     the item
     * @param optional whether the entry may be missing
     */
    private static void add(TagAppender<Item, Item> tag, Item item, boolean optional) {
        if (optional) tag.addOptional(item);
        else tag.add(item);
    }
}
