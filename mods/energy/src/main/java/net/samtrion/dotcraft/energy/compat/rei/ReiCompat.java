package net.samtrion.dotcraft.energy.compat.rei;

import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Bootstrap for the optional REI integration.
 * <p>
 * Only call {@link #init()} when REI is loaded.
 */
public final class ReiCompat {
    private ReiCompat() {}

    /** Initializes the REI integration. */
    public static void init() {
        CraftResources.LOGGER.info("REI integration enabled");
    }
}
