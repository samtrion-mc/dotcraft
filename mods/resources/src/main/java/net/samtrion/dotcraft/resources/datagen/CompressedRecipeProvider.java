package net.samtrion.dotcraft.resources.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/** Compress (9 of a tier into one of the next) and decompress (one back into 9) for every tier. */
final class CompressedRecipeProvider extends RecipeProvider.Runner {
    CompressedRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            /** Recipes of other mods' blocks only load when that mod is present. */
            private RecipeOutput conditioned(CompressedBase base) {
                return base.modId().equals("minecraft") ? output : output.withConditions(new ModLoadedCondition(base.modId()));
            }

            @Override
            protected void buildRecipes() {
                for (CompressedBase base : CompressedBases.enabled().toList()) {
                    for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                        var lower = tier == 1 ? base.block() : CompressedBlocks.block(base, tier - 1).get();
                        var upper = CompressedBlocks.block(base, tier).get();
                        shaped(RecipeCategory.BUILDING_BLOCKS, upper)
                                .define('#', lower)
                                .pattern("###").pattern("###").pattern("###")
                                .unlockedBy(getHasName(lower), has(lower))
                                .save(conditioned(base), DataGenIds.recipeKey(base.recipePath("compress", tier)));
                        shapeless(RecipeCategory.BUILDING_BLOCKS, lower, CompressedTier.FACTOR)
                                .requires(upper)
                                .unlockedBy(getHasName(upper), has(upper))
                                .save(conditioned(base), DataGenIds.recipeKey(base.recipePath("decompress", tier)));
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
