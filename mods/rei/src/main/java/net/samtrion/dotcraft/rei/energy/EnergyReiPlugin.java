package net.samtrion.dotcraft.rei.energy;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.neoforged.fml.ModList;

/**
 * REI plugin of the energy mod; discovered by REI via {@link REIPluginClient}. Adds an information display to every
 * generator item (see {@link EnergyReiInfo}). Does nothing when the energy mod is not loaded.
 */
@REIPluginClient
public class EnergyReiPlugin implements REIClientPlugin {
    /** Mod id of the optional energy mod. */
    private static final String ENERGY = "dotcraftenergy";

    /**
     * Adds the information displays of the generators.
     *
     * @param registry the REI display registry
     */
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (ModList.get().isLoaded(ENERGY)) EnergyReiInfo.register(registry);
    }
}
