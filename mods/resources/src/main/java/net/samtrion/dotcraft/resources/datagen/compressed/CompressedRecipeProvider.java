package net.samtrion.dotcraft.resources.datagen.compressed;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.common.conditions.MaxTierCondition;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;

/**
 * Compress (9 of a tier into one of the next) and decompress (one back into 9) for every tier. Every recipe is
 * disabled by {@link MaxTierCondition} when its output level exceeds the configured maximum.
 */
public final class CompressedRecipeProvider extends RecipeProvider.Runner {
    public CompressedRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            /** Recipes of other mods' blocks only load when that mod is present. */
            private RecipeOutput conditioned(CompressedBase base) {
                return base.modId().equals("minecraft") ? output : output.withConditions(new ModLoadedCondition(base.modId()));
            }

            /** Disabled by the config when the recipe outputs a level above the configured maximum. */
            private RecipeOutput conditioned(CompressedBase base, int outputLevel) {
                return outputLevel < 1 ? conditioned(base) : conditioned(base).withConditions(new MaxTierCondition(outputLevel));
            }

            @Override
            protected void buildRecipes() {
                for (CompressedBase base : CompressedBases.enabled().toList()) {
                    for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                        var lower = tier == 1 ? base.block() : ResourceBlocks.block(base, tier - 1).get();
                        var upper = ResourceBlocks.block(base, tier).get();
                        shaped(RecipeCategory.BUILDING_BLOCKS, upper)
                                .define('#', lower)
                                .pattern("###").pattern("###").pattern("###")
                                .unlockedBy(getHasName(lower), has(lower))
                                .save(conditioned(base, tier), DataGenIds.recipeKey(base.recipePath("compress", tier)));
                        shapeless(RecipeCategory.BUILDING_BLOCKS, lower, CompressedTier.FACTOR)
                                .requires(upper)
                                .unlockedBy(getHasName(upper), has(upper))
                                .save(conditioned(base, tier - 1), DataGenIds.recipeKey(base.recipePath("decompress", tier)));
                    }
                }
            }
        };
    }

    @Override
    public String getName() {
        return "Compressed Block Recipes";
    }
}
