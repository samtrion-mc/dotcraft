package net.samtrion.dotcraft.resources.datagen.material;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.samtrion.dotcraft.resources.api.BlockLoot;
import net.samtrion.dotcraft.resources.api.LootPlugin;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreRichness;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The loot of the material blocks as their {@link Form.BlockSpec.Loot form source} names it: blocks drop
 * themselves; ore blocks drop the {@code drop} form as vanilla ores do, in the count of their
 * {@link OreRichness}, and themselves with silk touch. A material or form with {@code neoforge:conditions} (see {@link Conditions}) gets the same conditions on its loot tables.
 */
public final class MaterialBlockLootPlugin implements LootPlugin {
    @Override
    public void generate(BlockLoot loot) {
        for (var material : Materials.all()) {
            for (Form form : material.forms()) {
                if (!form.isBlock()) continue;
                Form.BlockSpec spec = form.block().orElseThrow();
                if (spec.loot() == Form.BlockSpec.Loot.ORE) {
                    Form dropForm = Forms.get(spec.drop().orElseThrow());
                    var drop = ResourceItems.item(material, dropForm).get();
                    List<ICondition> conditions = material.conditions(form);
                    for (var variant : material.oreVariants()) {
                        Block ore = MaterialBlocks.ore(material, variant.host(), variant.richness()).get();
                        loot.add(ore, conditional(loot.createOreDrop(ore, drop, count(variant.richness())), conditions));
                    }
                } else {
                    Block block = MaterialBlocks.block(material, form).get();
                    List<ICondition> conditions = material.conditions(form);
                    if (conditions.isEmpty()) loot.dropSelf(block);
                    else loot.add(block, conditional(loot.createSingleItemTable(block), conditions));
                }
            }
        }
    }

    /**
     * Makes a loot table load only while conditions hold; the loot table that its items are missing from would else fail to load.
     *
     * @param table      the loot table
     * @param conditions the {@code neoforge:conditions} of the material and its forms, may be empty
     * @return the table with the conditions
     */
    private static LootTable.Builder conditional(LootTable.Builder table, List<ICondition> conditions) {
        return conditions.isEmpty() ? table : table.withConditions(conditions);
    }

    /** The drop count of an ore of a richness. */
    private static NumberProvider count(OreRichness richness) {
        return richness.minDrops() == richness.maxDrops()
                ? ConstantValue.exactly(richness.minDrops())
                : UniformGenerator.between(richness.minDrops(), richness.maxDrops());
    }

    @Override
    public Stream<Block> knownBlocks() {
        return MaterialBlocks.blocks().map(DeferredBlock::get);
    }
}
