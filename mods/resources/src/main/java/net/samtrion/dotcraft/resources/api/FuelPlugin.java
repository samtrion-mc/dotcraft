package net.samtrion.dotcraft.resources.api;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;

/** Contributes furnace fuels to the fuel provider. */
public interface FuelPlugin {
    /**
     * Adds the fuels.
     *
     * @param fuels the builder of the furnace fuel data map; plugins can add to the same builder
     */
    void addFuels(DataMapProvider.Builder<FurnaceFuel, Item> fuels);
}
