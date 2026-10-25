package net.samtrion.dotcraft.energy.compat.rei;

import net.samtrion.dotcraft.resources.CraftResources;

public final class ReiCompat {
    private ReiCompat() {}

    public static void init() {
        CraftResources.LOGGER.info("REI integration enabled");
    }
}
