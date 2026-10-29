package net.samtrion.dotcraft.energy.current;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.datagen.EnergyRecipeProvider;
import net.samtrion.dotcraft.energy.item.EnergyItems;

/**
 * The recipes of the current generators, called from the {@link EnergyRecipeProvider}. A subclass of {@link RecipeProvider}, as
 * its builders are protected.
 */
public final class CurrentRecipes extends RecipeProvider {
    /** The metal of the plates and gears of every tier. */
    private static final String[] METALS = {"iron", "tin", "steel", "platinum", "iridium"};

    private final RecipeOutput output;

    /**
     * Creates the recipe provider.
     *
     * @param registries the registries
     * @param output     where the recipes go
     */
    public CurrentRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
        this.output = output;
    }

    /** Builds the recipes of all tiers. */
    public void generate() {
        buildRecipes();
    }

    /**
     * The five tiers, shaped: plates in the corners and top middle, gears left and right of the battery of the tier, and below
     * the generator of the tier below (tier 1: redstone grains). The metal of plates and gears is the tier's; the ingredients are
     * tags except the batteries and the lower generator, which are items of this mod. Unlocked by the battery; dropped when a used
     * item is switched off in the startup config.
     */
    @Override
    protected void buildRecipes() {
        Item[] batteries = {EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), EnergyItems.BATTERY_TIER_2.get(), EnergyItems.BATTERY_TIER_3.get(),
                EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_5.get()};
        Item previous = null;
        for (int tier = 1; tier <= CurrentMath.MAX_TIER; tier++) {
            var block = CurrentBlocks.block(tier);
            if (block == null) {
                previous = null;
                continue;
            }
            var item = block.get().asItem();
            var battery = batteries[tier - 1];
            var builder = shaped(RecipeCategory.MISC, item)
                    .define('P', EnergyRecipeProvider.materialTag(METALS[tier - 1], "plate"))
                    .define('G', EnergyRecipeProvider.materialTag(METALS[tier - 1], "gear"))
                    .define('B', battery)
                    .pattern("PGP").pattern("GBG");
            if (previous == null) builder.define('X', EnergyRecipeProvider.materialTag("redstone", "grains"));
            else builder.define('X', previous);
            builder.pattern("PXP");
            var conditions = previous == null
                    ? new ICondition[] {EnergyRecipeProvider.registered(item), EnergyRecipeProvider.registered(battery)}
                    : new ICondition[] {EnergyRecipeProvider.registered(item), EnergyRecipeProvider.registered(battery), EnergyRecipeProvider.registered(previous)};
            builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                    ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
            previous = item;
        }
    }
}
