package net.samtrion.dotcraft.resources.api;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Contributes block tags to the block tags provider. */
public interface BlockTagsPlugin {
    /**
     * Adds the tag entries.
     *
     * @param tags opens a tag for adding entries; plugins can add to the same tag
     */
    void addTags(Function<TagKey<Block>, TagAppender<Block, Block>> tags);
}
