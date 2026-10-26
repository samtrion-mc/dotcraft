package net.samtrion.dotcraft.resources.datagen.mod;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;

/** The recipes of the own blocks of the mod: the charcoal block from and back into charcoal. */
public final class ModRecipePlugin implements RecipePlugin {
    @Override
    public void buildRecipes(RecipeBuilders recipes, RecipeOutput output) {
        var charcoalBlock = ResourceBlocks.CHARCOAL_BLOCK.get();
        recipes.shaped(RecipeCategory.BUILDING_BLOCKS, charcoalBlock)
                .define('#', Items.CHARCOAL)
                .pattern("###").pattern("###").pattern("###")
                .unlockedBy(recipes.hasName(Items.CHARCOAL), recipes.has(Items.CHARCOAL))
                .save(output, DataGenIds.recipeKey("charcoal_block"));
        recipes.shapeless(RecipeCategory.MISC, Items.CHARCOAL, CompressedTier.FACTOR)
                .requires(charcoalBlock)
                .unlockedBy(recipes.hasName(charcoalBlock), recipes.has(charcoalBlock))
                .save(output, DataGenIds.recipeKey("charcoal_from_charcoal_block"));
    }
}
