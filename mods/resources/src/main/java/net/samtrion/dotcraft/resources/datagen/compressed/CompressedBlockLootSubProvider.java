package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/** Every compressed block drops itself. */
public final class CompressedBlockLootSubProvider extends BlockLootSubProvider {
    public CompressedBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = CompressedBlocks.block(base, tier).get();
                if (base.modId().equals("minecraft")) {
                    dropSelf(block);
                } else {
                    // without the mod the table would reference an item that does not exist
                    add(block, createSingleItemTable(block).withConditions(List.of(new ModLoadedCondition(base.modId()))));
                }
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CompressedBlocks.blocks().<Block>map(DeferredBlock::get).toList();
    }
}
