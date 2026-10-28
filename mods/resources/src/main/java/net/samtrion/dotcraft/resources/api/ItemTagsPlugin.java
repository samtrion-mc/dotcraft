package net.samtrion.dotcraft.resources.api;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Contributes item tags to the item tags provider. */
public interface ItemTagsPlugin {
    /**
     * Adds the tag entries.
     *
     * @param tags opens a tag for adding entries; plugins can add to the same tag
     */
    void addTags(Function<TagKey<Item>, TagAppender<Item, Item>> tags);
}
