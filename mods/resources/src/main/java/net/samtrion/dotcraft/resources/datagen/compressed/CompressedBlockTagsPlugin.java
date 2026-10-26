package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.function.Function;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockTagsPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * The tool and tool tier tags of the compressed blocks, and the links between the tiers. The required tool tier
 * rises with the compression tier, see {@link CompressedBase#required(int)}.
 */
public final class CompressedBlockTagsPlugin implements BlockTagsPlugin {
    @Override
    public void addTags(Function<TagKey<Block>, TagAppender<Block, Block>> tags) {
        // incorrect_for_* only reference the needs tags of the tiers above, as far as vanilla and NeoForge do not
        // already; vanilla entries that contradict the order (gold mines like wood in vanilla) are removed
        ToolTier.materials().forEach(material -> {
            ToolTier.materials()
                    .filter(tier -> tier.rank() > material.rank() && tier != ToolTier.NETHERITE && !material.vanillaBlocks(tier))
                    .forEach(tier -> tags.apply(material.incorrectTag()).addOptionalTag(tier.needsTag()));
            material.contradictions().forEach(tier -> tags.apply(material.incorrectTag()).remove(tier.needsTag()));
        });
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = ResourceBlocks.block(base, tier).get();
                if (base.tool.tag() != null) tags.apply(base.tool.tag()).addOptional(block);
                if (base.required(tier).needsTag() != null) tags.apply(base.required(tier).needsTag()).addOptional(block);
            }
        }
    }
}
