package net.samtrion.dotcraft.resources.api;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
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
     * Starts a furnace recipe; call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the smelted item
     * @param result     the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 200 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder smelting(ItemLike input, ItemLike result, float experience, int time);

    /**
     * Starts a blast furnace recipe; call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the smelted item
     * @param result     the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 100 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder blasting(ItemLike input, ItemLike result, float experience, int time);

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

    /**
     * The ingredient that accepts every item of a tag.
     *
     * @param tag the item tag
     * @return the ingredient
     */
    Ingredient tag(TagKey<Item> tag);

    /**
     * Starts a furnace recipe that smelts every item of a tag; call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the item tag of the smelted items
     * @param result     the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 200 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder smelting(TagKey<Item> input, ItemLike result, float experience, int time);

    /**
     * Starts a blast furnace recipe that smelts every item of a tag; call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the item tag of the smelted items
     * @param result     the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 100 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder blasting(TagKey<Item> input, ItemLike result, float experience, int time);

    /**
     * The advancement criterion of having an item of a tag.
     *
     * @param tag the item tag
     * @return the criterion
     */
    Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag);

    /**
     * The name of the {@code has} criterion of a tag, as the first argument of {@code unlockedBy}.
     *
     * @param tag the item tag
     * @return the criterion name
     */
    String hasName(TagKey<Item> tag);

    /**
     * Starts a furnace recipe that smelts every item of a tag into an item given by its id; the item need not be
     * registered, so the item of another mod can be the result. Call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the item tag of the smelted items
     * @param result     the id of the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 200 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder smelting(TagKey<Item> input, Identifier result, float experience, int time);

    /**
     * Starts a blast furnace recipe that smelts every item of a tag into an item given by its id; the item need not be
     * registered, so the item of another mod can be the result. Call {@code unlockedBy} and {@code save} on it.
     *
     * @param input      the item tag of the smelted items
     * @param result     the id of the item that comes out
     * @param experience the experience of one result
     * @param time       the cooking time in ticks, 100 in vanilla
     * @return the builder
     */
    SimpleCookingRecipeBuilder blasting(TagKey<Item> input, Identifier result, float experience, int time);
}
