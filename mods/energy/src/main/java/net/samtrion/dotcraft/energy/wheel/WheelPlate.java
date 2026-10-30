package net.samtrion.dotcraft.energy.wheel;

/**
 * The running surface of a mob wheel, the one place that defines it. It is the top face of the tread of the model
 * ({@code models/block/mob_wheel.json}, the element from 3/1/2 to 13/15/14 for a wheel that faces north), in 16ths of a block:
 * 10 by 12 pixels at a height of 15 pixels. The collision shape of the block ends at the same height ({@link #TOP}), so a mob
 * that stands on the wheel has its feet exactly on the plate. A wheel turned east or west has the footprint turned with it.
 * Pure doubles, free of any Minecraft types, so unit tests can carry it.
 */
public final class WheelPlate {
    /** The height of the plate and of the collision shape of the block, in blocks (15 pixels). */
    public static final double TOP = 15 / 16.0;
    /** How far the feet of a mob may be off the plate vertically, in blocks. */
    public static final double FEET_TOLERANCE = 0.05;

    /** The short side of the footprint from the block edge, in blocks (3 pixels). */
    private static final double NEAR = 3 / 16.0;
    /** The long side of the footprint from the block edge, in blocks (2 pixels). */
    private static final double FAR = 2 / 16.0;

    /** Not instantiable. */
    private WheelPlate() {}

    /**
     * Whether a mob stands on the plate: its feet are within {@link #FEET_TOLERANCE} of the plate and its position is inside the
     * footprint.
     *
     * @param sideways {@code true} for a wheel that faces east or west (the footprint is turned by a quarter)
     * @param dx       the position of the mob relative to the corner of the block in x
     * @param dy       the height of its feet relative to the bottom of the block
     * @param dz       the position relative to the corner of the block in z
     * @return {@code true} if the mob stands on the running surface
     */
    public static boolean carries(boolean sideways, double dx, double dy, double dz) {
        double x = sideways ? FAR : NEAR;
        double z = sideways ? NEAR : FAR;
        return Math.abs(dy - TOP) <= FEET_TOLERANCE && dx >= x && dx <= 1 - x && dz >= z && dz <= 1 - z;
    }
}
