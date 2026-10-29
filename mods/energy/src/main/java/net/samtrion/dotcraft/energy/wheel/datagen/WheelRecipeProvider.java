package net.samtrion.dotcraft.energy.wheel.datagen;

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
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the mob wheels, shaped: plates of the tier's metal at the corners and below, gears of the metal at the sides, the
 * axle in the middle (tier 1: a wooden rod, above: the wheel of the tier below) and the battery of the tier under it. The metals
 * are iron, nickel, steel, platinum and iridium; every ingredient is a tag but the own items (battery, lower wheel), which are
 * the documented exception. A recipe is dropped when its wheel or one of the own items is switched off in the startup config.
 */
public final class WheelRecipeProvider extends RecipeProvider.Runner {
    private static final String[] METALS = {"iron", "nickel", "steel", "platinum", "iridium"};

    /**
     * Creates the provider.
     *
     * @param output     the pack output
     * @param registries the registries
     */
    public WheelRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output);
    }

    @Override
    public String getName() {
        return "Wheel Recipes";
    }

    /** Builds the recipes; a subclass of {@link RecipeProvider} as its builders are protected. */
    private static final class Recipes extends RecipeProvider {
        private final RecipeOutput output;

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
                var block = WheelBlocks.block(tier);
                if (block == null) {
                    previous = null;
                    continue;
                }
                var item = block.get().asItem();
                var battery = batteries[tier - 1];
                var builder = shaped(RecipeCategory.MISC, item)
                        .define('P', tag(METALS[tier - 1], "plate"))
                        .define('G', tag(METALS[tier - 1], "gear"))
                        .define('B', battery)
                        .pattern("PGP").pattern("GXG").pattern("PBP");
                if (previous == null) builder.define('X', Tags.Items.RODS_WOODEN);
                else builder.define('X', previous);
                var conditions = previous == null
                        ? new ICondition[] {registered(item), registered(battery)}
                        : new ICondition[] {registered(item), registered(battery), registered(previous)};
                builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                        ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
                previous = item;
            }
        }

        private static RegisteredCondition<Item> registered(Item item) {
            return new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item)));
        }

        /** The item tag of a material in a form of the resources mod, e.g. {@code c:plates/iron}. */
        private static TagKey<Item> tag(String material, String form) {
            var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
            return MaterialTags.item(Forms.get(form), m);
        }
    }
}
