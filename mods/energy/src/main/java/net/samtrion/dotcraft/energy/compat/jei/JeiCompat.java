package net.samtrion.dotcraft.energy.compat.jei;

import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Bootstrap for the optional JEI integration.
 * <p>
 * Only call {@link #init()} when JEI is loaded.
 */
public final class JeiCompat {
    private JeiCompat() {}

    /** Initializes the JEI integration. */
    public static void init() {
        CraftResources.LOGGER.info("JEI integration enabled");
    }
}
