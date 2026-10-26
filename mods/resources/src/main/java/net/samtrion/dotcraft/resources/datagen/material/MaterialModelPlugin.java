package net.samtrion.dotcraft.resources.datagen.material;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.ModelPlugin;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreHost;
import net.samtrion.dotcraft.resources.common.OreRichness;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * Blockstates and item definitions of the material items and blocks. Nothing is generated per material but the
 * definitions: all share the hand written grayscale models {@code models/item/material/<template>.json} and
 * {@code models/block/material/<template>.json} of their {@link Form#template() form}, and the item definition
 * and the block tint colour them (except for forms that are not {@link Form#tinted() tinted}). The ores
 * share one small model per host and richness, {@code models/block/material/<template>/<host>_<richness>.json},
 * which puts the tinted overlay on the host texture.
 */
public final class MaterialModelPlugin implements ModelPlugin {
    private static final TextureSlot HOST = TextureSlot.create("host");
    private static final TextureSlot OVERLAY = TextureSlot.create("overlay");

    @Override
    public void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        Set<Identifier> oreModels = new HashSet<>();
        for (var material : Materials.all()) {
            ItemTintSource color = ItemModelUtils.constantTint(material.color());
            for (Form form : material.forms()) {
                ItemTintSource tint = form.tinted() ? color : null;
                if (form.hosted()) {
                    for (var variant : material.oreVariants()) {
                        Identifier model = oreModel(blocks, form, variant.host(), variant.richness(), oreModels);
                        block(blocks, MaterialBlocks.ore(material, variant.host(), variant.richness()).get(), model, tint);
                    }
                } else if (form.isBlock()) {
                    block(blocks, MaterialBlocks.block(material, form).get(), DataGenIds.id("block/material/" + form.template()), tint);
                } else {
                    blocks.itemModelOutput.accept(ResourceItems.item(material, form).get(),
                            itemModel(DataGenIds.id("item/material/" + form.template()), tint));
                }
            }
        }
    }

    /** The item definition of a model, colored where there is a tint. */
    private static ItemModel.Unbaked itemModel(Identifier model, @Nullable ItemTintSource tint) {
        return tint == null ? ItemModelUtils.plainModel(model) : ItemModelUtils.tintedModel(model, tint);
    }

    /** Generates the model and item of a block. */
    private static void block(BlockModelGenerators blocks, Block block, Identifier model, @Nullable ItemTintSource tint) {
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model)));
        blocks.itemModelOutput.accept(block.asItem(), itemModel(model, tint));
    }

    /** Generates the model of a block of a hosted form. */
    private static Identifier oreModel(BlockModelGenerators blocks, Form form, OreHost host, OreRichness richness, Set<Identifier> done) {
        Identifier id = DataGenIds.id("block/material/" + form.template() + "/" + host.getSerializedName() + "_" + richness.id());
        if (done.add(id)) {
            new ModelTemplate(Optional.of(DataGenIds.id("block/material/" + form.template())), Optional.empty(), HOST, OVERLAY)
                    .create(id, new TextureMapping()
                            .put(HOST, new Material(Identifier.parse(host.texture())))
                            .put(OVERLAY, new Material(DataGenIds.id("block/material/" + richness.overlayTexture()))), blocks.modelOutput);
        }
        return id;
    }
}
