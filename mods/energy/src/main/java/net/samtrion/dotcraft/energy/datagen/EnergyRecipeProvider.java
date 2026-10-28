package net.samtrion.dotcraft.energy.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.RegisteredCondition;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the batteries: the first two shapeless from a potato, redstone grains and copper and zinc nuggets, tiers 2-5 shaped from
 * two batteries of the tier below, redstone grains and ingots; the ingredients are tags.
 */
public final class EnergyRecipeProvider extends RecipeProvider.Runner {
    /**
     * Creates the provider.
     *
     * @param output     the pack output
     * @param registries the registries
     */
    public EnergyRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output);
    }

    @Override
    public String getName() {
        return "Energy Recipes";
    }

    /** Builds the recipes; a subclass of {@link RecipeProvider} as its builders are protected. */
    private static final class Recipes extends RecipeProvider {
        private final RecipeOutput output;

        /** Creates the recipe provider. */
        Recipes(HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
            this.output = output;
        }

        @Override
        protected void buildRecipes() {
            battery("potato_battery", EnergyItems.POTATO_BATTERY.get(), Tags.Items.CROPS_POTATO);
            // no tag holds the poisonous potato alone ("c:foods/food_poisoning" has other items too), so its id it is
            battery("rechargeable_potato_battery", EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), Items.POISONOUS_POTATO);
            tier("battery_tier_2", EnergyItems.BATTERY_TIER_2.get(), EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), "tin", "lead");
            tier("battery_tier_3", EnergyItems.BATTERY_TIER_3.get(), EnergyItems.BATTERY_TIER_2.get(), "nickel", "zinc");
            tier("battery_tier_4", EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_3.get(), "silver", "zinc");
            tier("battery_tier_5", EnergyItems.BATTERY_TIER_5.get(), EnergyItems.BATTERY_TIER_4.get(), "iridium", "platinum");
        }

        /** A rechargeable battery as a recipe result: always uncharged (0 FE), a stack without the component would be full. */
        private static ItemStackTemplate empty(Item battery) {
            return new ItemStackTemplate(battery, DataComponentPatch.builder().set(EnergyItems.ENERGY.get(), 0).build());
        }

        /**
         * A higher tier, shaped: two batteries of the tier below in the top corners, a column of three redstone grains in the middle,
         * and two ingots each of the cathode (left) and the anode (right); the result is uncharged. Unlocked by the lower battery.
         */
        private void tier(String path, Item battery, Item lower, String cathode, String anode) {
            save(path, shaped(RecipeCategory.MISC, empty(battery))
                    .define('B', lower)
                    .define('R', tag("redstone", "grains"))
                    .define('C', tag(cathode, "ingot"))
                    .define('A', tag(anode, "ingot"))
                    .pattern("BRB").pattern("CRA").pattern("CRA"), getHasName(lower), has(lower));
        }

        /** The battery of a tagged potato. */
        private void battery(String path, Item battery, TagKey<Item> potato) {
            battery(path, battery, shapeless(RecipeCategory.MISC, battery).requires(potato), hasName(potato), has(potato));
        }

        /** The rechargeable battery of a single potato item; the result is uncharged. */
        private void battery(String path, Item battery, Item potato) {
            battery(path, battery, shapeless(RecipeCategory.MISC, empty(battery)).requires(potato), getHasName(potato), has(potato));
        }

        /**
         * One battery: the potato and the redstone grains and the copper and zinc nuggets (by tag), unlocked by the potato; dropped
         * when the battery is switched off in the startup config. The recipes are in the folder {@code items}.
         */
        private void battery(String path, Item battery, ShapelessRecipeBuilder recipe, String unlockName, Criterion<?> unlock) {
            save(path, recipe.requires(tag("redstone", "grains")).requires(tag("copper", "nugget")).requires(tag("zinc", "nugget")), unlockName, unlock);
        }

        /** Saves a battery recipe in the folder {@code items}, dropped when the battery is switched off in the startup config. */
        private void save(String path, RecipeBuilder recipe, String unlockName, Criterion<?> unlock) {
            recipe.unlockedBy(unlockName, unlock)
                    .save(output.withConditions(new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EnergyMod.MODID, path)))),
                            ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + path)));
        }

        /** The name of the unlock criterion of a tag. */
        private static String hasName(TagKey<Item> tag) {
            return "has_" + tag.location().getPath().replace('/', '_');
        }

        /** The item tag of a material in a form of the resources mod, e.g. {@code c:nuggets/zinc}. */
        private static TagKey<Item> tag(String material, String form) {
            var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
            return MaterialTags.item(Forms.get(form), m);
        }
    }
}
