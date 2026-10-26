package net.samtrion.dotcraft.resources.api;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;

/** The recipe builders of the recipe provider, handed to a {@link RecipePlugin}. */
public interface RecipeBuilders {
    /**
     * Starts a shaped recipe.
     *
     * @param category the recipe book category
     * @param result   the crafted item
     * @return the builder
     */
    ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result);

    /**
     * Starts a shapeless recipe.
     *
     * @param category the recipe book category
     * @param result   the crafted item
     * @param count    how many of the item are crafted
     * @return the builder
     */
    ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count);

    /**
     * The advancement criterion of having an item.
     *
     * @param item the item
     * @return the criterion
     */
    Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item);

    /**
     * The name of the {@code has} criterion of an item, as the first argument of {@code unlockedBy}.
     *
     * @param item the item
     * @return the criterion name
     */
    String hasName(ItemLike item);
}
