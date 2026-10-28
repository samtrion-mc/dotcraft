package net.samtrion.dotcraft.resources.datagen.mod;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.item.Items;
import net.samtrion.dotcraft.resources.api.ItemTagsPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;

/** The item tags of the own blocks of the mod and of coal, charcoal and paper, the compressed blocks have none. */
public final class ModItemTagsPlugin implements ItemTagsPlugin {
    @Override
    public void addTags(Function<TagKey<Item>, TagAppender<Item, Item>> tags) {
        tags.apply(Tags.Items.STORAGE_BLOCKS).addTag(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL_ITEM);
        tags.apply(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL_ITEM).add(ResourceBlocks.CHARCOAL_BLOCK_ITEM.get());
        tags.apply(ResourceBlockTags.COAL).add(Items.COAL);
        tags.apply(ResourceBlockTags.CHARCOAL).add(Items.CHARCOAL);
        tags.apply(ResourceBlockTags.PAPERS).add(Items.PAPER);
    }
}
