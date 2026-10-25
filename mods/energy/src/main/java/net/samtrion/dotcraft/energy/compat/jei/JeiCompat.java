package net.samtrion.dotcraft.energy.compat.jei;

import net.samtrion.dotcraft.resources.CraftResources;

public final class JeiCompat {
    private JeiCompat() {}

    public static void init() {
        CraftResources.LOGGER.info("JEI integration enabled");
    }
}
