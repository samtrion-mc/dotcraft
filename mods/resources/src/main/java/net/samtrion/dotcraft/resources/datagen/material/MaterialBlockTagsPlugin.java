package net.samtrion.dotcraft.resources.datagen.material;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockTagsPlugin;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.tool.ToolTier;
import net.samtrion.dotcraft.resources.tool.ToolType;

/**
 * The block tags of the material blocks: the tool of the form (e.g. pickaxe, or shovel for dust blocks) and
 * the needed tool tier (the tier of the material for the forms that are tier gated), the convention tags per
 * material ({@code c:storage_blocks/tin}, {@code c:ores/tin}, ...) with their parents, and for hosted forms such
 * as the ores {@code c:ores_in_ground} and {@code c:ore_rates}. A tag file has no conditions, so the entries of a material or
 * form with {@code neoforge:conditions} (see {@link net.samtrion.dotcraft.resources.common.conditions.Conditions}) are optional: they are skipped where
 * the block is not registered.
 */
public final class MaterialBlockTagsPlugin implements BlockTagsPlugin {
    @Override
    public void addTags(Function<TagKey<Block>, TagAppender<Block, Block>> tags) {
        for (Material material : Materials.all()) {
            for (Form form : material.forms()) {
                if (!form.isBlock()) continue;
                boolean optional = !material.conditions(form).isEmpty();
                Form.BlockSpec spec = form.block().orElseThrow();
                ToolTier tier = spec.tierGated() ? material.tier() : ToolTier.NONE;
                if (form.hosted()) {
                    for (var variant : material.oreVariants()) {
                        Block ore = MaterialBlocks.ore(material, variant.host(), variant.richness()).get();
                        mining(tags, ore, spec.tool(), tier, optional);
                        add(tags.apply(MaterialTags.block(form, material)), ore, optional);
                        add(tags.apply(MaterialTags.blockGround(variant.host())), ore, optional);
                        add(tags.apply(MaterialTags.blockRate(variant.richness())), ore, optional);
                    }
                } else {
                    Block block = MaterialBlocks.block(material, form).get();
                    mining(tags, block, spec.tool(), tier, optional);
                    add(tags.apply(MaterialTags.block(form, material)), block, optional);
                }
                TagAppender<Block, Block> parent = tags.apply(MaterialTags.blockParent(form));
                if (optional) parent.addOptionalTag(MaterialTags.block(form, material));
                else parent.addTag(MaterialTags.block(form, material));
            }
        }
    }

    /** Adds the mining tags of a block. */
    private static void mining(Function<TagKey<Block>, TagAppender<Block, Block>> tags, Block block, ToolType tool, ToolTier tier,
                               boolean optional) {
        if (tool.tag() != null) add(tags.apply(tool.tag()), block, optional);
        if (tier.needsTag() != null) add(tags.apply(tier.needsTag()), block, optional);
    }

    /**
     * Adds a block to a tag, as an optional entry where the material or form has conditions.
     *
     * @param tag      the tag
     * @param block    the block
     * @param optional whether the entry may be missing
     */
    private static void add(TagAppender<Block, Block> tag, Block block, boolean optional) {
        if (optional) tag.addOptional(block);
        else tag.add(block);
    }
}
