package net.samtrion.dotcraft.resources.datagen.material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.samtrion.dotcraft.resources.api.RecipeBuilders;
import net.samtrion.dotcraft.resources.api.RecipePlugin;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.MaterialTags;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.common.conditions.ConfigCondition;
import net.samtrion.dotcraft.resources.datagen.DataGenIds;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The mixing recipes of the alloys, disabled by {@code materials.enabled}, by the config switches of the alloy form and
 * by the switches of every input (the form of an input material that has it, else the input material, see
 * {@link ConfigCondition}). For each material with an {@code alloy} and each of its forms with a {@code mix} entry
 * (dust and grains), one shapeless recipe turns the item tag of the same form of every input
 * material into the alloy form, making the {@code result} of the alloy, else as many items as the inputs have items together. An input material that lacks
 * a form is resolved through the tag of its {@code external} sources (the key of the {@code mix} entry of the form);
 * when that fails too, the material is no {@link Option} of that form.
 *
 * <p>An input that lists alternatives ({@code "material": ["coal", "charcoal"]}) and an input material with several
 * external sources each give several options; one recipe is made for every combination of the options of the inputs,
 * so no recipe has to accept several tags and two recipes never share their ingredients. A recipe is skipped when an
 * input has no option at all. The id of the recipe is {@code material/<id>/<form>_from_mix}, followed for each input in
 * order by {@code _<material>} where the input has alternatives and by {@code _<mod>} where the option is a source of a mod,
 * e.g. {@code material/steel/ingot_from_mix_charcoal}. The conditions of a recipe are those of the
 * alloy form (with its {@code neoforge:conditions} and those of the alloy, see {@link Conditions}), of every option (the form or material switch of the option material and {@code mod_loaded} of a mod
 * source) and, for an input with alternatives, the switch {@code materials.<id>.recipes.<material>.enabled} of the chosen
 * alternative. Every option also adds the {@code neoforge:conditions} of its material, and of its form where it has it, so
 * the recipe is dropped where an input is left out. An unknown input material or more than nine items in all fail the data generation. The mixed dust is
 * smelted to an ingot by {@link MaterialRecipePlugin}.
 */
public final class MaterialAlloyRecipePlugin implements RecipePlugin {
    /** Size of the crafting grid, the most items of one recipe. */
    private static final int GRID = 9;

    /**
     * One way to supply an input of an alloy in a form.
     *
     * @param material the material that supplies the input
     * @param source   its external source where it lacks the form, else {@code null}
     * @param tag      the item tag that is the ingredient
     */
    public record Option(Material material, Material.@Nullable External source, TagKey<Item> tag) {}

    @Override
    public void buildRecipes(RecipeBuilders recipes, RecipeOutput all) {
        for (Material m : Materials.all()) {
            if (m.alloy().isEmpty()) continue;
            for (Form form : m.forms()) {
                if (form.mix().isPresent()) mix(recipes, all, m, form);
            }
        }
    }

    /** Makes the recipes of one form, one for every combination of the options of the inputs. */
    private static void mix(RecipeBuilders recipes, RecipeOutput all, Material m, Form form) {
        List<Material.Alloy.Input> inputs = m.alloy().orElseThrow().inputs();
        int total = inputs.stream().mapToInt(Material.Alloy.Input::count).sum();
        if (total > GRID) throw new IllegalStateException(m.id() + ": alloy inputs have " + total + " items, the grid holds " + GRID);
        for (List<Option> choice : combinations(m, form, Materials.all())) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < inputs.size(); i++) {
                Option option = choice.get(i);
                if (inputs.get(i).hasAlternatives()) suffix.append('_').append(option.material().id());
                if (option.source() != null && option.source().mod().isPresent()) suffix.append('_').append(option.source().mod().get());
            }
            ShapelessRecipeBuilder builder = recipes.shapeless(RecipeCategory.MISC, ResourceItems.item(m, form).get(), m.alloy().orElseThrow().yield());
            for (int i = 0; i < inputs.size(); i++) builder.requires(recipes.tag(choice.get(i).tag()), inputs.get(i).count());
            TagKey<Item> first = choice.get(0).tag();
            builder.unlockedBy(recipes.hasName(first), recipes.has(first))
                    .save(all.withConditions(conditions(m, form, choice).toArray(ICondition[]::new)),
                            DataGenIds.recipeKey("material/" + m.id() + "/" + form.id() + "_from_mix" + suffix));
        }
    }

    /**
     * The conditions of one alloy recipe: the config switches, then {@code mod_loaded} of a mod source, then the
     * {@code neoforge:conditions} of the alloy form and of every option, the form of an input material that has it, else
     * the material.
     *
     * @param m      the alloy
     * @param form   the form of the recipe
     * @param choice one option per input
     * @return the conditions without repeats
     */
    static List<ICondition> conditions(Material m, Form form, List<Option> choice) {
        List<Material.Alloy.Input> inputs = m.alloy().orElseThrow().inputs();
        Set<ICondition> conditions = new LinkedHashSet<>();
        conditions.add(new ConfigCondition(ConfigCondition.Switch.MATERIALS));
        conditions.add(ConfigCondition.ofForm(m.id(), form.id()));
        for (int i = 0; i < inputs.size(); i++) {
            Option option = choice.get(i);
            Material source = option.material();
            conditions.add(source.has(form) ? ConfigCondition.ofForm(source.id(), form.id()) : ConfigCondition.ofMaterial(source.id()));
            if (inputs.get(i).hasAlternatives()) conditions.add(ConfigCondition.ofRecipe(m.id(), source.id()));
            if (option.source() != null && option.source().mod().isPresent()) conditions.add(new ModLoadedCondition(option.source().mod().get()));
        }
        List<ICondition> result = new ArrayList<>(conditions);
        result.addAll(m.conditions(form));
        for (Option option : choice) result.addAll(option.material().has(form) ? option.material().conditions(form) : option.material().conditions());
        return Conditions.merge(result);
    }

    /**
     * Every combination of the options of the inputs of an alloy in a form, in source order.
     *
     * @param alloy the alloy
     * @param form  the form of the recipe
     * @param all   all materials
     * @return one list per recipe with one option per input; empty when an input has no option; fails when a material is unknown
     */
    public static List<List<Option>> combinations(Material alloy, Form form, List<Material> all) {
        List<List<Option>> result = new ArrayList<>();
        result.add(List.of());
        for (Material.Alloy.Input in : alloy.alloy().orElseThrow().inputs()) {
            List<Option> options = options(alloy, in, form, all);
            List<List<Option>> next = new ArrayList<>();
            for (List<Option> prefix : result) {
                for (Option option : options) {
                    List<Option> list = new ArrayList<>(prefix);
                    list.add(option);
                    next.add(list);
                }
            }
            result = next;
        }
        return result;
    }

    /**
     * The ways to supply an input in a form: for each alternative material the tag of the form where it has the
     * form, else one option per {@code external} source that has the tag of the key of the {@code mix} entry of the form.
     *
     * @param alloy the alloy
     * @param in    the input
     * @param form  the form of the recipe
     * @param all   all materials
     * @return the options, empty when the input cannot give that form; fails when a material is unknown
     */
    public static List<Option> options(Material alloy, Material.Alloy.Input in, Form form, List<Material> all) {
        List<Option> options = new ArrayList<>();
        for (String id : in.materials()) {
            Material source = all.stream().filter(x -> x.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(alloy.id() + ": unknown alloy input material " + id));
            if (source.has(form)) {
                options.add(new Option(source, null, MaterialTags.item(form, source)));
                continue;
            }
            Optional<String> key = form.mix().flatMap(Form.Mix::external);
            if (key.isEmpty()) continue;
            for (Material.External external : source.external()) {
                external.tag(key.get()).ifPresent(tag -> options.add(new Option(source, external, TagKey.create(Registries.ITEM, tag))));
            }
        }
        return options;
    }
}
