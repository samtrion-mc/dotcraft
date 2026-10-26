package net.samtrion.dotcraft.resources.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Convention tags ({@code c} namespace) of the material items and blocks. */
public final class MaterialTags {
    /** Not instantiable. */
    private MaterialTags() {}

    /** A tag id in the {@code c} namespace. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("c", path);
    }

    /**
     * The block tag of one material and form.
     *
     * @param form     a block form
     * @param material the material
     * @return e.g. {@code c:storage_blocks/raw_tin}, {@code c:ores/tin}
     */
    public static TagKey<Block> block(Form form, Material material) {
        return TagKey.create(Registries.BLOCK, id(form.tagFolder() + "/" + form.tagName(material)));
    }

    /**
     * The item tag of one material and form.
     *
     * @param form     the form
     * @param material the material
     * @return e.g. {@code c:ingots/tin}, {@code c:small_dusts/tin}
     */
    public static TagKey<Item> item(Form form, Material material) {
        return TagKey.create(Registries.ITEM, id(form.tagFolder() + "/" + form.tagName(material)));
    }

    /**
     * The block tag that holds the tags of all materials of a form.
     *
     * @param form the form
     * @return e.g. {@code c:ores}
     */
    public static TagKey<Block> blockParent(Form form) {
        return TagKey.create(Registries.BLOCK, id(form.tagFolder()));
    }

    /**
     * The item tag that holds the tags of all materials of a form.
     *
     * @param form the form
     * @return e.g. {@code c:ingots}
     */
    public static TagKey<Item> itemParent(Form form) {
        return TagKey.create(Registries.ITEM, id(form.tagFolder()));
    }

    /**
     * Block tag of the ores in a host, {@code c:ores_in_ground/<host>}.
     *
     * @param host the ore host
     * @return the tag
     */
    public static TagKey<Block> blockGround(OreHost host) {
        return TagKey.create(Registries.BLOCK, id("ores_in_ground/" + host.groundTag()));
    }

    /**
     * Item tag of the ores in a host, {@code c:ores_in_ground/<host>}.
     *
     * @param host the ore host
     * @return the tag
     */
    public static TagKey<Item> itemGround(OreHost host) {
        return TagKey.create(Registries.ITEM, id("ores_in_ground/" + host.groundTag()));
    }

    /**
     * Block tag of the ores of a richness, {@code c:ore_rates/<rate>}.
     *
     * @param richness the richness
     * @return the tag
     */
    public static TagKey<Block> blockRate(OreRichness richness) {
        return TagKey.create(Registries.BLOCK, id("ore_rates/" + richness.rateTag()));
    }

    /**
     * Item tag of the ores of a richness, {@code c:ore_rates/<rate>}.
     *
     * @param richness the richness
     * @return the tag
     */
    public static TagKey<Item> itemRate(OreRichness richness) {
        return TagKey.create(Registries.ITEM, id("ore_rates/" + richness.rateTag()));
    }
}
