package net.samtrion.dotcraft.energy.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import com.mojang.math.Quadrant;
import net.samtrion.dotcraft.energy.current.CurrentModels;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;

/**
 * The item definitions of the batteries, the solar cell and the generator blocks. The models are source files ({@code models/item/<name>.json}): the vanilla
 * potato texture with the shared {@code item/battery_overlay} (electrodes and redstone) on top.
 */
public final class EnergyModelProvider extends ModelProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public EnergyModelProvider(PackOutput output) {
        super(output, EnergyMod.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        blocks.itemModelOutput.accept(EnergyItems.POTATO_BATTERY.get(), ItemModelUtils.plainModel(model("potato_battery")));
        blocks.itemModelOutput.accept(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), ItemModelUtils.plainModel(model("rechargeable_potato_battery")));
        blocks.itemModelOutput.accept(EnergyItems.BATTERY_TIER_2.get(), ItemModelUtils.plainModel(model("battery_tier_2")));
        blocks.itemModelOutput.accept(EnergyItems.BATTERY_TIER_3.get(), ItemModelUtils.plainModel(model("battery_tier_3")));
        blocks.itemModelOutput.accept(EnergyItems.BATTERY_TIER_4.get(), ItemModelUtils.plainModel(model("battery_tier_4")));
        blocks.itemModelOutput.accept(EnergyItems.BATTERY_TIER_5.get(), ItemModelUtils.plainModel(model("battery_tier_5")));
        blocks.itemModelOutput.accept(EnergyItems.PHOTOVOLTAIC_CELL.get(), ItemModelUtils.plainModel(model("photovoltaic_cell")));
        // the block models are source files, like the item models
        var drainModel = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/power_drain");
        blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(EnergyBlocks.POWER_DRAIN.get(), BlockModelGenerators.plainVariant(drainModel)));
        blocks.itemModelOutput.accept(EnergyBlocks.POWER_DRAIN_ITEM.get(), ItemModelUtils.plainModel(drainModel));
        CurrentModels.register(blocks);
        // the vibration generators are full blocks: the block model is a source file per tier, the blockstate a single variant
        VibrationBlocks.blocks().forEach(b -> {
            var model = blockModel(b.getId().getPath());
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(b.get(), BlockModelGenerators.plainVariant(model)));
            blocks.itemModelOutput.accept(b.get().asItem(), ItemModelUtils.plainModel(model));
        });
        EnergyBlocks.blocks().forEach(b -> {
            var name = b.getId().getPath();
            var block = b.get();
            // every block takes one of three cell variants, turned, by its position: a group of panels does not look tiled; the
            // busbars of a solar panel run one way, so it is only turned half way
            Quadrant[] turns = name.startsWith("solar") ? new Quadrant[] {Quadrant.R0, Quadrant.R180} : Quadrant.values();
            var variants = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.Variant>();
            for (int v = 0; v < 3; v++) {
                for (Quadrant turn : turns) variants.add(BlockModelGenerators.plainModel(blockModel(name + "_v" + v)).withYRot(turn));
            }
            var base = BlockModelGenerators.variants(variants.toArray(net.minecraft.client.renderer.block.dispatch.Variant[]::new));
            var edge = BlockModelGenerators.plainVariant(blockModel("panel_edge_tier_" + block.tier()));
            var generator = MultiPartGenerator.multiPart(block).with(base);
            // the border of the top is an overlay per side that no other generator touches; the overlay model is the north border
            Quadrant[] sides = {Quadrant.R0, Quadrant.R90, Quadrant.R180, Quadrant.R270};
            int i = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                generator.with(new ConditionBuilder().term(GeneratorBlock.property(d), false), edge.with(VariantMutator.Y_ROT.withValue(sides[i++])));
            }
            blocks.blockStateOutput.accept(generator);
            blocks.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(model(name)));
        });
    }

    private static Identifier blockModel(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "block/" + name);
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "item/" + name);
    }
}
