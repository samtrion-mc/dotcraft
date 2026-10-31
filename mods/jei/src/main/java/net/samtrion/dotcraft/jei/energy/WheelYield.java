package net.samtrion.dotcraft.jei.energy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The pure part of the mob wheel yield page: turns the attributes of the mobs into the entries of the page. Free of any Minecraft,
 * JEI or energy mod type, so a unit test can check it; the weight and share formulas of {@code WheelMath} are passed in by
 * {@code WheelYieldCategory}, so the page never copies them.
 */
public final class WheelYield {
    /** The number of wheel tiers. */
    public static final int TIERS = GeneratorInfo.MAX_TIER;

    /** Translation key of the category title. */
    public static final String TITLE_KEY = "jei.dotcraftjei.wheel_yield";
    /** Translation key of the weight line, one argument: the weight at tier 1 and 2. */
    public static final String WEIGHT_KEY = TITLE_KEY + ".weight";
    /** Translation key of the weight line for the tiers whose weight differs, one argument: the weight at tier 3 and up. */
    public static final String WEIGHT_LIGHT_KEY = TITLE_KEY + ".weight_light";
    /** Translation key of the running line, one argument: the five outputs. */
    public static final String RUNNING_KEY = TITLE_KEY + ".running";
    /** Translation key of the standing line, one argument: the five outputs. */
    public static final String STANDING_KEY = TITLE_KEY + ".standing";
    /** All translation keys of the page and the number of format arguments of each. */
    public static final java.util.Map<String, Integer> KEYS = java.util.Map.of(TITLE_KEY, 0, WEIGHT_KEY, 1, WEIGHT_LIGHT_KEY, 1, RUNNING_KEY, 1,
            STANDING_KEY, 1);

    /** Not instantiable. */
    private WheelYield() {}

    /** The weight of a mob for a tier, as {@code WheelMath.weight}. */
    @FunctionalInterface
    public interface Weigher {
        /**
         * Computes the weight.
         *
         * @param health the maximum health
         * @param speed  the base movement speed
         * @param tier   the tier, 1..5
         * @return the weight
         */
        double weight(double health, double speed, int tier);
    }

    /** The share of the peak a weight makes, as {@code WheelMath.share}. */
    @FunctionalInterface
    public interface Sharer {
        /**
         * Computes the share.
         *
         * @param weight  the weight of the mob
         * @param running whether it runs (otherwise it stands)
         * @return the share of the peak, 0..1
         */
        double share(double weight, boolean running);
    }

    /**
     * The attributes of one mob type.
     *
     * @param id     the entity type id, e.g. {@code minecraft:pig}; the sort tiebreaker
     * @param health the default maximum health
     * @param speed  the default movement speed
     */
    public record MobStats(String id, double health, double speed) {}

    /**
     * One row of the page.
     *
     * @param id       the entity type id
     * @param weights  the weight at tier 1..5 (index 0 is tier 1; tier 3 and up count light mobs as the reference mob)
     * @param running  the FE/t at tier 1..5 of a running mob
     * @param standing the FE/t at tier 1..5 of a standing mob
     */
    public record Entry(String id, List<Double> weights, List<Double> running, List<Double> standing) {}

    /**
     * Builds the entries, heaviest mob first (by the weight of tier 1, ties by id, so the order is stable).
     *
     * @param mobs    the mobs to show
     * @param peaks   the peak FE/t of tier 1..5
     * @param weigher the weight formula
     * @param sharer  the share formula
     * @return the entries
     */
    public static List<Entry> entries(List<MobStats> mobs, List<? extends Number> peaks, Weigher weigher, Sharer sharer) {
        var list = new ArrayList<Entry>();
        for (var mob : mobs) {
            var weights = new ArrayList<Double>();
            var running = new ArrayList<Double>();
            var standing = new ArrayList<Double>();
            for (int tier = 1; tier <= TIERS; tier++) {
                double weight = weigher.weight(mob.health(), mob.speed(), tier);
                double peak = peaks.get(tier - 1).doubleValue();
                weights.add(weight);
                running.add(peak * sharer.share(weight, true));
                standing.add(peak * sharer.share(weight, false));
            }
            list.add(new Entry(mob.id(), List.copyOf(weights), List.copyOf(running), List.copyOf(standing)));
        }
        list.sort(Comparator.<Entry>comparingDouble(e -> -e.weights().get(0)).thenComparing(Entry::id));
        return List.copyOf(list);
    }

    /**
     * Formats an FE/t value: whole numbers with a thousands separator from 100 on, one decimal below.
     *
     * @param value the value
     * @return the text
     */
    public static String fe(double value) {
        return value >= 100 ? String.format(Locale.ROOT, "%,d", Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Joins the values of the five tiers with {@code " / "}.
     *
     * @param values the values of tier 1..5
     * @return the text
     */
    public static String perTier(List<Double> values) {
        return String.join(" / ", values.stream().map(WheelYield::fe).toList());
    }
}
