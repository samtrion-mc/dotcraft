package net.samtrion.dotcraft.jade.energy;

import net.neoforged.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin of the energy mod; discovered by Jade via {@link WailaPlugin}. Registers one provider per generator family
 * (see {@link EnergyProviders}): the server half on the block entity, the client half on the block, so every tier is
 * covered. Does nothing when the energy mod is not loaded.
 */
@WailaPlugin
public class EnergyJadePlugin implements IWailaPlugin {
    /**
     * Registers the server data providers.
     *
     * @param registration the Jade common registration
     */
    @Override
    public void register(IWailaCommonRegistration registration) {
        if (ModList.get().isLoaded(JadeKeys.ENERGY)) {
            for (var provider : EnergyProviders.all()) registration.registerBlockDataProvider(provider, provider.entityClass());
        }
    }

    /**
     * Registers the tooltip providers.
     *
     * @param registration the Jade client registration
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (ModList.get().isLoaded(JadeKeys.ENERGY)) {
            for (var provider : EnergyProviders.all()) registration.registerBlockComponent(provider.client(), provider.blockClass());
        }
    }
}
