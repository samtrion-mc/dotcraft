package net.samtrion.dotcraft.energy.wheel.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;

/**
 * The block states and item definitions of the mob wheels. The models are source files ({@code models/block/mob_wheel_tier_<n>.json}, all
 * with the parent {@code mob_wheel}): a drum on a stand, with the wheel face on both discs. The block state turns the wheel by its facing.
 */
public final class WheelModels {
    /** Not instantiable. */
    private WheelModels() {}

    /**
     * Writes the block state and the item definition of every wheel.
     *
     * @param blocks the model generators
     */
    public static void register(BlockModelGenerators blocks) {
        WheelBlocks.blocks().forEach(b -> {
            var model = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/" + b.getId().getPath());
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(b.get(), BlockModelGenerators.plainVariant(model))
                    .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(model));
        });
    }
}
