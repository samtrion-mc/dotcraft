package net.samtrion.dotcraft.energy.wheel.datagen;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The mob wheels drop themselves. */
public final class WheelLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        WheelBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return WheelBlocks.blocks().map(b -> (Block) b.get());
    }
}
