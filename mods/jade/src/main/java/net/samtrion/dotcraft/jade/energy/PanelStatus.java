package net.samtrion.dotcraft.jade.energy;

import java.util.List;

/**
 * Picks the one status word of a solar or lunar panel that is shown without the details key. Free of any Minecraft type
 * so a unit test can check the priority directly.
 */
public final class PanelStatus {
    /** The statuses in the order of their priority, the first that applies wins; each has the key {@link JadeKeys#panelStatus}. */
    public static final List<String> ALL = List.of("occupied", "no_sky", "waterlogged", "thunder", "rain", "night", "day");

    /** Not instantiable. */
    private PanelStatus() {}

    /**
     * The reason the panel is limited or blocked.
     *
     * @param occupied an entity stands on the panel
     * @param sky      the panel sees the sky
     * @param water    the panel is waterlogged
     * @param weather  {@code CLEAR}, {@code RAIN} or {@code THUNDER}
     * @param height   the height of the sun or moon after tracking, 0 if it is not up
     * @param solar    sun (true) or moon (false)
     * @return one of {@link #ALL}, or null if nothing limits the panel
     */
    public static String of(boolean occupied, boolean sky, boolean water, String weather, double height, boolean solar) {
        if (occupied) return "occupied";
        if (!sky) return "no_sky";
        if (water) return "waterlogged";
        if (weather.equals("THUNDER")) return "thunder";
        if (weather.equals("RAIN")) return "rain";
        if (height <= 0) return solar ? "night" : "day";
        return null;
    }
}
