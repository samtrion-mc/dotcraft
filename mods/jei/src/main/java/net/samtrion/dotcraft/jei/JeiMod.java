package net.samtrion.dotcraft.jei;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

/**
 * Entry point of the {@code .Craft: JEI} mod.
 * <p>
 * Holds the JEI plugins of all .Craft mods. The plugins are discovered by JEI itself; each one only touches the
 * classes of a .Craft mod when that mod is loaded.
 */
@Mod(JeiMod.MODID)
public final class JeiMod {
    /** Mod id of the JEI integration mod. */
    public static final String MODID = "dotcraftjei";

    /** Logger of the JEI integration mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Constructs the mod; invoked by NeoForge on load. */
    public JeiMod() {
        LOGGER.info("{} loaded", MODID);
    }
}
