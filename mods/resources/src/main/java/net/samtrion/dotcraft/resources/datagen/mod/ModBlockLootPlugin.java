package net.samtrion.dotcraft.resources.datagen.mod;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** The own blocks of the mod drop themselves. */
public final class ModBlockLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        loot.dropSelf(ResourceBlocks.CHARCOAL_BLOCK.get());
    }

    @Override
    public Stream<Block> knownBlocks() {
        return Stream.of(ResourceBlocks.CHARCOAL_BLOCK.get());
    }
}
