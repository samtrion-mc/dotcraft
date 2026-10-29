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
import net.samtrion.dotcraft.energy.current.CurrentRecipes;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.melt.datagen.MeltRecipes;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the batteries, the solar cell and the panels: the first two shapeless from a potato, redstone grains and copper and zinc nuggets, tiers 2-5 shaped from
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
            photovoltaicCell();
            Item[] batteries = {EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), EnergyItems.BATTERY_TIER_2.get(), EnergyItems.BATTERY_TIER_3.get(),
                    EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_5.get()};
            panels(EnergyBlocks.SOLAR, batteries, Tags.Items.GLASS_PANES, "aluminum", "bronze", "steel", "electrum", "osmium");
            panels(EnergyBlocks.LUNAR, batteries, Tags.Items.GEMS_LAPIS, "lead", "brass", "invar", "silver", "platinum");
            new CurrentRecipes(this.registries, output).generate();
            vibrationGenerators(batteries, "iron", "copper", "nickel", "gold", "iridium");
            new MeltRecipes(registries, output).build();
        }

        /** Two photovoltaic cells: two quartz, a glass pane and redstone grains; unlocked by the quartz. */
        private void photovoltaicCell() {
            var cell = EnergyItems.PHOTOVOLTAIC_CELL.get();
            var recipe = shapeless(RecipeCategory.MISC, cell, 2).requires(Tags.Items.GEMS_QUARTZ).requires(Tags.Items.GEMS_QUARTZ)
                    .requires(Tags.Items.GLASS_PANES).requires(tag("redstone", "grains"));
            recipe.unlockedBy(hasName(Tags.Items.GEMS_QUARTZ), has(Tags.Items.GEMS_QUARTZ)).save(output,
                    ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/photovoltaic_cell")));
        }

        /**
         * The five tiers of a panel family, shaped: solar cell, the family's accent (glass pane / lapis) and solar cell on top, the
         * tier's battery between two plates of the tier's metal, and below another plate, the panel of the tier below (tier 1: redstone
         * grains) and a plate. Unlocked by the battery; dropped when a used item is switched off in the startup config.
         */
        private void panels(EnergyBlocks.Family family, Item[] batteries, TagKey<Item> accent, String... metals) {
            Item previous = null;
            for (int tier = 1; tier <= metals.length; tier++) {
                var block = family.block(tier);
                if (block == null) {
                    previous = null;
                    continue;
                }
                var item = block.get().asItem();
                var battery = batteries[tier - 1];
                var builder = shaped(RecipeCategory.MISC, item)
                        .define('C', EnergyItems.PHOTOVOLTAIC_CELL.get())
                        .define('A', accent)
                        .define('P', tag(metals[tier - 1], "plate"))
                        .define('B', battery)
                        .pattern("CAC").pattern("PBP");
                if (previous == null) builder.define('X', tag("redstone", "grains"));
                else builder.define('X', previous);
                builder.pattern("PXP");
                var conditions = previous == null
                        ? new net.neoforged.neoforge.common.conditions.ICondition[] {registered(item), registered(battery)}
                        : new net.neoforged.neoforge.common.conditions.ICondition[] {registered(item), registered(battery), registered(previous)};
                builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                        ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
                previous = item;
            }
        }

        /**
         * The five tiers of the vibration generator, shaped: plates of the tier's metal around a quartz (the piezo crystal) on top,
         * gears left and right of the centre, which is the generator of the tier below (tier 1: redstone grains), and the tier's
         * battery below between two plates. Unlocked by the battery; dropped when a used item is switched off in the startup config.
         */
        private void vibrationGenerators(Item[] batteries, String... metals) {
            Item previous = null;
            for (int tier = 1; tier <= metals.length; tier++) {
                var block = VibrationBlocks.block(tier);
                if (block == null) {
                    previous = null;
                    continue;
                }
                var item = block.get().asItem();
                var battery = batteries[tier - 1];
                var builder = shaped(RecipeCategory.MISC, item)
                        .define('P', tag(metals[tier - 1], "plate"))
                        .define('Q', Tags.Items.GEMS_QUARTZ)
                        .define('G', tag(metals[tier - 1], "gear"))
                        .define('B', battery)
                        .pattern("PQP").pattern("GXG").pattern("PBP");
                if (previous == null) builder.define('X', tag("redstone", "grains"));
                else builder.define('X', previous);
                var conditions = previous == null
                        ? new net.neoforged.neoforge.common.conditions.ICondition[] {registered(item), registered(battery)}
                        : new net.neoforged.neoforge.common.conditions.ICondition[] {registered(item), registered(battery), registered(previous)};
                builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                        ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
                previous = item;
            }
        }

        private static RegisteredCondition<Item> registered(Item item) {
            return EnergyRecipeProvider.registered(item);
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
            return materialTag(material, form);
        }
    }

    /**
     * The item tag of a material in a form of the resources mod, e.g. {@code c:plates/iron}; shared with the recipes of the other packages.
     *
     * @param material the id of the material
     * @param form     the id of the form, e.g. {@code plate}
     * @return the tag
     */
    public static TagKey<Item> materialTag(String material, String form) {
        var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
        return MaterialTags.item(Forms.get(form), m);
    }

    /**
     * The condition that an item is registered, so a recipe is dropped when the item is switched off; shared with the recipes of the other packages.
     *
     * @param item the item
     * @return the condition
     */
    public static RegisteredCondition<Item> registered(Item item) {
        return new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)));
    }
}
