package net.samtrion.dotcraft.resources.api;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;

/** Contributes block loot tables to the block loot provider. */
public interface LootPlugin {
    /**
     * Registers the loot tables.
     *
     * @param loot the registry of the loot tables
     */
    void generate(BlockLoot loot);

    /**
     * The blocks that must have a loot table after {@link #generate(BlockLoot)}.
     *
     * @return the blocks of this plugin
     */
    Stream<Block> knownBlocks();
}
