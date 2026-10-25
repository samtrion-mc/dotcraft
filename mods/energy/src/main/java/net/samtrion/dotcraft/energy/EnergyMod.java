package net.samtrion.dotcraft.energy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.samtrion.dotcraft.resources.CraftResources;

@Mod(EnergyMod.MODID)
public class EnergyMod {
    public static final String MODID = "dotcraft_energy";

    public EnergyMod(IEventBus modEventBus, ModContainer modContainer) {
        CraftResources.LOGGER.info("{} loaded alongside {}", MODID, CraftResources.MODID);
    }
}
