package net.samtrion.dotcraft.energy.thermo.datagen;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The thermoelectric generators drop themselves. */
public final class ThermoLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        ThermoBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return ThermoBlocks.blocks().map(b -> (Block) b.get());
    }
}
