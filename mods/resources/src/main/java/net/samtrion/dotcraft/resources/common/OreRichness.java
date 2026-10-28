package net.samtrion.dotcraft.resources.common;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * How much an ore yields. Poor ore drops half a raw item on average (none or one), normal ore one, rich ore two;
 * a Fortune bonus applies on top as for vanilla ores, and silk touch drops the ore itself.
 */
public enum OreRichness implements StringRepresentable {
    /** Drops 0 or 1 raw item. */
    POOR("poor_", 0, 1, "sparse"),
    /** Drops 1 raw item. */
    NORMAL("", 1, 1, "singular"),
    /** Drops 2 raw items. */
    RICH("rich_", 2, 2, "dense");

    /** Codec that reads and writes the lower case name, e.g. in the material sources. */
    public static final Codec<OreRichness> CODEC = StringRepresentable.fromEnum(OreRichness::values);

    private final String prefix;
    private final int minDrops;
    private final int maxDrops;
    private final String rateTag;

    /**
     * Creates a richness.
     *
     * @param prefix   part of the registry path in front of the host
     * @param minDrops fewest drops
     * @param maxDrops most drops
     * @param rateTag  name of the tag that scales the worldgen rate
     */
    OreRichness(String prefix, int minDrops, int maxDrops, String rateTag) {
        this.prefix = prefix;
        this.minDrops = minDrops;
        this.maxDrops = maxDrops;
        this.rateTag = rateTag;
    }

    /** @return the part of the registry path in front of everything else, empty for normal ore */
    public String prefix() {
        return prefix;
    }

    /** @return the fewest raw items dropped without a bonus */
    public int minDrops() {
        return minDrops;
    }

    /** @return the most raw items dropped without a bonus */
    public int maxDrops() {
        return maxDrops;
    }

    /** @return the name in {@code c:ore_rates/<name>} */
    public String rateTag() {
        return rateTag;
    }

    /** @return the id of the overlay texture, one per richness */
    public String overlayTexture() {
        return "ore_overlay" + (this == NORMAL ? "" : "_" + name().toLowerCase(java.util.Locale.ROOT));
    }

    /** @return the lower case name, used in model ids */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
