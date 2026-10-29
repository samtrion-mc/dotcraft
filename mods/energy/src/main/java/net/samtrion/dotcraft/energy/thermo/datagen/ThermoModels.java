package net.samtrion.dotcraft.energy.thermo.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;

/**
 * The block states and item definitions of the thermoelectric generators. The block models are source files
 * ({@code models/block/thermo_generator_tier_<n>.json}, a cube with a side and a top texture); the item shows the block model.
 */
public final class ThermoModels {
    /** Not instantiable. */
    private ThermoModels() {}

    /**
     * Writes the block state and the item definition of every generator.
     *
     * @param blocks the generators of the model provider
     */
    public static void register(BlockModelGenerators blocks) {
        ThermoBlocks.blocks().forEach(b -> {
            var model = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/" + b.getId().getPath());
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(b.get(), BlockModelGenerators.plainVariant(model)));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(model));
        });
    }
}
