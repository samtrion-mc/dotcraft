package net.samtrion.dotcraft.energy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.energy.compat.jade.JadeCompat;
import net.samtrion.dotcraft.energy.compat.jei.JeiCompat;
import net.samtrion.dotcraft.energy.compat.rei.ReiCompat;
import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Entry point of the {@code .Craft: Energy} mod.
 * <p>
 * Initializes the optional JEI, Jade and REI integrations when the respective mod is present.
 */
@Mod(EnergyMod.MODID)
public class EnergyMod {
    /** Mod id of the energy mod. */
    public static final String MODID = "dotcraftenergy";

    /**
     * Constructs the mod; invoked by NeoForge on load.
     *
     * @param modEventBus  the mod-specific event bus
     * @param modContainer the container of this mod
     */
    public EnergyMod(IEventBus modEventBus, ModContainer modContainer) {
        CraftResources.LOGGER.info("{} loaded alongside {}", MODID, CraftResources.MODID);
        if (ModList.get().isLoaded("jei")) JeiCompat.init();
        if (ModList.get().isLoaded("jade")) JadeCompat.init();
        if (ModList.get().isLoaded("roughlyenoughitems")) ReiCompat.init();
    }
}
