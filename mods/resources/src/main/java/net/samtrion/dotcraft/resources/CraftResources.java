package net.samtrion.dotcraft.resources;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;

/**
 * Entry point of the {@code .Craft: Resources} mod.
 * <p>
 * Provides the shared mod id and logger that the other .Craft mods reuse.
 */
@Mod(CraftResources.MODID)
public class CraftResources {
    /** Mod id of the resources mod. */
    public static final String MODID = "dotcraft_resources";

    /** Shared logger used by all .Craft mods. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Constructs the mod; invoked by NeoForge on load.
     *
     * @param modEventBus  the mod-specific event bus
     * @param modContainer the container of this mod
     */
    public CraftResources(IEventBus modEventBus, ModContainer modContainer) {
        CompressedBlocks.register(modEventBus);
    }
}
