package net.samtrion.dotcraft.energy.datagen;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The generator blocks drop themselves. */
final class GeneratorLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        EnergyBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return EnergyBlocks.blocks().map(b -> (Block) b.get());
    }
}
