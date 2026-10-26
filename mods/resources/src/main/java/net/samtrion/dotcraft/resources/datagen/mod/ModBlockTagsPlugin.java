package net.samtrion.dotcraft.resources.datagen.mod;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.samtrion.dotcraft.resources.api.BlockTagsPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;

/** The block tags of the own blocks of the mod. */
public final class ModBlockTagsPlugin implements BlockTagsPlugin {
    @Override
    public void addTags(Function<TagKey<Block>, TagAppender<Block, Block>> tags) {
        tags.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ResourceBlocks.CHARCOAL_BLOCK.get());
        tags.apply(Tags.Blocks.STORAGE_BLOCKS).addTag(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL);
        tags.apply(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL).add(ResourceBlocks.CHARCOAL_BLOCK.get());
    }
}
