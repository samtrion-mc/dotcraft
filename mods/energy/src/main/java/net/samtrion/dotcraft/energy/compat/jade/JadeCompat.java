package net.samtrion.dotcraft.energy.compat.jade;

import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Bootstrap for the optional Jade integration.
 * <p>
 * Only call {@link #init()} when Jade is loaded.
 */
public final class JadeCompat {
    private JadeCompat() {}

    /** Initializes the Jade integration. */
    public static void init() {
        CraftResources.LOGGER.info("Jade integration enabled");
    }
}
