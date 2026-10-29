package net.samtrion.dotcraft.energy.melt.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.melt.MeltBlock;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;

/**
 * The block states and item definitions of the melt generators. The models are source files
 * ({@code models/block/melt_generator_tier_<n>.json} and {@code ..._lit.json}), like the other blocks of the mod.
 */
public final class MeltModels {
    /** Not instantiable. */
    private MeltModels() {}

    /**
     * Writes a block state per generator that switches the model with the {@code lit} property, and an item definition that shows the unlit block.
     *
     * @param blocks the block model generators of the provider
     */
    public static void register(BlockModelGenerators blocks) {
        MeltBlocks.blocks().forEach(b -> {
            var name = b.getId().getPath();
            var cold = BlockModelGenerators.plainVariant(model(name));
            var hot = BlockModelGenerators.plainVariant(model(name + "_lit"));
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(b.get()).with(BlockModelGenerators.createBooleanModelDispatch(MeltBlock.LIT, hot, cold)));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(model(name)));
        });
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/" + name);
    }
}
