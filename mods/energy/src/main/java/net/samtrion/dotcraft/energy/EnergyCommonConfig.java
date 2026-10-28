package net.samtrion.dotcraft.energy;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The common config of the energy mod ({@code config/dotcraft/energy/common.toml}): per battery the FE values
 * {@code batteries.<name>.capacity}, {@code maxInput} (not for the single-use battery, which can never be charged) and {@code maxOutput}. They are read whenever they are needed, so a change
 * counts for every battery already in the world; a stack that holds more than the new capacity is capped on its next transfer. The
 * batteries are switched on and off in the {@link EnergyStartupConfig}. Client and server read their own file: for a correct bar and
 * tooltip on a client of a dedicated server keep the values equal.
 */
public final class EnergyCommonConfig {
    /** The registry paths of the batteries, which are also their config sections. */
    static final List<String> NAMES = List.of("potato_battery", "rechargeable_potato_battery");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** The single-use potato battery. */
    public static final Battery POTATO_BATTERY = new Battery(NAMES.get(0), 4000, -1, 40);
    /** The rechargeable potato battery. */
    public static final Battery RECHARGEABLE_POTATO_BATTERY = new Battery(NAMES.get(1), 20000, 100, 100);

    /** The built spec, registered as COMMON config. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private EnergyCommonConfig() {}

    /** The FE values of one battery; while the config is not loaded (e.g. in datagen and unit tests) the defaults count. */
    public static final class Battery {
        private final ModConfigSpec.IntValue capacity;
        private final ModConfigSpec.@Nullable IntValue maxInput;
        private final ModConfigSpec.IntValue maxOutput;
        private final int defaultCapacity;
        private final int defaultMaxInput;
        private final int defaultMaxOutput;

        /**
         * Defines the entries of a battery.
         *
         * @param name               the registry path of the battery, also its config section
         * @param defaultCapacity the default capacity in FE
         * @param defaultMaxInput    the default most FE taken per operation, negative for a battery that can never be charged (it has no such entry)
         * @param defaultMaxOutput   the default most FE given per operation
         */
        private Battery(String name, int defaultCapacity, int defaultMaxInput, int defaultMaxOutput) {
            this.defaultCapacity = defaultCapacity;
            this.defaultMaxInput = defaultMaxInput;
            this.defaultMaxOutput = defaultMaxOutput;
            BUILDER.push("batteries").push(name);
            capacity = BUILDER.comment("How much FE the " + name + " stores.")
                    .defineInRange("capacity", defaultCapacity, 1, Integer.MAX_VALUE);
            maxInput = defaultMaxInput < 0 ? null : BUILDER.comment("Most FE the " + name + " takes per operation; 0 means it cannot be charged.")
                    .defineInRange("maxInput", defaultMaxInput, 0, Integer.MAX_VALUE);
            maxOutput = BUILDER.comment("Most FE the " + name + " gives per operation; 0 means it cannot be drained.")
                    .defineInRange("maxOutput", defaultMaxOutput, 0, Integer.MAX_VALUE);
            BUILDER.pop(2);
        }

        /**
         * The capacity.
         *
         * @return the FE the battery stores
         */
        public int capacity() {
            return SPEC.isLoaded() ? capacity.get() : defaultCapacity;
        }

        /**
         * The charge rate.
         *
         * @return the most FE the battery takes per operation; always 0 for the single-use battery
         */
        public int maxInput() {
            if (maxInput == null) return 0;
            return SPEC.isLoaded() ? maxInput.get() : defaultMaxInput;
        }

        /**
         * The discharge rate.
         *
         * @return the most FE the battery gives per operation
         */
        public int maxOutput() {
            return SPEC.isLoaded() ? maxOutput.get() : defaultMaxOutput;
        }
    }
}
