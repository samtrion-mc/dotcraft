package net.samtrion.dotcraft.energy.flow;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The flow generators drop themselves. */
public final class FlowLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        FlowBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return FlowBlocks.blocks().map(b -> (Block) b.get());
    }
}
