package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.samtrion.dotcraft.resources.api.FuelPlugin;

/** The furnace fuels of all blocks; the plugins are called in the given order. */
public final class ResourceFuelProvider extends DataMapProvider {
    private final List<FuelPlugin> plugins;

    public ResourceFuelProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, FuelPlugin... plugins) {
        super(output, lookup);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var fuels = builder(NeoForgeDataMaps.FURNACE_FUELS);
        plugins.forEach(plugin -> plugin.addFuels(fuels));
    }
}
