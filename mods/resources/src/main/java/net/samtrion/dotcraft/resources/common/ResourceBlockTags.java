package net.samtrion.dotcraft.resources.common;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/** Convention tags ({@code c} namespace) of the resource blocks that NeoForge does not define, and the item tags of coal and charcoal. */
public final class ResourceBlockTags {
    /** Blocks of charcoal. */
    public static final TagKey<Block> STORAGE_BLOCKS_CHARCOAL =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "storage_blocks/charcoal"));

    /** Items of {@link #STORAGE_BLOCKS_CHARCOAL}. */
    public static final TagKey<Item> STORAGE_BLOCKS_CHARCOAL_ITEM =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "storage_blocks/charcoal"));

    /** Items that count as coal for the recipes of this mod; NeoForge has no tag of coal alone ({@code minecraft:coals} also holds charcoal). */
    public static final TagKey<Item> COAL =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dotcraftresources", "coal"));

    /** Items that count as charcoal for the recipes of this mod. */
    public static final TagKey<Item> CHARCOAL =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dotcraftresources", "charcoal"));

    /** Paper, for the sandpaper recipes; NeoForge has no paper tag. */
    public static final TagKey<Item> PAPERS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "papers"));

    /** Every hammer of the materials, for the crush and forge recipes of a material that needs no tier. */
    public static final TagKey<Item> HAMMERS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/hammer"));

    /** The id of the hammer form, whose items are the tools of the crush and forge recipes. */
    public static final String HAMMER_FORM = "hammer";

    private ResourceBlockTags() {}

    /**
     * The tiers that have a hammer: the distinct tiers of the materials with the hammer form, in ascending order.
     *
     * @return the tiers, e.g. stone, iron, diamond, netherite
     */
    public static List<ToolTier> hammerTiers() {
        return Materials.all().stream().filter(m -> m.has(HAMMER_FORM)).map(Material::tier).distinct()
                .sorted(Comparator.comparingInt(ToolTier::rank)).toList();
    }

    /**
     * The tag of the hammers of a tier or a better one, {@code dotcraftresources:hammers/<tier>}; generated for each of
     * the {@link #hammerTiers()} by {@code MaterialItemTagsPlugin}.
     *
     * @param tier the tier
     * @return the tag
     */
    public static TagKey<Item> hammerTier(ToolTier tier) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dotcraftresources", "hammers/" + tier.getSerializedName()));
    }

    /**
     * The tag of the hammers that a recipe on a material of the given tier needs: the hammers of the lowest tier that has a
     * hammer and is at least the given one, so a stone hammer cannot work a diamond material; the tag of every hammer for a
     * tier below the lowest hammer (none, wood, gold) and where no hammer is as good.
     *
     * @param tier the tier of the material the recipe works on
     * @return the item tag the recipe uses as its hammer
     */
    public static TagKey<Item> hammers(ToolTier tier) {
        List<ToolTier> tiers = hammerTiers();
        if (tiers.isEmpty() || tier.rank() < tiers.get(0).rank()) return HAMMERS;
        return tiers.stream().filter(t -> t.canMine(tier)).findFirst().map(ResourceBlockTags::hammerTier).orElse(HAMMERS);
    }
}
