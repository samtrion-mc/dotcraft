package net.samtrion.dotcraft.resources.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/** The block an ore sits in, which decides the background texture, the name and the {@code c:ores_in_ground} tag. */
public enum OreHost implements StringRepresentable {
    /** Stone. */
    STONE("", "minecraft:block/stone", "stone"),
    /** Deepslate. */
    DEEPSLATE("deepslate_", "minecraft:block/deepslate", "deepslate"),
    /** Netherrack. */
    NETHER("nether_", "minecraft:block/netherrack", "netherrack"),
    /** End stone. */
    END("end_", "minecraft:block/end_stone", "end_stone"),
    /** Blackstone, which also occurs in the overworld. */
    BLACKSTONE("blackstone_", "minecraft:block/blackstone", "blackstone"),
    /** Basalt, which also occurs in the overworld. */
    BASALT("basalt_", "minecraft:block/basalt_side", "basalt");

    /** Codec that reads and writes the lower case name, e.g. in the material sources. */
    public static final Codec<OreHost> CODEC = StringRepresentable.fromEnum(OreHost::values);

    /** The hosts of an ore entry that names none: the four that a material has always had. */
    public static final List<OreHost> DEFAULTS = List.of(STONE, DEEPSLATE, NETHER, END);

    private final String prefix;
    private final String texture;
    private final String groundTag;

    /**
     * Creates a host.
     *
     * @param prefix    part of the registry path in front of the material
     * @param texture   name of the host texture
     * @param groundTag tag of the blocks the ore replaces
     */
    OreHost(String prefix, String texture, String groundTag) {
        this.prefix = prefix;
        this.texture = texture;
        this.groundTag = groundTag;
    }

    /** @return the part of the registry path in front of the material, empty for stone */
    public String prefix() {
        return prefix;
    }

    /** @return the id of the background texture */
    public String texture() {
        return texture;
    }

    /**
     * The dimensions the host block occurs in: stone and deepslate in the overworld, netherrack in the nether, end
     * stone in the end, blackstone and basalt in the overworld and the nether.
     *
     * @return the dimensions in their declaration order
     */
    public List<OreDimension> dimensions() {
        return switch (this) {
            case STONE, DEEPSLATE -> List.of(OreDimension.OVERWORLD);
            case NETHER -> List.of(OreDimension.NETHER);
            case END -> List.of(OreDimension.END);
            case BLACKSTONE, BASALT -> List.of(OreDimension.OVERWORLD, OreDimension.NETHER);
        };
    }

    /**
     * The dimensions an ore entry with these hosts generates in when it names none: the union of the dimensions of
     * the hosts.
     *
     * @param hosts the hosts of the entry
     * @return the dimensions in their declaration order, without repeats
     */
    public static List<OreDimension> dimensionsOf(List<OreHost> hosts) {
        List<OreDimension> result = new ArrayList<>();
        for (OreDimension dimension : OreDimension.values()) {
            if (hosts.stream().anyMatch(h -> h.dimensions().contains(dimension))) result.add(dimension);
        }
        return result;
    }

    /**
     * The rule that matches the blocks an ore replaces in this host: the {@code stone_ore_replaceables} tag for stone, the
     * {@code deepslate_ore_replaceables} tag for deepslate, the {@code base_stone_nether} tag for the nether, and the
     * block itself for end stone, blackstone and basalt.
     *
     * @return the rule of the world generation feature
     */
    public RuleTest rule() {
        return switch (this) {
            case STONE -> new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
            case DEEPSLATE -> new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            case NETHER -> new TagMatchTest(BlockTags.BASE_STONE_NETHER);
            case END -> new BlockMatchTest(Blocks.END_STONE);
            case BLACKSTONE -> new BlockMatchTest(Blocks.BLACKSTONE);
            case BASALT -> new BlockMatchTest(Blocks.BASALT);
        };
    }

    /** @return the name in {@code c:ores_in_ground/<name>} */
    public String groundTag() {
        return groundTag;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
