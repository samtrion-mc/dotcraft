package net.samtrion.dotcraft.energy.melt.datagen;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The melt generators drop themselves. */
public final class MeltLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        MeltBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return MeltBlocks.blocks().map(b -> (Block) b.get());
    }
}
