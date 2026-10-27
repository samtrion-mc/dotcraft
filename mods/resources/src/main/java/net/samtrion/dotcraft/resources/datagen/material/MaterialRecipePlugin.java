package net.samtrion.dotcraft.resources.datagen.material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 *   <li>a {@code smelt} or {@code blast} conversion: one item of the tag of the other form (all ore blocks for a hosted form) is
 *   smelted in a furnace and blasted in a blast furnace (a {@code smelt}) or only blasted (a {@code blast}) into one of the form, or, for an {@code external} material that lacks the form, into the item of its
 *   first {@link Material.External external} source that names one. Every further source (it names a mod) gets the
 *   same recipes with its own item and the recipe id suffix {@code _<mod>}; a source that names a mod adds the
 *   {@code mod_loaded} condition to its recipes, also the first one;</li>
 *   <li>a {@code forge} conversion: shaped, a hammer over a row of {@code ratio} items of the other form make one; a {@code crush} conversion: a hammer and one item make {@code ratio} items; the hammer is
 *   the tag of the tier of the material ({@link ResourceBlockTags#hammers}), so a hammer of a lower tier cannot work it, and
 *   the recipe carries the {@link ConfigCondition.Switch#TOOL} condition of that tier, so it is dropped where no such hammer is
 *   enabled; a {@code shaped} conversion: a shaped
 *   recipe of its {@code pattern} and {@code key}, see {@link Form.Conversion};</li>
 *   <li>a {@code craft} conversion: shapeless, {@code ratio} items of the tag of the other form make one of the form. An
 *   {@code external} material that lacks the other form uses the tag its sources name for it, one recipe per source
 *   (suffix and {@code mod_loaded} as for the smelt).</li>
 * </ul>
 * Every ingredient is an item tag ({@code c:ingots/tin}, ...), only the results are items. The item of a source without
 * a mod must be registered, else the data generation fails; the item of a mod source need not be. A material with
 * {@code "smelt": false} has no smelt and blast recipes. A crafting tool form (see {@link Form.ToolSpec}) has its own recipe, made of tags
 * only. Recipe ids are {@code material/<id>/<name>}, where the name is
 * <ul>
 *   <li>{@code <to>_from_<from>} for a pack (both directions) and a craft;</li>
 *   <li>{@code <to>_from_<from>_smelting} and {@code <to>_from_<from>_blasting} for a smelt or blast;</li>
 *   <li>{@code <to>_from_<from>_crushing} for a crush and {@code <to>_from_<from>_forging} for a forge;</li>
 *   <li>{@code <to>} for a shaped or shapeless conversion ({@code gear}, {@code hammer}, {@code sandpaper});</li>
 * </ul>
 * and a source of another mod adds {@code _<mod>} at the end; the alloys, see {@link MaterialAlloyRecipePlugin}, are
 * {@code <to>_from_alloy}.
 */
public final class MaterialRecipePlugin implements RecipePlugin {
    /** Cooking time in a furnace, ticks. */
    private static final int SMELT_TIME = 200;
    /** Cooking time in a blast furnace, ticks. */
    private static final int BLAST_TIME = 100;

    @Override
    public void buildRecipes(RecipeBuilders recipes, RecipeOutput all) {
        for (Material m : Materials.all()) {
            for (Form to : Forms.all()) {
                for (Form.Conversion c : to.conversions()) {
                    if (c.kind() == Form.Conversion.Kind.ALLOY) continue; // made by MaterialAlloyRecipePlugin
                    if (!c.materials().isEmpty() && !c.materials().contains(m.id())) continue;
                    if (c.kind() == Form.Conversion.Kind.SHAPED || c.kind() == Form.Conversion.Kind.SHAPELESS) {
                        if (m.has(to)) shaped(recipes, all, m, to, c);
                        continue;
                    }
                    Form from = Forms.get(c.from());
                    if (c.kind() == Form.Conversion.Kind.CRAFT || c.kind() == Form.Conversion.Kind.CRUSH || c.kind() == Form.Conversion.Kind.FORGE) {
                        if (m.has(to)) craft(recipes, all, m, from, to, c);
                        continue;
                    }
                    if (!m.has(from)) continue;
                    if (c.kind() == Form.Conversion.Kind.PACK) {
                        if (m.has(to)) pack(recipes, out(all, m, from, to, null), m, from, to, c.ratio());
                        else packExternal(recipes, all, m, from, to, c.ratio());
                    } else if (m.smelt()) {
                        smelt(recipes, all, m, from, to, c);
                    }
                }
            }
        }
    }

    /**
     * Makes the smelting and blasting recipes of one conversion: into the item of the form, or into the external items.
     *
     * @param recipes the recipe builders
     * @param all     the output of the provider
     * @param m       the material
     * @param from    the form that is smelted or blasted
     * @param to      the form that comes out
     * @param c       the conversion
     */
    private static void smelt(RecipeBuilders recipes, RecipeOutput all, Material m, Form from, Form to, Form.Conversion c) {
        String name = to.id() + "_from_" + from.id();
        TagKey<Item> input = MaterialTags.item(from, m);
        if (m.has(to)) {
            Identifier result = BuiltInRegistries.ITEM.getKey(ResourceItems.item(m, to).get());
            furnace(recipes, out(all, m, from, to, null), m, input, result, name, "", c);
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
            furnace(recipes, out(all, m, from, null, source), m, input, result, name, suffix, c);
        }
    }

    /**
     * Makes the craft recipes of one conversion: from the form of the material, or from the tag that each external source
     * names for it. A crush or forge also needs the config switch of a hammer of the tier of the material.
     *
     * @param recipes the recipe builders
     * @param all     the output of the provider
     * @param m       the material, which has the form {@code to}
     * @param from    the form that is used
     * @param to      the form that is made
     * @param c       the conversion
     */
    private static void craft(RecipeBuilders recipes, RecipeOutput all, Material m, Form from, Form to, Form.Conversion c) {
        String name = to.id() + "_from_" + from.id() + (switch (c.kind()) {
            case CRUSH -> "_crushing";
            case FORGE -> "_forging";
            default -> "";
        });
        List<ICondition> hammer = c.kind() == Form.Conversion.Kind.CRUSH || c.kind() == Form.Conversion.Kind.FORGE
                ? List.of(ConfigCondition.ofTool(ResourceBlockTags.HAMMER_FORM, m.tier())) : List.of();
        if (m.has(from)) {
            craft(recipes, out(all, m, from, to, null, hammer), m, MaterialTags.item(from, m), to, c, name);
            return;
        }
        List<Material.External> sources = m.external().stream().filter(e -> e.tag(from.id()).isPresent()).toList();
        for (int i = 0; i < sources.size(); i++) {
            Material.External source = sources.get(i);
            TagKey<Item> input = TagKey.create(Registries.ITEM, source.tag(from.id()).orElseThrow());
            String suffix = i == 0 ? "" : "_" + source.mod().orElseThrow();
            craft(recipes, out(all, m, to, null, source, hammer), m, input, to, c, name + suffix);
        }
    }

    /**
     * One shapeless recipe: {@code ratio} items of a tag make one item of a form, or, for a crush, one item of the tag and a
     * hammer of the tier of the material make {@code ratio} items of the form; the name ends the recipe id.
     */
    private static void craft(RecipeBuilders recipes, RecipeOutput output, Material m, TagKey<Item> input, Form to,
                              Form.Conversion c, String name) {
        if (c.kind() == Form.Conversion.Kind.FORGE) {
            forge(recipes, output, m, input, to, c.ratio(), name);
            return;
        }
        boolean crush = c.kind() == Form.Conversion.Kind.CRUSH;
        var builder = recipes.shapeless(RecipeCategory.MISC, ResourceItems.item(m, to).get(), crush ? c.ratio() : 1)
                .requires(recipes.tag(input), crush ? 1 : c.ratio());
        if (crush) builder.requires(recipes.tag(ResourceBlockTags.hammers(m.tier())));
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
        return out(all, m, from, to, source, List.of());
    }

    /**
     * Like {@link #out(RecipeOutput, Material, Form, Form, Material.External)}, with further conditions of the recipe.
     *
     * @param all    the output of the provider
     * @param m      the material
     * @param from   the form that is used
     * @param to     the form that is made, {@code null} where the result is an external item
     * @param source the external source of the result, {@code null} where the result is an item of the material
     * @param extra  the further conditions, e.g. that a hammer is enabled
     * @return the output that adds the conditions
     */
    private static RecipeOutput out(RecipeOutput all, Material m, Form from, @Nullable Form to, Material.@Nullable External source,
                                    List<ICondition> extra) {
        return all.withConditions(conditions(m, from, to, source, extra).toArray(ICondition[]::new));
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
        return conditions(m, from, to, source, List.of());
    }

    /**
     * The conditions of the recipes between forms of a material with further conditions, which come after {@code mod_loaded}
     * of the source and before the {@code neoforge:conditions}.
     *
     * @param m      the material
     * @param from   the form that is used
     * @param to     the form that is made, {@code null} where the result is an external item
     * @param source the external source of the result, {@code null} where the result is an item of the material
     * @param extra  the further conditions, e.g. {@link ConfigCondition#ofTool} for a recipe that needs a hammer
     * @return the conditions without repeats
     */
    static List<ICondition> conditions(Material m, Form from, @Nullable Form to, Material.@Nullable External source, List<ICondition> extra) {
        List<ICondition> conditions = new ArrayList<>();
        conditions.add(new ConfigCondition(ConfigCondition.Switch.MATERIALS));
        conditions.add(ConfigCondition.ofForm(m.id(), from.id()));
        if (to != null) conditions.add(ConfigCondition.ofForm(m.id(), to.id()));
        if (source != null && source.mod().isPresent()) conditions.add(new ModLoadedCondition(source.mod().get()));
        conditions.addAll(extra);
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

    /**
     * A {@code forge} is a shaped recipe: the hammer tag of the tier of the material sits over a row of {@code ratio} items of the
     * tag, e.g. {@code " H "} over {@code "III"} for three, and makes one item of the form.
     */
    private static void forge(RecipeBuilders recipes, RecipeOutput output, Material m, TagKey<Item> input, Form to, int ratio,
                              String name) {
        var shaped = recipes.shaped(RecipeCategory.MISC, ResourceItems.item(m, to).get())
                .define('H', ResourceBlockTags.hammers(m.tier())).define('I', input)
                .pattern(" ".repeat((ratio - 1) / 2) + "H" + " ".repeat(ratio / 2))
                .pattern("I".repeat(ratio));
        shaped.unlockedBy(recipes.hasName(input), recipes.has(input)).save(output, key(m, name));
    }

    /**
     * Makes the recipe of a {@code shaped} or {@code shapeless} conversion. Each symbol of the key is an item tag, or the tag of a
     * form of the material, which, where the material lacks the form, is the tag its first source that names one gives for it.
     * The symbol {@code #} is the ingredient of the material for the tool form (its {@code tools} entry), and in a shapeless recipe
     * it stands for as many items as the {@code count} of that entry. The recipe is skipped where a form of the key is missing in
     * both, and where the tool form is used by a material without a {@code tools} entry.
     */
    private static void shaped(RecipeBuilders recipes, RecipeOutput all, Material m, Form to, Form.Conversion c) {
        Map<Character, TagKey<Item>> tags = new LinkedHashMap<>();
        Form first = null;
        Material.External external = null;
        for (var entry : c.key().entrySet()) {
            String value = entry.getValue();
            if (value.contains(":")) {
                tags.put(entry.getKey().charAt(0), TagKey.create(Registries.ITEM, Identifier.parse(value)));
            } else if (m.has(value)) {
                Form form = Forms.get(value);
                first = first == null ? form : first;
                tags.put(entry.getKey().charAt(0), MaterialTags.item(form, m));
            } else {
                Optional<Material.External> source = m.external().stream().filter(e -> e.tag(value).isPresent()).findFirst();
                if (source.isEmpty()) return;
                external = external == null ? source.get() : external;
                tags.put(entry.getKey().charAt(0), TagKey.create(Registries.ITEM, source.get().tag(value).orElseThrow()));
            }
        }
        Material.Tool tool = m.tools().get(to.id());
        if (c.pattern().stream().anyMatch(r -> r.contains(Form.ToolSpec.MATERIAL))) {
            if (tool == null) return;
            tags.put('#', TagKey.create(Registries.ITEM, tool.ingredient()));
        }
        ItemLike result = ResourceItems.item(m, to).get();
        RecipeCategory category = to.tool().isPresent() ? RecipeCategory.TOOLS : RecipeCategory.MISC;
        TagKey<Item> unlock = tags.values().iterator().next();
        RecipeOutput output = first != null ? out(all, m, first, to, external) : out(all, m, to, null, external);
        if (c.kind() == Form.Conversion.Kind.SHAPELESS) {
            var shapeless = recipes.shapeless(category, result, 1);
            for (char symbol : c.pattern().get(0).toCharArray()) {
                if (symbol != '#') shapeless.requires(recipes.tag(tags.get(symbol)));
            }
            if (tags.containsKey('#')) shapeless.requires(recipes.tag(tags.get('#')), tool.count());
            shapeless.unlockedBy(recipes.hasName(unlock), recipes.has(unlock)).save(output, key(m, to.id()));
            return;
        }
        var shaped = recipes.shaped(category, result);
        tags.forEach(shaped::define);
        c.pattern().forEach(shaped::pattern);
        shaped.unlockedBy(recipes.hasName(unlock), recipes.has(unlock)).save(output, key(m, to.id()));
    }

    /**
     * The pack of a form whose big form is an external item (nine redstone grains make the vanilla redstone, which makes nine
     * grains back): nine of the small form are crafted shapeless into the external item, and its tag crafts the small ones.
     * Made only for a material that has the small form and whose source names a tag and an item for the big form.
     */
    private static void packExternal(RecipeBuilders recipes, RecipeOutput all, Material m, Form small, Form big, int ratio) {
        if (!m.has(small) || ratio == 4) return;
        for (Material.External source : m.external().stream().filter(e -> e.item(big.id()).isPresent() && e.tag(big.id()).isPresent()).toList()) {
            TagKey<Item> smallTag = MaterialTags.item(small, m);
            TagKey<Item> bigTag = TagKey.create(Registries.ITEM, source.tag(big.id()).orElseThrow());
            Identifier bigItem = source.item(big.id()).orElseThrow();
            RecipeOutput output = out(all, m, small, null, source);
            recipes.shapeless(RecipeCategory.MISC, BuiltInRegistries.ITEM.getValue(bigItem), 1)
                    .requires(recipes.tag(smallTag), ratio)
                    .unlockedBy(recipes.hasName(smallTag), recipes.has(smallTag))
                    .save(output, key(m, big.id() + "_from_" + small.id()));
            recipes.shapeless(RecipeCategory.MISC, ResourceItems.item(m, small).get(), ratio)
                    .requires(recipes.tag(bigTag))
                    .unlockedBy(recipes.hasName(bigTag), recipes.has(bigTag))
                    .save(output, key(m, small.id() + "_from_" + big.id()));
        }
    }

    /** Smelting and blasting (a {@code smelt}) or only blasting (a {@code blast}) of every item of a tag into a result; the suffix ends the recipe ids. */
    private static void furnace(RecipeBuilders recipes, RecipeOutput output, Material m, TagKey<Item> input, Identifier result,
                             String name, String suffix, Form.Conversion c) {
        if (c.kind() == Form.Conversion.Kind.SMELT) {
            recipes.smelting(input, result, c.xp(), SMELT_TIME)
                    .unlockedBy(recipes.hasName(input), recipes.has(input))
                    .save(output, key(m, name + "_smelting" + suffix));
        }
        recipes.blasting(input, result, c.xp(), BLAST_TIME)
                .unlockedBy(recipes.hasName(input), recipes.has(input))
                .save(output, key(m, name + "_blasting" + suffix));
    }

    /** The key of a recipe of a material, {@code material/<id>/<name>}. */
    private static ResourceKey<Recipe<?>> key(Material m, String name) {
        return DataGenIds.recipeKey("material/" + m.id() + "/" + name);
    }
}
