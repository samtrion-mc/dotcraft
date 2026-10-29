package net.samtrion.dotcraft.energy.current;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The current generators drop themselves. */
public final class CurrentLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        CurrentBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return CurrentBlocks.blocks().map(b -> (Block) b.get());
    }
}
