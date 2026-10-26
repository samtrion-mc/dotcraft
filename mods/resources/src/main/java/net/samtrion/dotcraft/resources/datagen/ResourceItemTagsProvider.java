package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.api.ItemTagsPlugin;

/** The item tags of all items; the plugins are called in the given order. */
public final class ResourceItemTagsProvider extends ItemTagsProvider {
    private final List<ItemTagsPlugin> plugins;

    /**
     * Creates the provider.
     *
     * @param output  the pack output
     * @param lookup  the registries
     * @param plugins the plugins
     */
    public ResourceItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ItemTagsPlugin... plugins) {
        super(output, lookup, CraftResources.MODID);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        plugins.forEach(plugin -> plugin.addTags(this::tag));
    }
}
