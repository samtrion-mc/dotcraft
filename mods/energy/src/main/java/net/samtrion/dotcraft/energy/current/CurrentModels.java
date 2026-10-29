package net.samtrion.dotcraft.energy.current;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The blockstates and item definitions of the current generators, called from the {@code EnergyModelProvider}. The block models
 * ({@code models/block/current_generator*.json}) are source files: one shared parent with the frame and the rotor, one child per
 * tier with its textures. The model is built with the flow along the north-south axis (an axle through the rotor shows it) and rotated by the {@code facing} property. The item shows the block model.
 */
public final class CurrentModels {
    /** Not instantiable. */
    private CurrentModels() {}

    /**
     * Writes the blockstate and the item definition of every registered tier.
     *
     * @param blocks the generators of the model provider
     */
    public static void register(BlockModelGenerators blocks) {
        CurrentBlocks.blocks().forEach(b -> {
            var model = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/" + b.getId().getPath());
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(b.get(), BlockModelGenerators.plainVariant(model))
                    .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(model));
        });
    }
}
