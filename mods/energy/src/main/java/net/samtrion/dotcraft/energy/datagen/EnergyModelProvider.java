package net.samtrion.dotcraft.energy.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.item.EnergyItems;

/**
 * The item definitions of the batteries. The models are source files ({@code models/item/<name>.json}): the vanilla
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
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "item/" + name);
    }
}
