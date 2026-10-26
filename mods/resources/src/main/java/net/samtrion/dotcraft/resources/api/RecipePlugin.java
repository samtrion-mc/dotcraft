package net.samtrion.dotcraft.resources.api;

import net.minecraft.data.recipes.RecipeOutput;

/** Contributes recipes to the recipe provider. */
public interface RecipePlugin {
    /**
     * Builds the recipes.
     *
     * @param recipes the recipe builders
     * @param output  where the recipes are saved
     */
    void buildRecipes(RecipeBuilders recipes, RecipeOutput output);
}
