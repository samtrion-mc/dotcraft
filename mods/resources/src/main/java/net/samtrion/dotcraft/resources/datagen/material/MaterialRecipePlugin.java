package net.samtrion.dotcraft.resources.datagen.material;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Forms;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.common.conditions.ConfigCondition;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The basic recipe chain of every material, made from the {@link Form#conversions() conversions} of the forms;
 * all recipes are disabled by {@code materials.enabled} and by the config switches of the material and of every
 * form the recipe makes or uses (see {@link ConfigCondition}) and carry the {@code neoforge:conditions} of the material and
 * of those forms (see {@link Conditions}), and are only made for the forms a material has:
 * <ul>
 *   <li>a {@code pack} conversion: shaped, the {@code ratio} items of the other form make one of the form, and
 *   shapeless, one of the form makes the {@code ratio} items back;</li>
 *   <li>a {@code cook} conversion: one item of the tag of the other form (all ore blocks for a hosted form) is smelted and
 *   blasted into one of the form, or, for an {@code external} material that lacks the form, into the item of its
 *   first {@link Material.External external} source that names one. Every further source (it names a mod) gets the
 *   same recipes with its own item and the recipe id suffix {@code _<mod>}; a source that names a mod adds the
 *   {@code mod_loaded} condition to its recipes, also the first one;</li>
 *   <li>a {@code craft} conversion: shapeless, {@code ratio} items of the tag of the other form make one of the form. An
 *   {@code external} material that lacks the other form uses the tag its sources name for it, one recipe per source
 *   (suffix and {@code mod_loaded} as for the cook).</li>
 * </ul>
 * Every ingredient is an item tag ({@code c:ingots/tin}, ...), only the results are items. The item of a source without
 * a mod must be registered, else the data generation fails; the item of a mod source need not be. A material with
 * {@code "cook": false} has no cook recipes. A crafting tool form (see {@link Form.ToolSpec}) has its own recipe, made of tags
 * only. Recipe ids are {@code material/<id>/<name>}.
 */
public final class MaterialRecipePlugin implements RecipePlugin {
    /** Cooking time in a furnace, ticks. */
    private static final int SMELT_TIME = 200;
    /** Cooking time in a blast furnace, ticks. */
    private static final int BLAST_TIME = 100;

    @Override
    public void buildRecipes(RecipeBuilders recipes, RecipeOutput all) {
        for (Material m : Materials.all()) {
            m.forms().stream().filter(f -> f.tool().isPresent()).forEach(f -> tool(recipes, all, m, f));
            for (Form to : Forms.all()) {
                for (Form.Conversion c : to.conversions()) {
                    Form from = Forms.get(c.from());
                    if (!c.materials().isEmpty() && !c.materials().contains(m.id())) continue;
                    if (c.kind() == Form.Conversion.Kind.CRAFT || c.kind() == Form.Conversion.Kind.CRUSH) {
                        if (m.has(to)) craft(recipes, all, m, from, to, c);
                        continue;
                    }
                    if (!m.has(from)) continue;
                    if (c.kind() == Form.Conversion.Kind.PACK) {
                        if (m.has(to)) pack(recipes, out(all, m, from, to, null), m, from, to, c.ratio());
                    } else if (m.cook()) {
                        cook(recipes, all, m, from, to, c);
                    }
                }
            }
        }
    }

    /**
     * Makes the recipe of a crafting tool: shaped with the pattern of its {@link Form.ToolSpec} where it has one, else
     * shapeless; {@code #} is the ingredient of the material, see {@link Material.Tool}. Only the tool itself is made, the
     * recipes that use it name its tag. The conditions are those of the material and the form.
     *
     * @param recipes the recipe builders
     * @param all     the output of the provider
     * @param m       the material
     * @param form    a tool form the material has
     */
    private static void tool(RecipeBuilders recipes, RecipeOutput all, Material m, Form form) {
        Form.ToolSpec spec = form.tool().orElseThrow();
        RecipeOutput output = all.withConditions(conditions(m, form, null, null).toArray(ICondition[]::new));
        TagKey<Item> head = TagKey.create(Registries.ITEM, m.tools().get(form.id()).ingredient());
        ItemLike result = ResourceItems.item(m, form).get();
        if (spec.pattern().isEmpty()) {
            var shapeless = recipes.shapeless(RecipeCategory.TOOLS, result, 1).requires(recipes.tag(head), m.tools().get(form.id()).count());
            spec.key().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(java.util.Map.Entry::getValue).forEach(tag -> shapeless.requires(recipes.tag(TagKey.create(Registries.ITEM, tag))));
            shapeless.unlockedBy(recipes.hasName(head), recipes.has(head)).save(output, key(m, form.id()));
            return;
        }
        var shaped = recipes.shaped(RecipeCategory.TOOLS, result).define('#', head);
        spec.key().forEach((symbol, tag) -> shaped.define(symbol.charAt(0), TagKey.create(Registries.ITEM, tag)));
        spec.pattern().forEach(shaped::pattern);
        shaped.unlockedBy(recipes.hasName(head), recipes.has(head)).save(output, key(m, form.id()));
    }

    /**
     * Makes the cook recipes of one conversion: into the item of the form, or into the external items.
     *
     * @param recipes the recipe builders
     * @param all     the output of the provider
     * @param m       the material
     * @param from    the form that is cooked
     * @param to      the form that comes out
     * @param c       the conversion
     */
    private static void cook(RecipeBuilders recipes, RecipeOutput all, Material m, Form from, Form to, Form.Conversion c) {
        String name = to.id() + "_from_" + from.id();
        TagKey<Item> input = MaterialTags.item(from, m);
        if (m.has(to)) {
            Identifier result = BuiltInRegistries.ITEM.getKey(ResourceItems.item(m, to).get());
            cook(recipes, out(all, m, from, to, null), m, input, result, name, "", c.xp());
            return;
        }
        List<Material.External> sources = m.external().stream().filter(e -> e.item(to.id()).isPresent()).toList();
        for (int i = 0; i < sources.size(); i++) {
            Material.External source = sources.get(i);
            Identifier result = source.item(to.id()).orElseThrow();
            if (source.mod().isEmpty() && !BuiltInRegistries.ITEM.containsKey(result)) {
                throw new IllegalStateException(m.id() + ": the external item " + result + " of \"" + to.id() + "\" is not registered");
            }
            String suffix = i == 0 ? "" : "_" + source.mod().orElseThrow();
            cook(recipes, out(all, m, from, null, source), m, input, result, name, suffix, c.xp());
        }
    }

    /**
     * Makes the craft recipes of one conversion: from the form of the material, or from the tag that each external source
     * names for it.
     *
     * @param recipes the recipe builders
     * @param all     the output of the provider
     * @param m       the material, which has the form {@code to}
     * @param from    the form that is used
     * @param to      the form that is made
     * @param c       the conversion
     */
    private static void craft(RecipeBuilders recipes, RecipeOutput all, Material m, Form from, Form to, Form.Conversion c) {
        String name = to.id() + "_from_" + from.id();
        if (m.has(from)) {
            craft(recipes, out(all, m, from, to, null), m, MaterialTags.item(from, m), to, c, name);
            return;
        }
        List<Material.External> sources = m.external().stream().filter(e -> e.tag(from.id()).isPresent()).toList();
        for (int i = 0; i < sources.size(); i++) {
            Material.External source = sources.get(i);
            TagKey<Item> input = TagKey.create(Registries.ITEM, source.tag(from.id()).orElseThrow());
            String suffix = i == 0 ? "" : "_" + source.mod().orElseThrow();
            craft(recipes, out(all, m, to, null, source), m, input, to, c, name + suffix);
        }
    }

    /**
     * One shapeless recipe: {@code ratio} items of a tag make one item of a form, or, for a crush, one item of the tag and a
     * hammer make {@code ratio} items of the form; the name ends the recipe id.
     */
    private static void craft(RecipeBuilders recipes, RecipeOutput output, Material m, TagKey<Item> input, Form to,
                              Form.Conversion c, String name) {
        boolean crush = c.kind() == Form.Conversion.Kind.CRUSH;
        var builder = recipes.shapeless(RecipeCategory.MISC, ResourceItems.item(m, to).get(), crush ? c.ratio() : 1)
                .requires(recipes.tag(input), crush ? 1 : c.ratio());
        if (crush) builder.requires(recipes.tag(ResourceBlockTags.HAMMERS));
        builder.unlockedBy(recipes.hasName(input), recipes.has(input)).save(output, key(m, name));
    }

    /**
     * The output of the recipes between forms of a material: the recipes are made only while {@code materials.enabled}
     * and the switches of the material and of the given forms are on, and while the mod of the source is loaded.
     *
     * @param all    the output of the provider
     * @param m      the material
     * @param from   the form that is used
     * @param to     the form that is made, {@code null} where the result is an external item
     * @param source the external source of the result, {@code null} where the result is an item of the material
     * @return the output that adds the conditions
     */
    private static RecipeOutput out(RecipeOutput all, Material m, Form from, @Nullable Form to, Material.@Nullable External source) {
        return all.withConditions(conditions(m, from, to, source).toArray(ICondition[]::new));
    }

    /**
     * The conditions of the recipes between forms of a material: the config switches and {@code mod_loaded} of the
     * source, then the {@code neoforge:conditions} of the material and of the forms that the recipe uses and makes, so
     * the recipe is only loaded where all of them are.
     *
     * @param m      the material
     * @param from   the form that is used
     * @param to     the form that is made, {@code null} where the result is an external item
     * @param source the external source of the result, {@code null} where the result is an item of the material
     * @return the conditions without repeats
     */
    static List<ICondition> conditions(Material m, Form from, @Nullable Form to, Material.@Nullable External source) {
        List<ICondition> conditions = new ArrayList<>();
        conditions.add(new ConfigCondition(ConfigCondition.Switch.MATERIALS));
        conditions.add(ConfigCondition.ofForm(m.id(), from.id()));
        if (to != null) conditions.add(ConfigCondition.ofForm(m.id(), to.id()));
        if (source != null && source.mod().isPresent()) conditions.add(new ModLoadedCondition(source.mod().get()));
        return Conditions.merge(conditions, m.conditions(from), to == null ? List.of() : m.conditions(to));
    }

    /** Several of the small form make one big form (shaped), and one big form makes the small ones (shapeless); the inputs are tags. */
    private static void pack(RecipeBuilders recipes, RecipeOutput output, Material m, Form small, Form big, int ratio) {
        TagKey<Item> smallTag = MaterialTags.item(small, m);
        TagKey<Item> bigTag = MaterialTags.item(big, m);
        ItemLike smallItem = ResourceItems.item(m, small).get();
        ItemLike bigItem = ResourceItems.item(m, big).get();
        var shaped = recipes.shaped(big.isBlock() ? RecipeCategory.BUILDING_BLOCKS : RecipeCategory.MISC, bigItem)
                .define('#', smallTag);
        if (ratio == 4) shaped.pattern("##").pattern("##");
        else shaped.pattern("###").pattern("###").pattern("###");
        shaped.unlockedBy(recipes.hasName(smallTag), recipes.has(smallTag))
                .save(output, key(m, big.id() + "_from_" + small.id()));
        recipes.shapeless(RecipeCategory.MISC, smallItem, ratio)
                .requires(recipes.tag(bigTag))
                .unlockedBy(recipes.hasName(bigTag), recipes.has(bigTag))
                .save(output, key(m, small.id() + "_from_" + big.id()));
    }

    /** Smelting and blasting of every item of a tag into a result; the suffix ends the recipe ids. */
    private static void cook(RecipeBuilders recipes, RecipeOutput output, Material m, TagKey<Item> input, Identifier result,
                             String name, String suffix, float xp) {
        recipes.smelting(input, result, xp, SMELT_TIME)
                .unlockedBy(recipes.hasName(input), recipes.has(input))
                .save(output, key(m, name + "_smelting" + suffix));
        recipes.blasting(input, result, xp, BLAST_TIME)
                .unlockedBy(recipes.hasName(input), recipes.has(input))
                .save(output, key(m, name + "_blasting" + suffix));
    }

    /** The key of a recipe of a material, {@code material/<id>/<name>}. */
    private static ResourceKey<Recipe<?>> key(Material m, String name) {
        return DataGenIds.recipeKey("material/" + m.id() + "/" + name);
    }
}
