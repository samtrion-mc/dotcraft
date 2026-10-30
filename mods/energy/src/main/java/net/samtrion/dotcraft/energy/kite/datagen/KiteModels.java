package net.samtrion.dotcraft.energy.kite.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;

/**
 * The blockstates and item definitions of the kite wind generator; called by the model provider of the energy mod. The models are
 * source files in {@code src/main/resources}: one block model per tier ({@code block/kite_generator_tier_<n>}, a shared parent with
 * the textures of the tier) and one item model each; the kite item is a flat item.
 */
public final class KiteModels {
    /** Not instantiable. */
    private KiteModels() {}

    /**
     * Writes the blockstates and item definitions.
     *
     * @param blocks the generators of the model provider
     */
    public static void register(BlockModelGenerators blocks) {
        blocks.itemModelOutput.accept(KiteBlocks.KITE.get(), ItemModelUtils.plainModel(id("item/kite")));
        KiteBlocks.blocks().forEach(b -> {
            var name = b.getId().getPath();
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(b.get(), BlockModelGenerators.plainVariant(id("block/" + name))));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(id("item/" + name)));
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(KiteBlocks.MODID, path);
    }
}
