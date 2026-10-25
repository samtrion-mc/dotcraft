package net.samtrion.dotcraft.energy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.energy.compat.jade.JadeCompat;
import net.samtrion.dotcraft.energy.compat.jei.JeiCompat;
import net.samtrion.dotcraft.energy.compat.rei.ReiCompat;
import net.samtrion.dotcraft.resources.CraftResources;

@Mod(EnergyMod.MODID)
public class EnergyMod {
    public static final String MODID = "dotcraft_energy";

    public EnergyMod(IEventBus modEventBus, ModContainer modContainer) {
        CraftResources.LOGGER.info("{} loaded alongside {}", MODID, CraftResources.MODID);
        if (ModList.get().isLoaded("jei")) JeiCompat.init();
        if (ModList.get().isLoaded("jade")) JadeCompat.init();
        if (ModList.get().isLoaded("roughlyenoughitems")) ReiCompat.init();
    }
}
