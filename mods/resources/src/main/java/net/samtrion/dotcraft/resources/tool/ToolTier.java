package net.samtrion.dotcraft.resources.tool;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.samtrion.dotcraft.resources.CraftResources;

/**
 * A tool material in strictly ascending order of what it can mine. Vanilla tools decide per material with an
 * {@code incorrect_for_<material>_tool} tag whether a block drops; a block therefore needs a tier when it
 * is in the incorrect tag of every material below it. That is the only way to tell diamond from netherite,
 * as vanilla has no {@code needs_netherite_tool} tag.
 * <p>
 * The block lists live in one {@code needs_<material>_tool} tag per tier and the
 * {@code incorrect_for_*} tags only reference them. Stone, iron and diamond use the vanilla needs tags, wood, gold
 * and netherite those of NeoForge ({@link Tags.Blocks}); NeoForge already adds the netherite tag to every
 * incorrect tag. Copper has no common tag and uses {@code dotcraft_resources:needs_copper_tool}.
 * <p>
 * Note that the order differs from vanilla, where gold tools mine like wooden ones: gold ranks between copper
 * and iron here, and the datagen removes the vanilla entries that contradict this.
 */
public enum ToolTier implements StringRepresentable {
    /** No tool needed, the block drops when mined by hand. */
    NONE(0, null, null),
    /** Wooden tools. */
    WOOD(1, BlockTags.INCORRECT_FOR_WOODEN_TOOL, Tags.Blocks.NEEDS_WOOD_TOOL),
    /** Stone tools. */
    STONE(2, BlockTags.INCORRECT_FOR_STONE_TOOL, BlockTags.NEEDS_STONE_TOOL),
    /** Copper tools. */
    COPPER(3, BlockTags.INCORRECT_FOR_COPPER_TOOL, TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CraftResources.MODID, "needs_copper_tool"))),
    /** Golden tools. */
    GOLD(4, BlockTags.INCORRECT_FOR_GOLD_TOOL, Tags.Blocks.NEEDS_GOLD_TOOL),
    /** Iron tools. */
    IRON(5, BlockTags.INCORRECT_FOR_IRON_TOOL, BlockTags.NEEDS_IRON_TOOL),
    /** Diamond tools. */
    DIAMOND(6, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, BlockTags.NEEDS_DIAMOND_TOOL),
    /** Netherite tools. */
    NETHERITE(7, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, Tags.Blocks.NEEDS_NETHERITE_TOOL);

    /** Codec that reads and writes the lower case name, e.g. in the compressed block sources. */
    public static final com.mojang.serialization.Codec<ToolTier> CODEC = StringRepresentable.fromEnum(ToolTier::values);

    private final int rank;
    private final @Nullable TagKey<Block> incorrectTag;
    private final @Nullable TagKey<Block> needsTag;

    ToolTier(int rank, @Nullable TagKey<Block> incorrectTag, @Nullable TagKey<Block> needsTag) {
        this.rank = rank;
        this.incorrectTag = incorrectTag;
        this.needsTag = needsTag;
    }

    /**
     * Position in the mining order.
     *
     * @return the rank, {@code 0} for {@link #NONE}
     */
    public int rank() {
        return rank;
    }

    /**
     * Blocks that this material does not get a drop from.
     *
     * @return the vanilla {@code incorrect_for_<material>_tool} tag, or {@code null} for {@link #NONE}
     */
    public @Nullable TagKey<Block> incorrectTag() {
        return incorrectTag;
    }

    /**
     * The tag of blocks that need this tier.
     *
     * @return the {@code needs_<material>_tool} tag, or {@code null} for {@link #NONE}
     */
    public @Nullable TagKey<Block> needsTag() {
        return needsTag;
    }

    /**
     * Tells whether a tool of this tier gets a drop from a block that needs the given tier.
     *
     * @param requirement the tier the block needs
     * @return {@code true} if this tier is at least as good
     */
    public boolean canMine(ToolTier requirement) {
        return rank >= requirement.rank;
    }

    /**
     * The tool materials that must not get a drop from a block that needs this tier.
     *
     * @return all materials ranked below this tier, without {@link #NONE}
     */
    public Stream<ToolTier> insufficient() {
        return Stream.of(values()).filter(t -> t.incorrectTag != null && !t.canMine(this));
    }

    /**
     * Tells whether vanilla itself lists the needs tag of the given tier in this material's incorrect tag
     * (wood and gold: stone, iron, diamond; stone and copper: iron, diamond; iron: diamond).
     *
     * @param needs the tier whose needs tag is asked for
     * @return {@code true} if vanilla puts it into this material's incorrect tag
     */
    public boolean vanillaBlocks(ToolTier needs) {
        return switch (this) {
            case WOOD, GOLD -> needs == STONE || needs == IRON || needs == DIAMOND;
            case STONE, COPPER -> needs == IRON || needs == DIAMOND;
            case IRON -> needs == DIAMOND;
            default -> false;
        };
    }

    /**
     * The vanilla incorrect-tag entries that contradict our order: tiers vanilla blocks although this
     * material ranks at least as high. The datagen removes them.
     *
     * @return the tiers whose needs tag has to be removed from this material's incorrect tag
     */
    public Stream<ToolTier> contradictions() {
        return Stream.of(values()).filter(t -> vanillaBlocks(t) && canMine(t));
    }

    /**
     * The tool materials, i.e. all tiers but {@link #NONE}.
     *
     * @return the tiers that have tags
     */
    public static Stream<ToolTier> materials() {
        return Stream.of(values()).filter(t -> t.incorrectTag != null);
    }

    /** Materials the requirement climbs through; gold is skipped as its tools are no real step up. */
    private static final java.util.List<ToolTier> LADDER = java.util.List.of(WOOD, STONE, COPPER, IRON, DIAMOND, NETHERITE);

    /** Compression tiers per step of the requirement. */
    private static final int TIERS_PER_STEP = 3;

    /**
     * The tier a compressed block needs: tiers 1-3 keep the base requirement, 4-6 need one material further up
     * the ladder, 7-9 two, capped at netherite. Blocks that need no tool stay free.
     *
     * @param compression the compression tier, {@code 1..MAX}
     * @return the requirement of that tier, {@link #NONE} for {@link #NONE}
     */
    public ToolTier progressed(int compression) {
        int steps = (compression - 1) / TIERS_PER_STEP;
        if (this == NONE || steps == 0) return this;
        int at = 0;
        for (int i = 0; i < LADDER.size(); i++) if (LADDER.get(i).rank <= rank) at = i;
        return LADDER.get(Math.min(at + steps, LADDER.size() - 1));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
