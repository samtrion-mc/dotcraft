package net.samtrion.dotcraft.resources.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;

/** The item tags of the own blocks of the mod, the compressed blocks have none. */
public final class ResourceItemTagsProvider extends ItemTagsProvider {
    public ResourceItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, CraftResources.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(Tags.Items.STORAGE_BLOCKS).addTag(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL_ITEM);
        tag(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL_ITEM).add(ResourceBlocks.CHARCOAL_BLOCK_ITEM.get());
    }
}
