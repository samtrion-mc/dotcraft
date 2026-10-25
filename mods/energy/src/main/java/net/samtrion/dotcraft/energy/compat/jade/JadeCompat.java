package net.samtrion.dotcraft.energy.compat.jade;

import net.samtrion.dotcraft.resources.CraftResources;

public final class JadeCompat {
    private JadeCompat() {}

    public static void init() {
        CraftResources.LOGGER.info("Jade integration enabled");
    }
}
