package net.samtrion.dotcraft.resources.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** The furnace fuels among the own blocks of the mod; the compressed blocks take theirs from their base block. */
public final class ResourceFuelProvider extends DataMapProvider {
    /** Burn time of the charcoal block in ticks, as the coal block. */
    public static final int CHARCOAL_BLOCK_BURN_TIME = 16_000;

    public ResourceFuelProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(ResourceBlocks.CHARCOAL_BLOCK_ITEM.getId(), new FurnaceFuel(CHARCOAL_BLOCK_BURN_TIME), false);
    }
}
