package net.samtrion.dotcraft.energy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The startup config of the energy mod ({@code config/dotcraft/energy/startup.toml}): only the switch
 * {@code batteries.<name>.enabled} of each battery and {@code generators.<name>.enabled} of each generator block. A startup config is loaded as soon as it is registered, before the
 * registries are filled, so it can decide what is registered: a battery that is switched off is not registered at all and its
 * recipe is dropped. A restart is needed after a change, and a client and a server with different values may fail to connect (the
 * registries differ). The FE values are in the {@link EnergyCommonConfig}.
 */
public final class EnergyStartupConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final Map<String, ModConfigSpec.BooleanValue> SWITCHES = new LinkedHashMap<>();

    static {
        BUILDER.push("batteries");
        for (String name : EnergyCommonConfig.NAMES) {
            BUILDER.push(name);
            SWITCHES.put(name, BUILDER.comment("Enables the " + name + ". When disabled, the item is not registered at all and its recipe is dropped. "
                    + "Needs a restart; a client and a server with different values may fail to connect.")
                    .define("enabled", true));
            BUILDER.pop();
        }
        BUILDER.pop();
        BUILDER.push("generators");
        for (String name : java.util.stream.Stream.of(EnergyCommonConfig.SOLAR_NAMES, EnergyCommonConfig.LUNAR_NAMES, EnergyCommonConfig.VIBRATION_NAMES).flatMap(List::stream).toList()) {
            BUILDER.push(name);
            SWITCHES.put(name, BUILDER.comment("Enables the " + name + ". When disabled, the block is not registered at all and its recipe is dropped. "
                    + "Needs a restart; a client and a server with different values may fail to connect.")
                    .define("enabled", true));
            BUILDER.pop();
        }
        for (String name : EnergyCommonConfig.FLOW_NAMES) {
            BUILDER.push(name);
            SWITCHES.put(name, BUILDER.comment("Enables the " + name + ". When disabled, the block is not registered at all and its recipe is dropped. "
                    + "Needs a restart; a client and a server with different values may fail to connect.")
                    .define("enabled", true));
            BUILDER.pop();
        }
        BUILDER.pop();
    }

    /** The built spec, registered as STARTUP config (loaded before the registries are filled). */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private EnergyStartupConfig() {}

    /**
     * Whether a battery is registered.
     *
     * @param name the registry path of the battery, e.g. {@code potato_battery}
     * @return its switch; {@code true} while the startup config is not loaded (e.g. in datagen and unit tests)
     */
    public static boolean enabled(String name) {
        return !SPEC.isLoaded() || SWITCHES.get(name).get();
    }
}
