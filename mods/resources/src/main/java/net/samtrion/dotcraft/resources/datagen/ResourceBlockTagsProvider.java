package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.api.BlockTagsPlugin;

/** The block tags of all blocks; the plugins are called in the given order. */
public final class ResourceBlockTagsProvider extends BlockTagsProvider {
    private final List<BlockTagsPlugin> plugins;

    public ResourceBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, BlockTagsPlugin... plugins) {
        super(output, lookup, CraftResources.MODID);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        plugins.forEach(plugin -> plugin.addTags(this::tag));
    }
}
