package net.samtrion.dotcraft.energy;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The common config of the energy mod ({@code config/dotcraft/energy/common.toml}): per battery the FE values
 * {@code batteries.<name>.capacity}, {@code maxInput} (not for the single-use battery, which can never be charged) and {@code maxOutput}. They are read whenever they are needed, so a change
 * counts for every battery already in the world; a stack that holds more than the new capacity is capped on its next transfer. The
 * generators (panels, mob wheels) have {@code generators.<name>.peak}, {@code capacity} and {@code maxOutput}. The
 * batteries and generators are switched on and off in the {@link EnergyStartupConfig}. Client and server read their own file: for a correct bar and
 * tooltip on a client of a dedicated server keep the values equal.
 */
public final class EnergyCommonConfig {
    /** The registry paths of the batteries, which are also their config sections. */
    static final List<String> NAMES = List.of("potato_battery", "rechargeable_potato_battery",
            "battery_tier_2", "battery_tier_3", "battery_tier_4", "battery_tier_5");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** The single-use potato battery. */
    public static final Battery POTATO_BATTERY = new Battery(NAMES.get(0), 4000, -1, 40);
    /** The rechargeable potato battery, tier 1. */
    public static final Battery RECHARGEABLE_POTATO_BATTERY = new Battery(NAMES.get(1), 20000, 100, 100);
    /** Tier 2: five times tier 1. */
    public static final Battery BATTERY_TIER_2 = new Battery(NAMES.get(2), 100_000, 500, 500);
    /** Tier 3: 7.5 times tier 1. */
    public static final Battery BATTERY_TIER_3 = new Battery(NAMES.get(3), 750_000, 3_750, 3_750);
    /** Tier 4: ten times tier 3. */
    public static final Battery BATTERY_TIER_4 = new Battery(NAMES.get(4), 7_500_000, 37_500, 37_500);
    /** Tier 5: ten times tier 4. */
    public static final Battery BATTERY_TIER_5 = new Battery(NAMES.get(5), 75_000_000, 375_000, 375_000);

    /** The registry paths of the solar panels, tiers 1-5, which are also their config sections. */
    static final List<String> SOLAR_NAMES = tierNames("solar_panel");
    /** The registry paths of the lunar panels, tiers 1-5, which are also their config sections. */
    static final List<String> LUNAR_NAMES = tierNames("lunar_panel");

    /** The solar panels: the peak FE/t at noon under a clear sky, at least 15 FE/t; a panel fills the battery of its tier in well under one MC day. */
    public static final List<Generator> SOLAR_PANELS = generators(SOLAR_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The lunar panels: the peak FE/t at full moon in a clear night. */
    public static final List<Generator> LUNAR_PANELS = generators(LUNAR_NAMES, new int[] {15, 75, 560, 5_600, 56_000});

    /** The registry paths of the mob wheels, tiers 1-5, which are also their config sections. */
    static final List<String> WHEEL_NAMES = tierNames("mob_wheel");
    /** The mob wheels: the peak FE/t with a full cage of running mobs, anchored by a duty cycle of a quarter (see {@code WheelMath}). */
    public static final List<Generator> MOB_WHEELS = generators(WHEEL_NAMES, new int[] {15, 75, 560, 5_600, 56_000});

    /** The built spec, registered as COMMON config. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private EnergyCommonConfig() {}

    private static List<String> tierNames(String prefix) {
        return List.of(prefix + "_tier_1", prefix + "_tier_2", prefix + "_tier_3", prefix + "_tier_4", prefix + "_tier_5");
    }

    private static List<Generator> generators(List<String> names, int[] peaks) {
        var list = new java.util.ArrayList<Generator>();
        for (int i = 0; i < names.size(); i++) list.add(new Generator(names.get(i), peaks[i]));
        return List.copyOf(list);
    }

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

    /** The FE values of one generator; while the config is not loaded (e.g. in datagen and unit tests) the defaults count. */
    public static final class Generator {
        /** Ticks of full output the buffer holds. */
        private static final int BUFFER_TICKS = 600;
        /** Multiple of the peak that can be given per operation. */
        private static final int OUTPUT_FACTOR = 5;

        private final ModConfigSpec.IntValue peak;
        private final ModConfigSpec.IntValue capacity;
        private final ModConfigSpec.IntValue maxOutput;
        private final int defaultPeak;

        /**
         * Defines the entries of a generator.
         *
         * @param name        the registry path of the generator, also its config section
         * @param defaultPeak the default peak output in FE/t
         */
        private Generator(String name, int defaultPeak) {
            this.defaultPeak = defaultPeak;
            BUILDER.push("generators").push(name);
            peak = BUILDER.comment("Peak FE per tick of the " + name + " under ideal conditions.")
                    .defineInRange("peak", defaultPeak, 0, Integer.MAX_VALUE / BUFFER_TICKS);
            capacity = BUILDER.comment("How much FE the " + name + " buffers.")
                    .defineInRange("capacity", defaultPeak * BUFFER_TICKS, 1, Integer.MAX_VALUE);
            maxOutput = BUILDER.comment("Most FE the " + name + " gives to a neighbour per tick.")
                    .defineInRange("maxOutput", defaultPeak * OUTPUT_FACTOR, 0, Integer.MAX_VALUE);
            BUILDER.pop(2);
        }

        /**
         * The peak output.
         *
         * @return the most FE per tick the generator makes
         */
        public int peak() {
            return SPEC.isLoaded() ? peak.get() : defaultPeak;
        }

        /**
         * The buffer size.
         *
         * @return the FE the generator buffers
         */
        public int capacity() {
            return SPEC.isLoaded() ? capacity.get() : defaultPeak * BUFFER_TICKS;
        }

        /**
         * The push rate.
         *
         * @return the most FE per tick given to a neighbour
         */
        public int maxOutput() {
            return SPEC.isLoaded() ? maxOutput.get() : defaultPeak * OUTPUT_FACTOR;
        }
    }
}
