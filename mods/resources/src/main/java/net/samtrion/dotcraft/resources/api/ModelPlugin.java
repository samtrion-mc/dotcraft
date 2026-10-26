package net.samtrion.dotcraft.resources.api;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;

/** Contributes blockstates, models and item definitions to the model provider. */
public interface ModelPlugin {
    /**
     * Generates the models of the blocks this plugin is responsible for.
     *
     * @param blocks the block model generators
     * @param items  the item model generators
     */
    void registerModels(BlockModelGenerators blocks, ItemModelGenerators items);
}
