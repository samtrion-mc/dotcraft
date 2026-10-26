package net.samtrion.dotcraft.resources.datagen.compressed;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.common.conditions.MaxTierCondition;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;

/**
 * Compress (9 of a tier into one of the next) and decompress (one back into 9) for every tier. Every recipe is
 * disabled by {@link MaxTierCondition} when its output level exceeds the configured maximum.
 */
public final class CompressedRecipePlugin implements RecipePlugin {
    /** Recipes of other mods' blocks only load when that mod is present. */
    private static RecipeOutput conditioned(RecipeOutput output, CompressedBase base) {
        return base.modId().equals("minecraft") ? output : output.withConditions(new ModLoadedCondition(base.modId()));
    }

    /** Disabled by the config when the recipe outputs a level above the configured maximum. */
    private static RecipeOutput conditioned(RecipeOutput output, CompressedBase base, int outputLevel) {
        return outputLevel < 1 ? conditioned(output, base) : conditioned(output, base).withConditions(new MaxTierCondition(outputLevel));
    }

    @Override
    public void buildRecipes(RecipeBuilders recipes, RecipeOutput output) {
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                var lower = tier == 1 ? base.block() : ResourceBlocks.block(base, tier - 1).get();
                var upper = ResourceBlocks.block(base, tier).get();
                recipes.shaped(RecipeCategory.BUILDING_BLOCKS, upper)
                        .define('#', lower)
                        .pattern("###").pattern("###").pattern("###")
                        .unlockedBy(recipes.hasName(lower), recipes.has(lower))
                        .save(conditioned(output, base, tier), DataGenIds.recipeKey(base.recipePath("compress", tier)));
                recipes.shapeless(RecipeCategory.BUILDING_BLOCKS, lower, CompressedTier.FACTOR)
                        .requires(upper)
                        .unlockedBy(recipes.hasName(upper), recipes.has(upper))
                        .save(conditioned(output, base, tier - 1), DataGenIds.recipeKey(base.recipePath("decompress", tier)));
            }
        }
    }
}
