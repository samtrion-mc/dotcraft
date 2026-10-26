package net.samtrion.dotcraft.resources.datagen.mod;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.samtrion.dotcraft.resources.api.ModelPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** Blockstates, block models and item definitions of the own blocks of the mod. */
public final class ModModelPlugin implements ModelPlugin {
    @Override
    public void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        blocks.createTrivialCube(ResourceBlocks.CHARCOAL_BLOCK.get());
    }
}
