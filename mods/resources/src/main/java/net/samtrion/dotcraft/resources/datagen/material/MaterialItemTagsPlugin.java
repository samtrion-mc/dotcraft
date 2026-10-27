package net.samtrion.dotcraft.resources.datagen.material;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.samtrion.dotcraft.resources.api.ItemTagsPlugin;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.item.ResourceItems;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * The convention item tags of the material items ({@code c:ingots/tin}, {@code c:small_dusts/tin},
 * {@code c:storage_blocks/tin_dust}, {@code c:ores/tin}, ...) with their parents, and for ores
 * {@code c:ores_in_ground} and {@code c:ore_rates}; the same names as the block tags of the block forms. A tag file has no
 * conditions, so the entries of a material or form with {@code neoforge:conditions} (see {@link Conditions}) are optional:
 * they are skipped where the item is not registered. For each tier that has a hammer there is also the tag
 * {@code dotcraftresources:hammers/<tier>} (see {@link ResourceBlockTags#hammerTier}), which holds the hammer tag
 * ({@code c:tools/hammer/<material>}) of every material of that tier or a better one, in the order of the {@link ToolTier},
 * so a recipe that needs a hammer of a tier takes that tag.
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
        addHammerTiers(tags);
    }

    /**
     * Adds the tags of the hammers of a tier or a better one: each tier that has a hammer gets the hammer tag of every
     * material with the hammer form whose tier ranks at least as high (the entry is optional where the material or its
     * hammer form has conditions).
     *
     * @param tags the tag appenders
     */
    private static void addHammerTiers(Function<TagKey<Item>, TagAppender<Item, Item>> tags) {
        Form hammer = Forms.get(ResourceBlockTags.HAMMER_FORM);
        for (ToolTier tier : ResourceBlockTags.hammerTiers()) {
            TagAppender<Item, Item> tag = tags.apply(ResourceBlockTags.hammerTier(tier));
            for (Material material : Materials.all()) {
                if (!material.has(hammer) || !material.tier().canMine(tier)) continue;
                if (material.conditions(hammer).isEmpty()) tag.addTag(MaterialTags.item(hammer, material));
                else tag.addOptionalTag(MaterialTags.item(hammer, material));
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
