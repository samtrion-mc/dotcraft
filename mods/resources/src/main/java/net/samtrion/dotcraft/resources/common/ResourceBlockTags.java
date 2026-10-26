package net.samtrion.dotcraft.resources.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Convention tags ({@code c} namespace) of the resource blocks that NeoForge does not define, and the item tags of coal and charcoal. */
public final class ResourceBlockTags {
    /** Blocks of charcoal. */
    public static final TagKey<Block> STORAGE_BLOCKS_CHARCOAL =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "storage_blocks/charcoal"));

    /** Items of {@link #STORAGE_BLOCKS_CHARCOAL}. */
    public static final TagKey<Item> STORAGE_BLOCKS_CHARCOAL_ITEM =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "storage_blocks/charcoal"));

    /** Items that count as coal for the recipes of this mod; NeoForge has no tag of coal alone ({@code minecraft:coals} also holds charcoal). */
    public static final TagKey<Item> COAL =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dotcraftresources", "coal"));

    /** Items that count as charcoal for the recipes of this mod. */
    public static final TagKey<Item> CHARCOAL =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dotcraftresources", "charcoal"));

    private ResourceBlockTags() {}
}
