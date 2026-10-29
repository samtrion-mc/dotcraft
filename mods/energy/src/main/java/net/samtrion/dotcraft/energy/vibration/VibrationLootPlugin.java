package net.samtrion.dotcraft.energy.vibration;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The vibration generators drop themselves. */
public final class VibrationLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        VibrationBlocks.blocks().forEach(b -> loot.dropSelf(b.get()));
    }

    @Override
    public Stream<Block> knownBlocks() {
        return VibrationBlocks.blocks().map(b -> (Block) b.get());
    }
}
