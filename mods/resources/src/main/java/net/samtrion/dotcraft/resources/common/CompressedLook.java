package net.samtrion.dotcraft.resources.common;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * How the base texture of a compressed block maps onto the cube. Unless a source gives the textures
 * explicitly, their names derive from the block id. Only {@link #LOG} blocks keep an axis (placeable
 * sideways); {@link #SIDED} columns always stand upright.
 */
public enum CompressedLook implements StringRepresentable {
    /** One texture on all faces, {@code block/<id>}. */
    CUBE("", ""),
    /** Like {@link #CUBE}, but translucent as vanilla glass. */
    GLASS("", ""),
    /** Logs and stems, with axis: {@code <id>} on the sides, {@code <id>_top} on the ends. */
    LOG("", "_top"),
    /** Hay, bone and quartz: {@code <id>_side} on the sides, {@code <id>_top} on top and bottom. */
    SIDED("_side", "_top"),
    /**
     * Grass-like blocks: {@code <id>_side} on the sides with a tinted overlay {@code <id>_side_overlay}, a tinted
     * {@code <id>_top} on top. The tint is the fixed {@code tint} color of the base; the bottom has to be given.
     */
    GRASS("_side", "_top");

    /** Codec that reads and writes the lower case name. */
    public static final Codec<CompressedLook> CODEC = StringRepresentable.fromEnum(CompressedLook::values);

    /** Suffix of the side texture. */
    public final String side;
    /** Suffix of the top and bottom texture. */
    public final String end;

    CompressedLook(String side, String end) {
        this.side = side;
        this.end = end;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
