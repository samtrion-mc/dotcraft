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
            "battery_tier_1", "battery_tier_2", "battery_tier_3", "battery_tier_4", "battery_tier_5");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** The single-use potato battery. */
    public static final Battery POTATO_BATTERY = new Battery(NAMES.get(0), 4000, -1, 40);
    /** The rechargeable potato battery, tier 0. Its 20 000 FE are also the reference the generator peaks and the economy tests are anchored on. */
    public static final Battery RECHARGEABLE_POTATO_BATTERY = new Battery(NAMES.get(1), 20000, 100, 100);
    /** Tier 1 (copper): twice tier 0. */
    public static final Battery BATTERY_TIER_1 = new Battery(NAMES.get(2), 40_000, 200, 200);
    /** Tier 2: 2.5 times tier 1. */
    public static final Battery BATTERY_TIER_2 = new Battery(NAMES.get(3), 100_000, 500, 500);
    /** Tier 3: 7.5 times tier 2. */
    public static final Battery BATTERY_TIER_3 = new Battery(NAMES.get(4), 750_000, 3_750, 3_750);
    /** Tier 4: ten times tier 3. */
    public static final Battery BATTERY_TIER_4 = new Battery(NAMES.get(5), 7_500_000, 37_500, 37_500);
    /** Tier 5: ten times tier 4. */
    public static final Battery BATTERY_TIER_5 = new Battery(NAMES.get(6), 75_000_000, 375_000, 375_000);

    /** The registry paths of the solar panels, tiers 1-5, which are also their config sections. */
    static final List<String> SOLAR_NAMES = tierNames("solar_panel");
    /** The registry paths of the lunar panels, tiers 1-5, which are also their config sections. */
    static final List<String> LUNAR_NAMES = tierNames("lunar_panel");

    /** The solar panels: the peak FE/t at noon under a clear sky, at least 15 FE/t; a panel fills the battery of its tier in well under one MC day. */
    public static final List<Generator> SOLAR_PANELS = generators(SOLAR_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The lunar panels: the peak FE/t at full moon in a clear night. */
    public static final List<Generator> LUNAR_PANELS = generators(LUNAR_NAMES, new int[] {15, 75, 560, 5_600, 56_000});

    /** The registry paths of the flow generators, tiers 1-5, which are also their config sections. */
    static final List<String> FLOW_NAMES = tierNames("flow_generator");

    /** The flow generators: the peak FE/t in the best case without animals (full water share in an ocean or river), at least 15 FE/t; animals add up to 30 % on top, so the real maximum is 1.3 times the peak. */
    public static final List<Generator> FLOW_GENERATORS = generators(FLOW_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The registry paths of the vibration generators, tiers 1-5, which are also their config sections. */
    static final List<String> VIBRATION_NAMES = tierNames("vibration_generator");

    /**
     * The vibration generators: the hard ceiling in FE/t, which a token bucket enforces no matter how many game events there are (see
     * {@code VibrationBucket}); a generator next to a busy player makes 2/9 (tier 1) to 2/3 (tier 5) of it, see {@code VibrationMath}.
     */
    public static final List<Generator> VIBRATION_GENERATORS = generators(VIBRATION_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The registry paths of the thermoelectric generators, tiers 1-5, which are also their config sections. */
    public static final List<String> THERMO_NAMES = tierNames("thermo_generator");
    /**
     * The thermoelectric generators: the peak FE/t at the temperature difference of the tier's ceiling (see ThermoMath in the thermo package). They run day and
     * night, so even the weakest hot and cold source fills the battery of the tier in well under one MC day.
     */
    public static final List<Generator> THERMO_GENERATORS = generators(THERMO_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The registry paths of the melt generators, tiers 1-5, which are also their config sections. */
    static final List<String> MELT_NAMES = tierNames("melt_generator");
    /** The melt generators: the most FE per tick the heat of the melted ice is released at (a rate limit, see {@code MeltMath}). */
    public static final List<Generator> MELT_GENERATORS = generators(MELT_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The FE a melted block of ice, packed ice and blue ice gives, section {@code melt}. */
    public static final MeltYield MELT_YIELD = new MeltYield(1_000, 9_000, 81_000);
    /** The registry paths of the mob wheels, tiers 1-5, which are also their config sections. */
    static final List<String> WHEEL_NAMES = tierNames("wheel_generator");
    /** The mob wheels: the peak FE/t with a full cage of running mobs, anchored by a duty cycle of a quarter (see {@code WheelMath}). */
    public static final List<Generator> MOB_WHEELS = generators(WHEEL_NAMES, new int[] {15, 75, 560, 5_600, 56_000});
    /** The registry paths of the kite wind generators, tiers 1-5, which are also their config sections. */
    public static final List<String> KITE_NAMES = tierNames("kite_generator");
    /**
     * The kite wind generators. The configured {@code base} FE/t is only the base of the tier: the output is the base times the length
     * factor of the tether (tether / 64 at the most: 0.25 / 0.375 / 0.5 / 0.75 / 1 for tier 1-5), so the buffer and the push rate default to the
     * real best case, base x factor.
     */
    public static final List<Generator> KITE_GENERATORS = kiteGenerators(new int[] {64, 400, 2_600, 17_333, 128_000}, new double[] {0.25, 0.375, 0.5, 0.75, 1});
    /** The default FE one lightning bolt on a kite gives (named constant, see {@link #kiteLightningEnergy()}). */
    public static final int KITE_LIGHTNING_ENERGY_DEFAULT = 1_000_000;
    private static final ModConfigSpec.IntValue KITE_LIGHTNING_ENERGY = BUILDER
            .comment("FE a kite wind generator gets from one lightning bolt (only tiers that stay up in a thunderstorm are struck).")
            .defineInRange("kite_lightning_energy", KITE_LIGHTNING_ENERGY_DEFAULT, 0, Integer.MAX_VALUE);

    /** The bosses of the vibration generators: entity types whose game events are worth {@code VibrationMath.BOSS_FACTOR} times as much, key {@code vibration.bosses}. */
    public static final MobList VIBRATION_BOSSES = new MobList("vibration", "bosses",
            List.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian"),
            "Entity types (ids or #tags) whose vibrations are worth much more to a vibration generator; the event must be caused by the entity itself.");

    /** The built spec, registered as COMMON config. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Not instantiable. */
    private EnergyCommonConfig() {}

    private static List<String> tierNames(String prefix) {
        return List.of(prefix + "_tier_1", prefix + "_tier_2", prefix + "_tier_3", prefix + "_tier_4", prefix + "_tier_5");
    }

    private static List<Generator> kiteGenerators(int[] bases, double[] factors) {
        var list = new java.util.ArrayList<Generator>();
        for (int i = 0; i < KITE_NAMES.size(); i++) list.add(new Generator(KITE_NAMES.get(i), bases[i], factors[i], "base"));
        return List.copyOf(list);
    }

    /**
     * The FE one lightning bolt gives a kite generator.
     *
     * @return the FE per bolt, 1 000 000 by default
     */
    public static int kiteLightningEnergy() {
        return SPEC.isLoaded() ? KITE_LIGHTNING_ENERGY.get() : KITE_LIGHTNING_ENERGY_DEFAULT;
    }

    private static List<Generator> generators(List<String> names, int[] peaks) {
        var list = new java.util.ArrayList<Generator>();
        for (int i = 0; i < names.size(); i++) list.add(new Generator(names.get(i), peaks[i]));
        return List.copyOf(list);
    }

    /**
     * A config list of entity types (see {@link EntityFilter}); it is parsed when first asked for after the registries are ready and again whenever
     * the config value changes (a reload); the tags inside are read from the data packs at every match. While the config is not loaded the defaults count.
     */
    public static final class MobList {
        private final ModConfigSpec.ConfigValue<List<? extends String>> value;
        private final List<String> defaults;
        private volatile @Nullable Cached cached;

        /** A parsed state of the list. */
        private record Cached(List<? extends String> source, EntityFilter filter) {}

        private MobList(String section, String key, List<String> defaults, String comment) {
            this.defaults = defaults;
            BUILDER.push(section);
            value = BUILDER.comment(comment).defineListAllowEmpty(key, defaults, () -> "minecraft:pig", EntityFilter::isValid);
            BUILDER.pop();
        }

        /**
         * The parsed list.
         *
         * @return the filter of the current config value
         */
        public EntityFilter filter() {
            List<? extends String> now = SPEC.isLoaded() ? value.get() : defaults;
            var c = cached;
            if (c == null || !c.source().equals(now)) cached = c = new Cached(List.copyOf(now), EntityFilter.parse(now));
            return c.filter();
        }
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
        private final int defaultCapacity;
        private final int defaultMaxOutput;

        /**
         * Defines the entries of a generator.
         *
         * @param name        the registry path of the generator, also its config section
         * @param defaultPeak the default peak output in FE/t
         */
        private Generator(String name, int defaultPeak) {
            this(name, defaultPeak, 1, "peak");
        }

        /**
         * Defines the entries of a generator whose real best case is a multiple of its configured value.
         *
         * @param name        the registry path of the generator, also its config section
         * @param defaultPeak the default value in FE/t (the peak, or the base for a generator that multiplies it)
         * @param factor      the best-case multiple of that value; the buffer and the push rate default to it
         * @param key         the config key of the value, {@code peak} or {@code base}
         */
        private Generator(String name, int defaultPeak, double factor, String key) {
            this.defaultPeak = defaultPeak;
            defaultCapacity = (int) Math.round(defaultPeak * factor * BUFFER_TICKS);
            defaultMaxOutput = (int) Math.round(defaultPeak * factor * OUTPUT_FACTOR);
            BUILDER.push("generators").push(name);
            peak = BUILDER.comment((factor == 1 ? "Peak" : "Base") + " FE per tick of the " + name
                    + (factor == 1 ? " under ideal conditions." : "; the real output is this base times the length factor of the tether."))
                    .defineInRange(key, defaultPeak, 0, (int) (Integer.MAX_VALUE / (BUFFER_TICKS * factor)));
            capacity = BUILDER.comment("How much FE the " + name + " buffers.")
                    .defineInRange("capacity", defaultCapacity, 1, Integer.MAX_VALUE);
            maxOutput = BUILDER.comment("Most FE the " + name + " gives to a neighbour per tick.")
                    .defineInRange("maxOutput", defaultMaxOutput, 0, Integer.MAX_VALUE);
            BUILDER.pop(2);
        }

        /**
         * The peak output (for the kite generators the base, see {@link #KITE_GENERATORS}).
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
            return SPEC.isLoaded() ? capacity.get() : defaultCapacity;
        }

        /**
         * The push rate.
         *
         * @return the most FE per tick given to a neighbour
         */
        public int maxOutput() {
            return SPEC.isLoaded() ? maxOutput.get() : defaultMaxOutput;
        }
    }

    /** The FE a melted ice block gives, per type, global for all tiers and so in the top-level section {@code melt}; while the config is not loaded (e.g. in datagen and unit tests) the defaults count. */
    public static final class MeltYield {
        private final ModConfigSpec.IntValue ice;
        private final ModConfigSpec.IntValue packedIce;
        private final ModConfigSpec.IntValue blueIce;
        private final int defaultIce;
        private final int defaultPackedIce;
        private final int defaultBlueIce;

        /**
         * Defines the entries {@code melt.ice}, {@code melt.packed_ice} and {@code melt.blue_ice}.
         *
         * @param defaultIce       the default FE of an ice block
         * @param defaultPackedIce the default FE of a packed ice block (crafted from nine ice)
         * @param defaultBlueIce   the default FE of a blue ice block (crafted from nine packed ice)
         */
        private MeltYield(int defaultIce, int defaultPackedIce, int defaultBlueIce) {
            this.defaultIce = defaultIce;
            this.defaultPackedIce = defaultPackedIce;
            this.defaultBlueIce = defaultBlueIce;
            BUILDER.push("melt");
            ice = BUILDER.comment("FE a melted block of ice gives a melt generator.").defineInRange("ice", defaultIce, 0, 100_000_000);
            packedIce = BUILDER.comment("FE a melted block of packed ice gives a melt generator.").defineInRange("packed_ice", defaultPackedIce, 0, 100_000_000);
            blueIce = BUILDER.comment("FE a melted block of blue ice gives a melt generator.").defineInRange("blue_ice", defaultBlueIce, 0, 100_000_000);
            BUILDER.pop();
        }

        /**
         * The FE of an ice block.
         *
         * @return the FE
         */
        public int ice() {
            return SPEC.isLoaded() ? ice.get() : defaultIce;
        }

        /**
         * The FE of a packed ice block.
         *
         * @return the FE
         */
        public int packedIce() {
            return SPEC.isLoaded() ? packedIce.get() : defaultPackedIce;
        }

        /**
         * The FE of a blue ice block.
         *
         * @return the FE
         */
        public int blueIce() {
            return SPEC.isLoaded() ? blueIce.get() : defaultBlueIce;
        }
    }
}
