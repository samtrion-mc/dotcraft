package net.samtrion.dotcraft.energy.kite.datagen;

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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.RegisteredCondition;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the kite and the kite wind generators. The kite is shaped from wool, a wooden rod and string. A generator is
 * shaped: gear, string and gear on top; a plate, the battery of the tier and a plate in the middle; at the bottom a plate, the
 * generator of the tier below (tier 1: redstone grains) and a plate. The metals are aluminum, copper, steel, gold and osmium; the
 * ingredients are tags except the batteries and the lower generator, which are items of this mod.
 */
public final class KiteRecipeProvider extends RecipeProvider.Runner {
    /**
     * Creates the provider.
     *
     * @param output     the pack output
     * @param registries the registries
     */
    public KiteRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output);
    }

    @Override
    public String getName() {
        return "Kite Recipes";
    }

    /** Builds the recipes; a subclass of {@link RecipeProvider} as its builders are protected. */
    private static final class Recipes extends RecipeProvider {
        private static final String[] METALS = {"aluminum", "copper", "steel", "gold", "osmium"};

        private final RecipeOutput output;

        /** Creates the recipe provider. */
        Recipes(HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
            this.output = output;
        }

        @Override
        protected void buildRecipes() {
            var kite = KiteBlocks.KITE.get();
            shaped(RecipeCategory.MISC, kite)
                    .define('W', ItemTags.WOOL)
                    .define('R', Tags.Items.RODS_WOODEN)
                    .define('S', Tags.Items.STRINGS)
                    .pattern(" W ").pattern("WRW").pattern(" S ")
                    .unlockedBy("has_string", has(Tags.Items.STRINGS))
                    .save(output.withConditions(registered(kite)), key("items/kite"));
            Item[] batteries = {EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), EnergyItems.BATTERY_TIER_2.get(), EnergyItems.BATTERY_TIER_3.get(),
                    EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_5.get()};
            Item previous = null;
            for (int tier = 1; tier <= METALS.length; tier++) {
                var block = KiteBlocks.block(tier);
                // a tier above a switched-off one gets no recipe: only tier 1 may use redstone grains instead of the lower generator
                if (block == null || (tier > 1 && previous == null)) {
                    previous = null;
                    continue;
                }
                var item = block.get().asItem();
                var battery = batteries[tier - 1];
                var builder = shaped(RecipeCategory.MISC, item)
                        .define('G', tag(METALS[tier - 1], "gear"))
                        .define('S', Tags.Items.STRINGS)
                        .define('P', tag(METALS[tier - 1], "plate"))
                        .define('B', battery)
                        .pattern("GSG").pattern("PBP");
                if (previous == null) builder.define('X', tag("redstone", "grains"));
                else builder.define('X', previous);
                builder.pattern("PXP");
                var conditions = previous == null ? new ICondition[] {registered(item), registered(battery), registered(kite)}
                        : new ICondition[] {registered(item), registered(battery), registered(previous), registered(kite)};
                builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions), key("items/" + block.getId().getPath()));
                previous = item;
            }
        }

        private static ResourceKey<Recipe<?>> key(String path) {
            return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, path));
        }

        private static RegisteredCondition<Item> registered(Item item) {
            return new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item)));
        }

        /** The item tag of a material in a form of the resources mod, e.g. {@code c:plates/steel}. */
        private static TagKey<Item> tag(String material, String form) {
            var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
            return MaterialTags.item(Forms.get(form), m);
        }
    }
}
