package net.samtrion.dotcraft.resources;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.conditions.ConfigCondition;
import net.samtrion.dotcraft.resources.common.conditions.MaxTierCondition;
import net.samtrion.dotcraft.resources.item.ResourceItems;
import net.samtrion.dotcraft.resources.worldgen.ResourceFeatures;

/**
 * Entry point of the {@code .Craft: Resources} mod.
 * <p>
 * Provides the shared mod id and logger that the other .Craft mods reuse.
 */
@Mod(CraftResources.MODID)
public class CraftResources {
    /** Mod id of the resources mod. */
    public static final String MODID = "dotcraftresources";

    /** Shared logger used by all .Craft mods. */
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    /**
     * Constructs the mod; invoked by NeoForge on load.
     *
     * @param modEventBus  the mod-specific event bus
     * @param modContainer the container of this mod
     */
    public CraftResources(IEventBus modEventBus, ModContainer modContainer) {
        // first: the startup config decides what the item registers below register, see CommonConfig
        modContainer.registerConfig(ModConfig.Type.STARTUP, StartupConfig.SPEC, "dotcraft/resources/startup.toml");
        ResourceBlocks.register(modEventBus);
        MaterialBlocks.register(modEventBus);
        ResourceItems.register(modEventBus);
        ResourceFeatures.register(modEventBus);
        CONDITIONS.register("max_tier", () -> MaxTierCondition.CODEC);
        CONDITIONS.register("config", () -> ConfigCondition.CODEC);
        CONDITIONS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "dotcraft/resources/common.toml");
    }
}
