package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;

/** The recipes of all blocks; the plugins are called in the given order. */
public final class ResourceRecipeProvider extends RecipeProvider.Runner {
    private final List<RecipePlugin> plugins;

    public ResourceRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, RecipePlugin... plugins) {
        super(output, registries);
        this.plugins = List.of(plugins);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output, plugins);
    }

    @Override
    public String getName() {
        return "Block Recipes";
    }

    /** Gives the plugins access to the protected builders of {@link RecipeProvider}. */
    private static final class Recipes extends RecipeProvider implements RecipeBuilders {
        private final RecipeOutput output;
        private final List<RecipePlugin> plugins;

        Recipes(HolderLookup.Provider registries, RecipeOutput output, List<RecipePlugin> plugins) {
            super(registries, output);
            this.output = output;
            this.plugins = plugins;
        }

        @Override
        protected void buildRecipes() {
            plugins.forEach(plugin -> plugin.buildRecipes(this, output));
        }

        @Override
        public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
            return super.shaped(category, result);
        }

        @Override
        public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
            return super.shapeless(category, result, count);
        }

        @Override
        public Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
            return super.has(item);
        }

        @Override
        public String hasName(ItemLike item) {
            return getHasName(item);
        }
    }
}
