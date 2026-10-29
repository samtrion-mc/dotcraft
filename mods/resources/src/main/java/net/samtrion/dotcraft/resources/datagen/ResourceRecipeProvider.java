package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;

/** The recipes of all blocks; the plugins are called in the given order. */
public final class ResourceRecipeProvider extends RecipeProvider.Runner {
    private final List<RecipePlugin> plugins;

    /**
     * Creates the provider.
     *
     * @param output     the pack output
     * @param registries the registries
     * @param plugins    the plugins
     */
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

        /** Creates the recipe provider. */
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
        public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
            return super.shaped(category, result, count);
        }

        @Override
        public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
            return super.shapeless(category, result, count);
        }

        @Override
        public SimpleCookingRecipeBuilder smelting(ItemLike input, ItemLike result, float experience, int time) {
            return SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, time);
        }

        @Override
        public SimpleCookingRecipeBuilder blasting(ItemLike input, ItemLike result, float experience, int time) {
            return SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, time);
        }

        @Override
        public Ingredient tag(TagKey<Item> tag) {
            return super.tag(tag);
        }

        @Override
        public SimpleCookingRecipeBuilder smelting(TagKey<Item> input, ItemLike result, float experience, int time) {
            return SimpleCookingRecipeBuilder.smelting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, time);
        }

        @Override
        public SimpleCookingRecipeBuilder blasting(TagKey<Item> input, ItemLike result, float experience, int time) {
            return SimpleCookingRecipeBuilder.blasting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, experience, time);
        }

        @Override
        public SimpleCookingRecipeBuilder smelting(TagKey<Item> input, Identifier result, float experience, int time) {
            return SimpleCookingRecipeBuilder.smelting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, template(result), experience, time);
        }

        @Override
        public SimpleCookingRecipeBuilder blasting(TagKey<Item> input, Identifier result, float experience, int time) {
            return SimpleCookingRecipeBuilder.blasting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, template(result), experience, time);
        }

        /** One item of an id; a registered item is looked up, another one (an item of a mod that is not loaded) becomes a reference by key. */
        private ItemStackTemplate template(Identifier id) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            var lookup = registries.lookupOrThrow(Registries.ITEM);
            Holder<Item> holder = lookup.get(key).<Holder<Item>>map(h -> h)
                    .orElseGet(() -> Holder.Reference.createStandAlone(lookup, key));
            return new ItemStackTemplate(holder);
        }

        @Override
        public Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
            return super.has(tag);
        }

        @Override
        public String hasName(TagKey<Item> tag) {
            return "has_" + tag.location().getPath().replace('/', '_');
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
