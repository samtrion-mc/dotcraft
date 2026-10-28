package net.samtrion.dotcraft.resources.api;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/** Registers block loot tables, handed to a {@link LootPlugin}. */
public interface BlockLoot {
    /**
     * Makes a block drop itself.
     *
     * @param block the block
     */
    void dropSelf(Block block);

    /**
     * Sets the loot table of a block.
     *
     * @param block the block
     * @param table the loot table
     */
    void add(Block block, LootTable.Builder table);

    /**
     * A loot table that drops one item.
     *
     * @param item the dropped item
     * @return the loot table builder
     */
    LootTable.Builder createSingleItemTable(ItemLike item);

    /**
     * A loot table as of vanilla ores: silk touch drops the block, otherwise the item drops in the given count
     * with the Fortune bonus.
     *
     * @param ore   the ore block
     * @param drop  the dropped item
     * @param count how many items drop before the Fortune bonus
     * @return the loot table builder
     */
    LootTable.Builder createOreDrop(Block ore, ItemLike drop, NumberProvider count);
}
