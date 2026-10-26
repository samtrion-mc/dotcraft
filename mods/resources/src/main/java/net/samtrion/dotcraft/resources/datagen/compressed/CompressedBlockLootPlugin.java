package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/** Every compressed block drops itself. */
public final class CompressedBlockLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = ResourceBlocks.block(base, tier).get();
                if (base.modId().equals("minecraft")) {
                    loot.dropSelf(block);
                } else {
                    // without the mod the table would reference an item that does not exist
                    loot.add(block, loot.createSingleItemTable(block).withConditions(List.of(new ModLoadedCondition(base.modId()))));
                }
            }
        }
    }

    @Override
    public Stream<Block> knownBlocks() {
        return ResourceBlocks.blocks().map(DeferredBlock::get);
    }
}
