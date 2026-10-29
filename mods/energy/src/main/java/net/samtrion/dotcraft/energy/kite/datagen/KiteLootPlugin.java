package net.samtrion.dotcraft.energy.kite.datagen;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The kite generators drop themselves (the kite inside comes back as an item through the block entity). */
public final class KiteLootPlugin implements LootPlugin {
    /** Creates the plugin. */
    public KiteLootPlugin() {}

    @Override
    public void generate(BlockLoot loot) {
        KiteBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return KiteBlocks.blocks().map(b -> (Block) b.get());
    }
}
