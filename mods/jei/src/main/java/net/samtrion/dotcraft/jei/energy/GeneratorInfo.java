package net.samtrion.dotcraft.jei.energy;

/**
 * The info pages of the energy mod's generators: which families there are, how many text lines each has and how many
 * format arguments every line takes. Free of any Minecraft or energy mod type, so unit tests can check the language
 * files against it; the texts are filled in by {@code EnergyJeiInfo}.
 */
public final class GeneratorInfo {
    /** Translation key prefix of the pages. */
    public static final String PREFIX = "info.dotcraftjei.";
    /** Translation key of the header of the tier perk line, one argument: the perk. */
    public static final String PERK_HEADER = PREFIX + "perk";
    /** The highest tier. */
    public static final int MAX_TIER = 5;

    /** A family of generator blocks with an info page. */
    public enum Family {
        /** Solar panels. */
        SOLAR("solar", true, 0, 2, 2, 1, 1, 1),
        /** Lunar panels. */
        LUNAR("lunar", true, 0, 2, 2, 1, 1, 1, 1),
        /** Flow generators. */
        FLOW("flow", true, 0, 0, 2, 3, 0, 5, 2),
        /** Vibration generators. */
        VIBRATION("vibration", true, 0, 0, 2, 2, 1, 1),
        /** Thermoelectric generators. */
        THERMO("thermo", true, 0, 0, 3, 2, 0),
        /** Melt generators. */
        MELT("melt", true, 0, 0, 4, 2),
        /** Mob wheels. */
        WHEEL("wheel", false, 0, 0, 1),
        /** Kite wind generators. */
        KITE("kite", true, 0, 1, 1, 2, 1, 1, 2),
        /** The creative power drain. */
        DRAIN("drain", false, 0);

        private final String id;
        private final boolean perks;
        private final int[] arguments;

        Family(String id, boolean perks, int... arguments) {
            this.id = id;
            this.perks = perks;
            this.arguments = arguments;
        }

        /** @return the name used in the translation keys */
        public String id() {
            return id;
        }

        /** @return how many text lines the page has, numbered from 1 */
        public int lines() {
            return arguments.length;
        }

        /**
         * The number of format arguments of a line.
         *
         * @param line the line, 1..{@link #lines()}
         * @return the number of arguments
         */
        public int arguments(int line) {
            return arguments[line - 1];
        }

        /** @return whether every tier of the family has a perk line */
        public boolean hasPerks() {
            return perks;
        }

        /**
         * The translation key of a line.
         *
         * @param line the line, 1..{@link #lines()}
         * @return the key
         */
        public String lineKey(int line) {
            return PREFIX + id + "." + line;
        }

        /**
         * The translation key of the perk of a tier.
         *
         * @param tier the tier, 1..{@link #MAX_TIER}
         * @return the key
         */
        public String perkKey(int tier) {
            return PREFIX + id + ".perk." + tier;
        }
    }

    /** Not instantiable. */
    private GeneratorInfo() {}
}
