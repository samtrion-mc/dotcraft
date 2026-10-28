package net.samtrion.dotcraft.rei;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

/**
 * Entry point of the {@code .Craft: REI} mod.
 * <p>
 * Holds the REI plugins of all .Craft mods. The plugins are discovered by REI itself; each one only touches the
 * classes of a .Craft mod when that mod is loaded.
 */
@Mod(ReiMod.MODID)
public final class ReiMod {
    /** Mod id of the REI integration mod. */
    public static final String MODID = "dotcraftrei";

    /** Logger of the REI integration mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Constructs the mod; invoked by NeoForge on load. */
    public ReiMod() {
        LOGGER.info("{} loaded", MODID);
    }
}
