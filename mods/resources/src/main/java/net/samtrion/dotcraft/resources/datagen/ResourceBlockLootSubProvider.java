package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The block loot tables of all blocks; the plugins are called in the given order. */
public final class ResourceBlockLootSubProvider extends BlockLootSubProvider implements BlockLoot {
    private final List<LootPlugin> plugins;

    public ResourceBlockLootSubProvider(HolderLookup.Provider registries, LootPlugin... plugins) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void generate() {
        plugins.forEach(plugin -> plugin.generate(this));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return plugins.stream().flatMap(LootPlugin::knownBlocks).toList();
    }

    @Override
    public void dropSelf(Block block) {
        super.dropSelf(block);
    }

    @Override
    public void add(Block block, LootTable.Builder table) {
        super.add(block, table);
    }
}
