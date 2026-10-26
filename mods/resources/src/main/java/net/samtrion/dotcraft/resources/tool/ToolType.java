package net.samtrion.dotcraft.resources.tool;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

/**
 * The tool a block is mined with; each type maps to the vanilla block tag that makes the tool fast on it.
 */
public enum ToolType implements StringRepresentable {
    /** Stone, ores and metal. */
    PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
    /** Wood and wooden blocks. */
    AXE(BlockTags.MINEABLE_WITH_AXE),
    /** Dirt, sand, gravel, snow. */
    SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
    /** Hay, leaves, sculk and similar. */
    HOE(BlockTags.MINEABLE_WITH_HOE),
    /** Cobweb, bamboo and other blocks a sword cuts efficiently. */
    SWORD(BlockTags.SWORD_EFFICIENT),
    /**
     * Shears. Minecraft 26.1.2 has no block tag for them (shears list their blocks as item rules); later versions
     * add {@code shears_minor/major/extreme_breaking_speed}. Until then this uses {@code c:mineable/shear} in the
     * style of the other mineable tags; vanilla shears do not read it, only mods and tooltips that look for it.
     */
    SHEAR(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "mineable/shear"))),
    /** Mined by hand, the block gets no tool tag. */
    NONE(null);

    /** Codec that reads and writes the lower case name, e.g. in the compressed block sources. */
    public static final com.mojang.serialization.Codec<ToolType> CODEC = StringRepresentable.fromEnum(ToolType::values);

    private final @Nullable TagKey<Block> tag;

    ToolType(@Nullable TagKey<Block> tag) {
        this.tag = tag;
    }

    /**
     * The tag that makes this tool fast on a block.
     *
     * @return the block tag, or {@code null} for {@link #NONE}
     */
    public @Nullable TagKey<Block> tag() {
        return tag;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
