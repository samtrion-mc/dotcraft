package net.samtrion.dotcraft.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * The config of the resources mod, in two files: the common config ({@code config/dotcraft/resources/common.toml}, this class)
 * holds all switches but those of the tool forms; recipes are checked when the data packs load, so a changed value
 * applies after a restart or {@code /reload}. The {@link StartupConfig} ({@code config/dotcraft/resources/startup.toml})
 * holds only the switches of the tool forms (hammers, sandpapers, every form with a
 * {@link Form#tool() tool}) at the same path {@code materials.<material>.<configKey>.enabled}. A common config is loaded
 * after the registries are filled, a startup config as soon as it is registered, so only the startup config can decide
 * what is registered: an item of a tool form that is switched off there is not registered at all. A restart is
 * needed after a change, and a client and a server with different values may fail to connect (the registries differ).
 * The switches {@code materials.enabled} and {@code materials.<id>.enabled} of the common config still drop the recipes
 * and hide the tool items, but cannot unregister them.
 *
 * <p>The material switches are built from the loaded {@link Materials}, so a new material file gets its switches
 * without code. Example of the {@code materials} table (all values default to {@code true}):
 * <pre>
 * [materials]
 *     enabled = true          # master switch, off: no material or form is active
 *     [materials.tin]
 *         enabled = true      # off: all forms of tin are disabled
 *         [materials.tin.ingots]
 *             enabled = true
 *         [materials.tin.gears]
 *             enabled = false # off: no gear of tin, its recipes (also alloy recipes that use it) are dropped
 *     [materials.steel.recipes.charcoal]
 *         enabled = true      # off: the recipes of steel that use charcoal (an alternative of an input) are dropped
 *     [materials.steel.recipes.coal]
 *         enabled = true
 *     [materials.tin.ores.surface]
 *         enabled = true      # off: the ore entry "surface" of tin does not generate (the ore form switch is materials.tin.ores.enabled)
 * </pre>
 * An alloy input that lists alternatives (see {@link Material.Alloy.Input}) gets one {@code recipes} switch per
 * alternative material.
 * The key of a form is its {@link Form#configKey()} ({@code raw}, {@code raw_blocks}, {@code ingots},
 * {@code nuggets}, {@code grains}, {@code blocks}, {@code dusts}, {@code dust_blocks}, {@code plates}, {@code gears},
 * {@code hammers}, {@code sandpapers},
 * {@code ores}); only the forms a material has are listed. A disabled form drops every generated
 * recipe that makes or uses it, is hidden in the creative tab and the recipe viewers, and (the ores) is not generated in
 * the world; the registered items and blocks, their loot tables and tags stay, so existing ones keep working. The recipes
 * that need a hammer (crush and forge) are dropped when no hammer form of the tier they need is enabled, see {@link #toolEnabled}.
 */
public final class CommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Master switch of the compression feature; when off, no compress recipe is craftable. */
    public static final ModConfigSpec.BooleanValue COMPRESSED_ENABLED = BUILDER
            .push("compressed")
            .comment("Enables compressing blocks. When disabled, all compress recipes are disabled and the compressed blocks are hidden in the recipe viewers; existing blocks can still be decompressed.")
            .define("enabled", true);

    /** Highest compression tier that can be crafted; recipes that output a higher tier are disabled. */
    public static final ModConfigSpec.IntValue MAX_COMPRESS_LEVEL = BUILDER
            .comment("Highest compression level that can be crafted (1-" + CompressedTier.MAX + "). Recipes that output a higher level are disabled.")
            .defineInRange("maxCompressLevel", CompressedTier.MAX, 1, CompressedTier.MAX);

    static {
        BUILDER.pop();
    }

    /** Master switch of the materials; when off, no material recipe is craftable and the items are hidden in the recipe viewers. */
    public static final ModConfigSpec.BooleanValue MATERIALS_ENABLED = BUILDER
            .push("materials")
            .comment("Enables the materials (ores, ingots, dusts, ...). When disabled, all material recipes are disabled and the items are hidden in the recipe viewers; existing items stay in the world. Overrides the switches of the single materials and forms.")
            .define("enabled", true);

    /** The switches of the single materials and forms by {@code <material>} and {@code <material>.<form id>}. */
    private static final Map<String, ModConfigSpec.BooleanValue> MATERIAL_SWITCHES = new LinkedHashMap<>();

    static {
        for (Material material : Materials.all()) {
            BUILDER.push(material.id());
            MATERIAL_SWITCHES.put(material.id(), BUILDER
                    .comment("Enables the material " + material.id() + ". When disabled, all its forms are disabled.")
                    .define("enabled", true));
            for (Form form : material.forms()) {
                if (form.tool().isPresent()) continue; // the startup spec has it
                BUILDER.push(form.configKey());
                MATERIAL_SWITCHES.put(material.id() + "." + form.id(), BUILDER
                        .comment("Enables the form " + form.id() + " of " + material.id() + ". When disabled, its recipes (also alloy recipes that use it) are disabled, it is hidden in the creative tab, and the ore does not generate.")
                        .define("enabled", true));
                if (form.hosted()) {
                    // materials.<id>.ores.<entry>.enabled next to the switch of the form; an entry id is never "enabled"
                    for (Material.OreSpec spec : material.oreSpecs()) {
                        BUILDER.push(spec.id());
                        MATERIAL_SWITCHES.put(oreKey(material.id(), spec.id()), BUILDER
                                .comment("Enables the ore generation entry " + spec.id() + " of " + material.id() + ". When disabled, this entry does not generate in the world; the ore blocks stay.")
                                .define("enabled", true));
                        BUILDER.pop();
                    }
                }
                BUILDER.pop();
            }
            Set<String> alternatives = material.alloy().map(Material.Alloy::alternatives).orElse(Set.of());
            if (!alternatives.isEmpty()) {
                BUILDER.push("recipes");
                for (String alternative : alternatives) {
                    BUILDER.push(alternative);
                    MATERIAL_SWITCHES.put(recipeKey(material.id(), alternative), BUILDER
                            .comment("Enables the recipes of " + material.id() + " that use " + alternative + " as one of the alternatives of an input.")
                            .define("enabled", true));
                    BUILDER.pop();
                }
                BUILDER.pop();
            }
            BUILDER.pop();
        }
        BUILDER.pop();
    }

    /** Master switch of the ore generation; when off, no biome modifier of the materials is active. */
    public static final ModConfigSpec.BooleanValue WORLDGEN_ENABLED = BUILDER
            .push("worldgen")
            .comment("Enables the generation of material ores in the world. Applies to new chunks after a restart or /reload.")
            .define("enabled", true);

    static {
        BUILDER.pop();
    }

    /** The built spec, registered as COMMON config. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private CommonConfig() {}

    /**
     * The switch stored under a key.
     *
     * @param key a material id, {@code <material>.<form id>}, an ore entry key or a recipe key
     * @return its config value, {@code null} if there is none
     */
    private static ModConfigSpec.BooleanValue value(String key) {
        return MATERIAL_SWITCHES.get(key);
    }

    /** Key of an ore entry switch in {@link #MATERIAL_SWITCHES}; the {@code #} cannot occur in a form id or a material id. */
    private static String oreKey(String materialId, String oreId) {
        return materialId + ".ore#" + oreId;
    }

    /** Key of a recipe switch in {@link #MATERIAL_SWITCHES}; the {@code #} cannot occur in a form id. */
    private static String recipeKey(String materialId, String alternativeId) {
        return materialId + "#" + alternativeId;
    }

    /**
     * The configured maximum level; the default while the config is not loaded (e.g. in datagen).
     *
     * @return the highest craftable level, {@code 0} when compressing is disabled, otherwise {@code 1..MAX}
     */
    public static int maxCompressLevel() {
        if (SPEC.isLoaded() && !COMPRESSED_ENABLED.get()) return 0;
        return SPEC.isLoaded() ? MAX_COMPRESS_LEVEL.get() : MAX_COMPRESS_LEVEL.getDefault();
    }

    /**
     * Whether the materials are enabled; on while the config is not loaded (e.g. in datagen).
     *
     * @return {@code true} if material recipes are active
     */
    public static boolean materialsEnabled() {
        return !SPEC.isLoaded() || MATERIALS_ENABLED.get();
    }

    /**
     * Whether a material is enabled: {@code materials.enabled} and {@code materials.<id>.enabled}; on while the
     * config is not loaded (e.g. in datagen). An unknown material is disabled.
     *
     * @param materialId the id of the material, e.g. {@code tin}
     * @return {@code true} if the forms of the material may be active
     */
    public static boolean materialEnabled(String materialId) {
        ModConfigSpec.BooleanValue value = value(materialId);
        if (value == null) return false;
        return !SPEC.isLoaded() || (MATERIALS_ENABLED.get() && value.get());
    }

    /**
     * Whether a form of a material is enabled: the material is enabled and {@code materials.<id>.<configKey>.enabled}
     * is on; on while the config is not loaded (e.g. in datagen). A form the material lacks is disabled.
     *
     * @param materialId the id of the material, e.g. {@code tin}
     * @param formId     the id of the form, e.g. {@code gear} (not the config key)
     * @return {@code true} if the items and blocks of the form may be active; for a tool form its startup switch counts too
     */
    public static boolean formEnabled(String materialId, String formId) {
        ModConfigSpec.BooleanValue tool = StartupConfig.tool(materialId + "." + formId);
        if (tool != null) return StartupConfig.on(tool) && materialEnabled(materialId);
        ModConfigSpec.BooleanValue value = value(materialId + "." + formId);
        if (value == null) return false;
        return !SPEC.isLoaded() || (materialEnabled(materialId) && value.get());
    }

    /**
     * Whether some tool form is enabled for a tier: at least one material of that tier or a better one has the form, and
     * the form of that material is enabled (see {@link #formEnabled}). Recipes that need a tool (the crush and forge recipes
     * need a hammer) are only loaded while this holds for the tier they need; on while the config is not loaded (e.g. in
     * datagen) as far as a material has the form. An unknown form is disabled.
     *
     * @param formId the id of the tool form, e.g. {@code hammer}
     * @param tier   the lowest tier that counts, {@link ToolTier#NONE} for every material
     * @return {@code true} if a tool of the form of that tier or better may exist
     */
    public static boolean toolEnabled(String formId, ToolTier tier) {
        return Materials.all().stream()
                .anyMatch(m -> m.has(formId) && m.tier().canMine(tier) && formEnabled(m.id(), formId));
    }

    /**
     * The own switch of a material, {@code materials.<id>.enabled}, without the master switch.
     *
     * @param materialId the id of a loaded material
     * @return its config value
     * @throws IllegalArgumentException if there is no such material
     */
    public static ModConfigSpec.BooleanValue materialSwitch(String materialId) {
        return switchOf(materialId);
    }

    /**
     * The own switch of a form of a material, {@code materials.<id>.<configKey>.enabled}.
     *
     * @param materialId the id of a loaded material
     * @param formId     the id of a form the material has
     * @return its config value
     * @throws IllegalArgumentException if the material lacks the form
     */
    public static ModConfigSpec.BooleanValue formSwitch(String materialId, String formId) {
        ModConfigSpec.BooleanValue tool = StartupConfig.tool(materialId + "." + formId);
        return tool != null ? tool : switchOf(materialId + "." + formId);
    }

    /**
     * Whether the recipes of an alloy that use an alternative input material are enabled: the material is enabled and
     * {@code materials.<alloy>.recipes.<alternative>.enabled} is on; on while the config is not loaded (e.g. in
     * datagen). An alternative the alloy does not list is disabled.
     *
     * @param materialId    the id of the alloy, e.g. {@code steel}
     * @param alternativeId the id of the alternative input material, e.g. {@code charcoal}
     * @return {@code true} if the recipes with that alternative may be active
     */
    public static boolean recipeEnabled(String materialId, String alternativeId) {
        ModConfigSpec.BooleanValue value = value(recipeKey(materialId, alternativeId));
        if (value == null) return false;
        return !SPEC.isLoaded() || (materialEnabled(materialId) && value.get());
    }

    /**
     * The own switch of an alternative recipe of an alloy, {@code materials.<alloy>.recipes.<alternative>.enabled}.
     *
     * @param materialId    the id of a loaded alloy
     * @param alternativeId the id of an alternative input material of the alloy
     * @return its config value
     * @throws IllegalArgumentException if the alloy has no such alternative
     */
    public static ModConfigSpec.BooleanValue recipeSwitch(String materialId, String alternativeId) {
        return switchOf(recipeKey(materialId, alternativeId));
    }

    /** Looks up a switch by its key in {@link CommonConfig}. */
    private static ModConfigSpec.BooleanValue switchOf(String key) {
        ModConfigSpec.BooleanValue value = value(key);
        if (value == null) throw new IllegalArgumentException("no config switch for " + key);
        return value;
    }

    /**
     * Whether an ore generation entry of a material is enabled: the material is enabled and
     * {@code materials.<material>.ores.<entry>.enabled} is on; on while the config is not loaded (e.g. in datagen).
     * The switch of the ore form is not part of it, the biome modifier has its own condition for that. An entry that
     * the material does not have is disabled.
     *
     * @param materialId the id of the material, e.g. {@code tin}
     * @param oreId      the id of the ore entry, e.g. {@code surface}
     * @return {@code true} if the entry may generate
     */
    public static boolean oreEnabled(String materialId, String oreId) {
        ModConfigSpec.BooleanValue value = value(oreKey(materialId, oreId));
        if (value == null) return false;
        return !SPEC.isLoaded() || (materialEnabled(materialId) && value.get());
    }

    /**
     * The own switch of an ore generation entry, {@code materials.<material>.ores.<entry>.enabled}.
     *
     * @param materialId the id of a loaded material
     * @param oreId      the id of an ore entry of the material
     * @return its config value
     * @throws IllegalArgumentException if the material has no such entry
     */
    public static ModConfigSpec.BooleanValue oreSwitch(String materialId, String oreId) {
        return switchOf(oreKey(materialId, oreId));
    }

    /**
     * Whether the ores of the materials generate; on while the config is not loaded (e.g. in datagen).
     *
     * @return {@code true} if the biome modifiers of the materials are active
     */
    public static boolean worldgenEnabled() {
        return !SPEC.isLoaded() || WORLDGEN_ENABLED.get();
    }
}
