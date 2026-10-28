package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;

/** The block loot tables of all blocks; the plugins are called in the given order. */
public final class ResourceBlockLootSubProvider extends BlockLootSubProvider implements BlockLoot {
    private final List<LootPlugin> plugins;

    /**
     * Creates the provider.
     *
     * @param registries the registries
     * @param plugins    the plugins
     */
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

    @Override
    public LootTable.Builder createOreDrop(Block ore, ItemLike drop, NumberProvider count) {
        var fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return createSilkTouchDispatchTable(ore, (LootPoolEntryContainer.Builder<?>) applyExplosionDecay(ore,
                LootItem.lootTableItem(drop)
                        .apply(SetItemCountFunction.setCount(count))
                        .apply(ApplyBonusCount.addOreBonusCount(fortune))));
    }
}
