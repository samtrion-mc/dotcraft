package net.samtrion.dotcraft.resources.common.conditions;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;

/**
 * Data condition that holds while a switch of the config is on; attached to the recipes of the materials and
 * to the biome modifiers of the world generation. The global switches are
 * <pre>{"type": "dotcraftresources:config", "switch": "materials"}</pre> and {@code "switch": "worldgen"}; a
 * single material is switched by
 * <pre>{"type": "dotcraftresources:config", "switch": "material", "material": "tin"}</pre> and a form of a
 * material by
 * <pre>{"type": "dotcraftresources:config", "switch": "material_form", "material": "tin", "form": "gear"}</pre>
 * where {@code form} is the id of the form (the config key of {@code gear} is {@code gears}), and an alternative recipe of an
 * alloy (see {@code Material.Alloy.Input}) by
 * <pre>{"type": "dotcraftresources:config", "switch": "material_recipe", "material": "steel", "recipe": "charcoal"}</pre>
 * where {@code recipe} is the id of the alternative input material, and one ore generation entry of a material (see
 * {@code Material.OreSpec}) by
 * <pre>{"type": "dotcraftresources:config", "switch": "material_ore", "material": "tin", "ore": "surface"}</pre>
 * where {@code ore} is the id of the entry ({@code materials.tin.ores.surface.enabled}). A material switch
 * also needs {@code materials.enabled}, a form, recipe or ore switch also the switch of its material (the form switch of the
 * ores is a condition of its own). A material, form, recipe or ore that is unknown counts as disabled.
 *
 * @param switchId the switch
 * @param material the id of the material, only for the material, form and recipe switches, else empty
 * @param form     the id of the form, only for {@link Switch#MATERIAL_FORM}, else empty
 * @param recipe   the id of the alternative input material, only for {@link Switch#MATERIAL_RECIPE}, else empty
 * @param ore      the id of the ore entry, only for {@link Switch#MATERIAL_ORE}, else empty
 */
public record ConfigCondition(Switch switchId, String material, String form, String recipe, String ore) implements ICondition {
    /** The switches of the config. */
    public enum Switch implements StringRepresentable {
        /** {@code materials.enabled}. */
        MATERIALS,
        /** {@code worldgen.enabled}. */
        WORLDGEN,
        /** {@code materials.<material>.enabled}, with {@code materials.enabled}. */
        MATERIAL,
        /** {@code materials.<material>.<configKey>.enabled}, with the switches of the material. */
        MATERIAL_FORM,
        /** {@code materials.<material>.recipes.<alternative>.enabled}, with the switches of the material. */
        MATERIAL_RECIPE,
        /** {@code materials.<material>.ores.<entry>.enabled}, with the switches of the material. */
        MATERIAL_ORE;

        /** Codec that reads and writes the lower case name. */
        public static final Codec<Switch> CODEC = StringRepresentable.fromEnum(Switch::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Serialized as in the class description; {@code material}, {@code form}, {@code recipe} and {@code ore} are left out where empty. */
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.<ConfigCondition>mapCodec(i -> i.group(
            Switch.CODEC.fieldOf("switch").forGetter(ConfigCondition::switchId),
            Codec.STRING.optionalFieldOf("material", "").forGetter(ConfigCondition::material),
            Codec.STRING.optionalFieldOf("form", "").forGetter(ConfigCondition::form),
            Codec.STRING.optionalFieldOf("recipe", "").forGetter(ConfigCondition::recipe),
            Codec.STRING.optionalFieldOf("ore", "").forGetter(ConfigCondition::ore)
    ).apply(i, ConfigCondition::new)).flatXmap(ConfigCondition::validate, ConfigCondition::validate);

    /**
     * A switch without an ore entry.
     *
     * @param switchId the switch
     * @param material the id of the material, or empty
     * @param form     the id of the form, or empty
     * @param recipe   the id of the alternative input material, or empty
     */
    public ConfigCondition(Switch switchId, String material, String form, String recipe) {
        this(switchId, material, form, recipe, "");
    }

    /**
     * A global switch.
     *
     * @param switchId {@link Switch#MATERIALS} or {@link Switch#WORLDGEN}
     */
    public ConfigCondition(Switch switchId) {
        this(switchId, "", "", "", "");
    }

    /**
     * The switch of a material.
     *
     * @param materialId the id of the material
     * @return the condition
     */
    public static ConfigCondition ofMaterial(String materialId) {
        return new ConfigCondition(Switch.MATERIAL, materialId, "", "");
    }

    /**
     * The switch of a form of a material.
     *
     * @param materialId the id of the material
     * @param formId     the id of the form
     * @return the condition
     */
    public static ConfigCondition ofForm(String materialId, String formId) {
        return new ConfigCondition(Switch.MATERIAL_FORM, materialId, formId, "");
    }

    /**
     * The switch of an alternative recipe of an alloy.
     *
     * @param materialId   the id of the alloy
     * @param alternativeId the id of the alternative input material, e.g. {@code charcoal}
     * @return the condition
     */
    public static ConfigCondition ofRecipe(String materialId, String alternativeId) {
        return new ConfigCondition(Switch.MATERIAL_RECIPE, materialId, "", alternativeId);
    }

    /**
     * The switch of one ore generation entry of a material.
     *
     * @param materialId the id of the material
     * @param oreId      the id of the entry, e.g. {@code surface}
     * @return the condition
     */
    public static ConfigCondition ofOre(String materialId, String oreId) {
        return new ConfigCondition(Switch.MATERIAL_ORE, materialId, "", "", oreId);
    }

    /** Checks that the switch has the fields it needs and no others. */
    private DataResult<ConfigCondition> validate() {
        boolean needsMaterial = switchId == Switch.MATERIAL || switchId == Switch.MATERIAL_FORM || switchId == Switch.MATERIAL_RECIPE
                || switchId == Switch.MATERIAL_ORE;
        if (needsMaterial != !material.isEmpty()) {
            return DataResult.error(() -> "\"material\" is needed by the material, material_form, material_recipe and material_ore switches only: " + switchId);
        }
        if ((switchId == Switch.MATERIAL_FORM) != !form.isEmpty()) {
            return DataResult.error(() -> "\"form\" is needed by the switch material_form only: " + switchId);
        }
        if ((switchId == Switch.MATERIAL_RECIPE) != !recipe.isEmpty()) {
            return DataResult.error(() -> "\"recipe\" is needed by the switch material_recipe only: " + switchId);
        }
        if ((switchId == Switch.MATERIAL_ORE) != !ore.isEmpty()) {
            return DataResult.error(() -> "\"ore\" is needed by the switch material_ore only: " + switchId);
        }
        return DataResult.success(this);
    }

    @Override
    public boolean test(IContext context) {
        return switch (switchId) {
            case MATERIALS -> CraftResourcesConfig.materialsEnabled();
            case WORLDGEN -> CraftResourcesConfig.worldgenEnabled();
            case MATERIAL -> CraftResourcesConfig.materialEnabled(material);
            case MATERIAL_FORM -> CraftResourcesConfig.formEnabled(material, form);
            case MATERIAL_RECIPE -> CraftResourcesConfig.recipeEnabled(material, recipe);
            case MATERIAL_ORE -> CraftResourcesConfig.oreEnabled(material, ore);
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
