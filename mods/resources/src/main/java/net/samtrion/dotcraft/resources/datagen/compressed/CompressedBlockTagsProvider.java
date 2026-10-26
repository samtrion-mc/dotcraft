package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * The tool and tool tier tags of the compressed blocks, and the links between the tiers. The required tool tier
 * rises with the compression tier, see {@link CompressedBase#required(int)}.
 */
public final class CompressedBlockTagsProvider extends BlockTagsProvider {
    public CompressedBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, CraftResources.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // incorrect_for_* only reference the needs tags of the tiers above, as far as vanilla and NeoForge do not
        // already; vanilla entries that contradict the order (gold mines like wood in vanilla) are removed
        ToolTier.materials().forEach(material -> {
            ToolTier.materials()
                    .filter(tier -> tier.rank() > material.rank() && tier != ToolTier.NETHERITE && !material.vanillaBlocks(tier))
                    .forEach(tier -> tag(material.incorrectTag()).addOptionalTag(tier.needsTag()));
            material.contradictions().forEach(tier -> tag(material.incorrectTag()).remove(tier.needsTag()));
        });
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = CompressedBlocks.block(base, tier).get();
                if (base.tool.tag() != null) tag(base.tool.tag()).addOptional(block);
                if (base.required(tier).needsTag() != null) tag(base.required(tier).needsTag()).addOptional(block);
            }
        }
    }
}
