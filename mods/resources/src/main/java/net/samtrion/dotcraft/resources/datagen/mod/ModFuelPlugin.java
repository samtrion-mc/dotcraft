package net.samtrion.dotcraft.resources.datagen.mod;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.samtrion.dotcraft.resources.api.FuelPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** The furnace fuels of the own blocks of the mod; the compressed blocks take theirs from their base block. */
public final class ModFuelPlugin implements FuelPlugin {
    /** Burn time of the charcoal block in ticks, as the coal block. */
    public static final int CHARCOAL_BLOCK_BURN_TIME = 16_000;

    @Override
    public void addFuels(DataMapProvider.Builder<FurnaceFuel, Item> fuels) {
        fuels.add(ResourceBlocks.CHARCOAL_BLOCK_ITEM.getId(), new FurnaceFuel(CHARCOAL_BLOCK_BURN_TIME), false);
    }
}
