package net.samtrion.dotcraft.jade;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

/**
 * Entry point of the {@code .Craft: Jade} mod.
 * <p>
 * Holds the Jade plugins of all .Craft mods. The plugins are discovered by Jade itself; each one only touches the
 * classes of a .Craft mod when that mod is loaded.
 */
@Mod(JadeMod.MODID)
public final class JadeMod {
    /** Mod id of the Jade integration mod. */
    public static final String MODID = "dotcraftjade";

    /** Logger of the Jade integration mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Constructs the mod; invoked by NeoForge on load. */
    public JadeMod() {
        LOGGER.info("{} loaded", MODID);
    }
}
