package net.samtrion.dotcraft.energy.item;

/**
 * The item bar of a battery, kept free of game classes so that plain unit tests can check it.
 */
final class BatteryBar {
    /** Width of the item bar in pixels when it is full. */
    static final int FULL_WIDTH = 13;

    /** Not instantiable. */
    private BatteryBar() {}

    /**
     * The width of the item bar.
     *
     * @param energy   the stored FE
     * @param capacity the capacity in FE, above 0
     * @return the width in pixels, from 0 (empty) to {@link #FULL_WIDTH} (full)
     */
    static int width(int energy, int capacity) {
        return Math.clamp(Math.round((float) energy * FULL_WIDTH / capacity), 0, FULL_WIDTH);
    }
}
