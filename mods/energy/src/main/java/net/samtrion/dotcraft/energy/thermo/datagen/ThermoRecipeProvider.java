package net.samtrion.dotcraft.energy.thermo.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.RegisteredCondition;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the thermoelectric generators, shaped, ingredients by tag: {@code PQP / GBG / PXP} with P a plate of the tier's metal, Q quartz (the semiconductor), G a gear
 * of the tier's metal, B the battery of the tier and X the generator of the tier below (tier 1: a copper plate). The battery and the
 * lower generator are items of this mod, so they are named by id (the documented exception). The recipes are dropped when a used
 * item is switched off in the startup config.
 */
public final class ThermoRecipeProvider extends RecipeProvider.Runner {
    /** The metal of each tier: the metals of the panels, so the accents match. */
    private static final String[] METALS = {"aluminum", "bronze", "steel", "electrum", "osmium"};

    /**
     * Creates the provider.
     *
     * @param output     the pack output
     * @param registries the registries
     */
    public ThermoRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output);
    }

    @Override
    public String getName() {
        return "Thermoelectric Generator Recipes";
    }

    /** Builds the recipes; a subclass of {@link RecipeProvider} as its builders are protected. */
    private static final class Recipes extends RecipeProvider {
        /** Where the recipes go. */
        private final RecipeOutput output;

        /** Creates the recipe provider. */
        Recipes(HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
            this.output = output;
        }

        @Override
        protected void buildRecipes() {
            Item[] batteries = {EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), EnergyItems.BATTERY_TIER_2.get(), EnergyItems.BATTERY_TIER_3.get(),
                    EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_5.get()};
            Item previous = null;
            for (int tier = 1; tier <= METALS.length; tier++) {
                var block = ThermoBlocks.block(tier);
                if (block == null) {
                    previous = null;
                    continue;
                }
                var item = block.get().asItem();
                var battery = batteries[tier - 1];
                var builder = shaped(RecipeCategory.MISC, item)
                        .define('P', tag(METALS[tier - 1], "plate"))
                        .define('Q', Tags.Items.GEMS_QUARTZ)
                        .define('G', tag(METALS[tier - 1], "gear"))
                        .define('B', battery)
                        .pattern("PQP").pattern("GBG");
                if (previous == null) builder.define('X', tag("copper", "plate"));
                else builder.define('X', previous);
                builder.pattern("PXP");
                var conditions = previous == null
                        ? new ICondition[] {registered(item), registered(battery)}
                        : new ICondition[] {registered(item), registered(battery), registered(previous)};
                builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                        ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
                previous = item;
            }
        }

        // The two helpers below deliberately mirror those of EnergyRecipeProvider (private there) so the shared file stays untouched.
        /** The condition that an item is registered, for a recipe that uses or makes it. */
        private static RegisteredCondition<Item> registered(Item item) {
            return new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item)));
        }

        /** The item tag of a material in a form of the resources mod, e.g. {@code c:plates/copper}. */
        private static TagKey<Item> tag(String material, String form) {
            var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
            return MaterialTags.item(Forms.get(form), m);
        }
    }
}
