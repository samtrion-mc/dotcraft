package net.samtrion.dotcraft.resources.common;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** A dimension that ores are generated in, with its default biome tag and the height range ores can be placed in. */
public enum OreDimension implements StringRepresentable {
    /** The overworld. */
    OVERWORLD("c:is_overworld", -64, 320),
    /** The nether. */
    NETHER("c:is_nether", 4, 120),
    /** The end. */
    END("c:is_end", 0, 80);

    /** Codec that reads and writes the lower case name, e.g. in the material sources. */
    public static final Codec<OreDimension> CODEC = StringRepresentable.fromEnum(OreDimension::values);

    private final String biomeTag;
    private final int lowest;
    private final int highest;

    /**
     * Creates a dimension.
     *
     * @param biomeTag id of the biome tag that ores generate in by default
     * @param lowest   lowest height ores are placed at
     * @param highest  highest height ores are placed at
     */
    OreDimension(String biomeTag, int lowest, int highest) {
        this.biomeTag = biomeTag;
        this.lowest = lowest;
        this.highest = highest;
    }

    /** @return the id of the biome tag that ores generate in unless their settings name another */
    public String biomeTag() {
        return biomeTag;
    }

    /** @return the lowest height ores are placed at */
    public int lowest() {
        return lowest;
    }

    /** @return the highest height ores are placed at */
    public int highest() {
        return highest;
    }

    /** @return the lower case name, as in the sources and in feature names */
    public String id() {
        return getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
