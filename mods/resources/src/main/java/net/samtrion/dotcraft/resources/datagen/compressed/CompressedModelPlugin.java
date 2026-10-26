package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.Optional;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.ModelPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedLook;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;

/**
 * Blockstates, block models and item definitions of the compressed blocks.
 * <p>
 * All blocks share one of two parent models: {@code models/block/compressed.json}, the base texture with a
 * per-tier overlay that grows with the tier, and {@code compressed_tinted.json} for grass-like blocks, which adds
 * a tinted top and a tinted side overlay. A block only gets a small child model with its textures.
 */
public final class CompressedModelPlugin implements ModelPlugin {
    private static final TextureSlot OVERLAY = TextureSlot.create("overlay");
    private static final TextureSlot TINT_OVERLAY = TextureSlot.create("tint_overlay");
    private static final ModelTemplate TEMPLATE = new ModelTemplate(
            Optional.of(DataGenIds.id("block/compressed")), Optional.empty(),
            TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BOTTOM, OVERLAY);
    private static final ModelTemplate TINTED_TEMPLATE = new ModelTemplate(
            Optional.of(DataGenIds.id("block/compressed_tinted")), Optional.empty(),
            TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BOTTOM, TINT_OVERLAY, OVERLAY);

    @Override
    public void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            boolean tinted = base.look == CompressedLook.GRASS;
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = ResourceBlocks.block(base, tier).get();
                var textures = new TextureMapping()
                        .put(TextureSlot.SIDE, texture(base, base.sideTexture()))
                        .put(TextureSlot.TOP, texture(base, base.endTexture()))
                        .put(TextureSlot.BOTTOM, texture(base, base.bottomTexture()))
                        .put(OVERLAY, new Material(DataGenIds.id("block/compressed_overlay_" + tier)));
                if (tinted) textures.put(TINT_OVERLAY, new Material(base.tintOverlayTexture()));
                Identifier model = (tinted ? TINTED_TEMPLATE : TEMPLATE).create(block, textures, blocks.modelOutput);
                var variant = BlockModelGenerators.plainVariant(model);
                blocks.blockStateOutput.accept(base.look == CompressedLook.LOG
                        ? BlockModelGenerators.createAxisAlignedPillarBlock(block, variant)
                        : MultiVariantGenerator.dispatch(block, variant));
                if (tinted) {
                    blocks.itemModelOutput.accept(block.asItem(),
                            ItemModelUtils.tintedModel(model, ItemModelUtils.constantTint(base.tint())));
                } else {
                    blocks.registerSimpleItemModel(block, model);
                }
            }
        }
    }

    private static Material texture(CompressedBase base, Identifier texture) {
        var material = new Material(texture);
        return base.look == CompressedLook.GLASS ? material.withForceTranslucent(true) : material;
    }
}
