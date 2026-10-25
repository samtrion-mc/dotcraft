package net.samtrion.dotcraft.resources;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(CraftResources.MODID)
public class CraftResources {
    public static final String MODID = "dotcraft_resources";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CraftResources(IEventBus modEventBus, ModContainer modContainer) {
    }
}
