package net.samtrion.dotcraft.resources.datagen;

import java.util.List;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.api.ModelPlugin;

/** Blockstates, block models and item definitions of all blocks; the plugins are called in the given order. */
public final class ResourceModelProvider extends ModelProvider {
    private final List<ModelPlugin> plugins;

    public ResourceModelProvider(PackOutput output, ModelPlugin... plugins) {
        super(output, CraftResources.MODID);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        plugins.forEach(plugin -> plugin.registerModels(blocks, items));
    }
}
