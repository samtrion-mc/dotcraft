package net.samtrion.dotcraft.energy;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.energy.compat.jade.JadeCompat;
import net.samtrion.dotcraft.energy.compat.jei.JeiCompat;
import net.samtrion.dotcraft.energy.compat.rei.ReiCompat;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Entry point of the {@code .Craft: Energy} mod.
 * <p>
 * Registers the potato batteries and initializes the optional JEI, Jade and REI integrations when the respective mod is present.
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
        // first: the startup config decides what the item register registers, see EnergyStartupConfig
        modContainer.registerConfig(ModConfig.Type.STARTUP, EnergyStartupConfig.SPEC, "dotcraft/energy/startup.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, EnergyCommonConfig.SPEC, "dotcraft/energy/common.toml");
        EnergyItems.register(modEventBus);
        if (ModList.get().isLoaded("jei")) JeiCompat.init();
        if (ModList.get().isLoaded("jade")) JadeCompat.init();
        if (ModList.get().isLoaded("roughlyenoughitems")) ReiCompat.init();
    }
}
