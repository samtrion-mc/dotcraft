package net.samtrion.dotcraft.energy.melt.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The recipes of the melt generators, shaped: a blaze rod (the heat) between two plates, two gears around the battery of the tier,
 * and below two plates around the generator of the tier below (tier 1: redstone grains). The metals are copper, brass, invar,
 * platinum and iridium plates and gears; every ingredient is a tag but the own items of the mod (the battery and the lower
 * generator), which are used by id as the documented exception. The recipes need the result and the own items to be registered.
 */
public final class MeltRecipes extends RecipeProvider {
    private static final String[] METALS = {"copper", "brass", "invar", "platinum", "iridium"};

    private final RecipeOutput output;

    /**
     * Creates the recipes.
     *
     * @param registries the registries
     * @param output     the recipe output
     */
    public MeltRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
        this.output = output;
    }

    /** Writes the recipes; the {@code buildRecipes} of the base class is protected. */
    public void build() {
        buildRecipes();
    }

    @Override
    protected void buildRecipes() {
        Item[] batteries = {EnergyItems.BATTERY_TIER_1.get(), EnergyItems.BATTERY_TIER_2.get(), EnergyItems.BATTERY_TIER_3.get(),
                EnergyItems.BATTERY_TIER_4.get(), EnergyItems.BATTERY_TIER_5.get()};
        Item previous = null;
        for (int tier = 1; tier <= METALS.length; tier++) {
            var block = MeltBlocks.block(tier);
            // a tier above a missing one gets no recipe, so a switched-off tier cannot be skipped (only tier 1 uses grains)
            if (block == null || (tier > 1 && previous == null)) {
                previous = null;
                continue;
            }
            var item = block.get().asItem();
            var battery = batteries[tier - 1];
            var builder = shaped(RecipeCategory.MISC, item)
                    .define('R', Tags.Items.RODS_BLAZE)
                    .define('P', tag(METALS[tier - 1], "plate"))
                    .define('G', tag(METALS[tier - 1], "gear"))
                    .define('B', battery)
                    .pattern("PRP").pattern("GBG");
            if (previous == null) builder.define('X', tag("redstone", "grains"));
            else builder.define('X', previous);
            builder.pattern("PXP");
            var conditions = previous == null ? new ICondition[] {registered(item), registered(battery)}
                    : new ICondition[] {registered(item), registered(battery), registered(previous)};
            builder.unlockedBy(getHasName(battery), has(battery)).save(output.withConditions(conditions),
                    ResourceKey.<Recipe<?>>create(Registries.RECIPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "items/" + block.getId().getPath())));
            previous = item;
        }
    }

    private static RegisteredCondition<Item> registered(Item item) {
        return new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item)));
    }

    /** The item tag of a material in a form of the resources mod, e.g. {@code c:plates/copper}. */
    private static TagKey<Item> tag(String material, String form) {
        var m = Materials.all().stream().filter(x -> x.id().equals(material)).findFirst().orElseThrow();
        return MaterialTags.item(Forms.get(form), m);
    }
}
