package net.samtrion.dotcraft.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * The startup config of the resources mod ({@code config/dotcraft/resources/startup.toml}): only the switches of the tool
 * forms (hammers, sandpapers, every form with a {@link Form#tool() tool}) at the path
 * {@code materials.<material>.<configKey>.enabled}. A startup config is loaded as soon as it is registered, before the
 * registries are filled, so it can decide what is registered: the item of a tool form that is switched off there is not
 * registered at all. A restart is needed after a change, and a client and a server with different values may fail to
 * connect (the registries differ). Read it through {@link CommonConfig}.
 */
public final class StartupConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** The switches of the tool forms by {@code <material>.<form id>}. */
    private static final Map<String, ModConfigSpec.BooleanValue> TOOL_SWITCHES = new LinkedHashMap<>();

    static {
        BUILDER.push("materials");
        for (Material material : Materials.all()) {
            for (Form form : material.forms()) {
                if (form.tool().isEmpty()) continue;
                BUILDER.push(material.id()).push(form.configKey());
                TOOL_SWITCHES.put(material.id() + "." + form.id(), BUILDER
                        .comment("Enables the form " + form.id() + " of " + material.id() + ". When disabled, the item is not registered at all and its recipes are dropped. "
                                + "Needs a restart; a client and a server with different values may fail to connect.")
                        .define("enabled", true));
                BUILDER.pop(2);
            }
        }
        BUILDER.pop();
    }

    /** The built spec, registered as STARTUP config (loaded before the registries are filled). */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private StartupConfig() {}

    /**
     * The switch of a tool form.
     *
     * @param key {@code <material>.<form id>}
     * @return its config value, {@code null} if the form of the material is no tool form
     */
    static ModConfigSpec.BooleanValue tool(String key) {
        return TOOL_SWITCHES.get(key);
    }

    /**
     * The value of a startup switch.
     *
     * @param value a switch of this config
     * @return its value; {@code true} while the startup config is not loaded (e.g. in datagen)
     */
    static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() || value.get();
    }

    /**
     * Whether the item of a form is registered: for a tool form its startup switch
     * ({@code materials.<id>.<configKey>.enabled} of the startup config) alone, so the common config cannot matter; always
     * {@code true} for any other form and while the startup config is not loaded (e.g. in datagen).
     *
     * @param materialId the id of the material, e.g. {@code iron}
     * @param formId     the id of the form, e.g. {@code hammer}
     * @return {@code false} only for a tool form that is switched off at startup
     */
    public static boolean registered(String materialId, String formId) {
        ModConfigSpec.BooleanValue tool = TOOL_SWITCHES.get(materialId + "." + formId);
        return tool == null || on(tool);
    }
}
