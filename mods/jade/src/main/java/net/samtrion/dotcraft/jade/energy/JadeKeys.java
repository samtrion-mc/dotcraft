package net.samtrion.dotcraft.jade.energy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The translation keys of the Jade lines of the energy mod's generators, free of any Minecraft type so unit tests can
 * check the language files against them. Every public {@code String} constant but the prefixes is a key; the families, the
 * kite statuses and the kite limits are listed for the keys that depend on them.
 */
public final class JadeKeys {
    /** Prefix of every line key. */
    public static final String PREFIX = "jade.dotcraftjade.";
    /** The mod id the Jade providers of the energy mod are registered under (also the namespace of their config keys). */
    public static final String ENERGY = "dotcraftenergy";

    /** Output now and at most, FE/t; arguments: now, max. */
    public static final String OUTPUT = PREFIX + "output";
    /** Peak output, FE/t; argument: peak. */
    public static final String PEAK = PREFIX + "peak";
    /** Stored and capacity, FE; arguments: stored, capacity. */
    public static final String STORED = PREFIX + "stored";

    /** Height of the sun or moon after tracking; argument: share. */
    public static final String PANEL_SUN = PREFIX + "panel.sun";
    /** See {@link #PANEL_SUN}. */
    public static final String PANEL_MOON = PREFIX + "panel.moon";
    /** Clear weather over a panel. */
    public static final String PANEL_WEATHER_CLEAR = PREFIX + "panel.weather.clear";
    /** Rain over a panel; argument: share of the output that stays. */
    public static final String PANEL_WEATHER_RAIN = PREFIX + "panel.weather.rain";
    /** A thunderstorm over a panel; argument: share of the output that stays. */
    public static final String PANEL_WEATHER_THUNDER = PREFIX + "panel.weather.thunder";
    /** The panel does not see the sky. */
    public static final String PANEL_NO_SKY = PREFIX + "panel.no_sky";
    /** Group size and pool; arguments: panels, stored, capacity. */
    public static final String PANEL_GROUP = PREFIX + "panel.group";
    /** An entity stands on the panel. */
    public static final String PANEL_OCCUPIED = PREFIX + "panel.occupied";
    /** The panel is waterlogged; argument: share of the output that stays. */
    public static final String PANEL_WATERLOGGED = PREFIX + "panel.waterlogged";

    /** Water in the scan cube; arguments: share, sources, flowing. */
    public static final String FLOW_WATER = PREFIX + "flow.water";
    /** Biome multiplier; argument: share. */
    public static final String FLOW_BIOME = PREFIX + "flow.biome";
    /** Fauna bonus; arguments: kinds, bonus. */
    public static final String FLOW_FAUNA = PREFIX + "flow.fauna";
    /** The pod is not waterlogged. */
    public static final String FLOW_DRY = PREFIX + "flow.dry";
    /** The flow axis is blocked. */
    public static final String FLOW_BLOCKED = PREFIX + "flow.blocked";

    /** Fill of the bucket; arguments: tokens, burst. */
    public static final String VIBRATION_BUCKET = PREFIX + "vibration.bucket";
    /** Peak and average ceiling; arguments: peak, average. */
    public static final String VIBRATION_PEAK = PREFIX + "vibration.peak";
    /** Radius and quietest frequency; arguments: radius, frequency. */
    public static final String VIBRATION_HEARING = PREFIX + "vibration.hearing";

    /** Heat, cold and difference; arguments: heat, cold, difference, full-peak difference, share. */
    public static final String THERMO_LEVELS = PREFIX + "thermo.levels";
    /** No heat and cold on opposite faces. */
    public static final String THERMO_NONE = PREFIX + "thermo.none";

    /** The heat reservoir; arguments: held, capacity. */
    public static final String MELT_RESERVOIR = PREFIX + "melt.reservoir";
    /** The water tank; arguments: mB, capacity mB. */
    public static final String MELT_TANK = PREFIX + "melt.tank";
    /** Full efficiency; argument: share. */
    public static final String MELT_EFFICIENCY = PREFIX + "melt.efficiency";
    /** Reduced efficiency because the tank is full; argument: share. */
    public static final String MELT_EFFICIENCY_FULL = PREFIX + "melt.efficiency_full";

    /** The wheel turns. */
    public static final String WHEEL_RUNNING = PREFIX + "wheel.running";
    /** Nothing drives the wheel. */
    public static final String WHEEL_IDLE = PREFIX + "wheel.idle";

    /** Wire stored; arguments: wire, capacity. */
    public static final String KITE_WIRE = PREFIX + "kite.wire";
    /** Effective length and length factor; arguments: blocks, factor. */
    public static final String KITE_LENGTH = PREFIX + "kite.length";
    /** Kites in the air; argument: count. */
    public static final String KITE_KITES = PREFIX + "kite.kites";
    /** The kite generator is too close to another; arguments: distance, minimum. */
    public static final String KITE_TOO_CLOSE = PREFIX + "kite.too_close";
    /** As {@link #KITE_TOO_CLOSE} when the distance is not known; argument: minimum. */
    public static final String KITE_TOO_CLOSE_UNKNOWN = PREFIX + "kite.too_close_unknown";
    /** What limits a flying kite; argument: the reason, one of {@link #KITE_LIMITS}. */
    public static final String KITE_LIMIT = PREFIX + "kite.limit";

    /** The families that have a provider and so a config entry. */
    public static final List<String> FAMILIES = List.of("panel", "flow", "vibration", "thermo", "melt", "wheel", "kite");
    /** The statuses of a kite generator, lower case (see {@code KiteBlockEntity.Status}); every one but too close has a line. */
    public static final List<String> KITE_STATUSES = List.of("no_kite", "no_wire", "no_sky", "weather_reeled", "flying");
    /** The reasons that limit a flying kite (see {@code Diagnostics.limit}), as translation key suffixes. */
    public static final List<String> KITE_LIMITS = List.of("blocked", "crowded", "no_sky", "storm", "weather");

    /** Not instantiable. */
    private JadeKeys() {}

    /**
     * The key of the line of a kite status.
     *
     * @param status the status name in lower case
     * @return the key
     */
    public static String kiteStatus(String status) {
        return PREFIX + "kite.status." + status;
    }

    /**
     * The key of a reason that limits a flying kite.
     *
     * @param limit the reason
     * @return the key
     */
    public static String kiteLimit(String limit) {
        return KITE_LIMIT + "." + limit;
    }

    /**
     * The key of the config entry that switches the provider of a family on or off.
     *
     * @param family one of {@link #FAMILIES}
     * @return the key
     */
    public static String config(String family) {
        return "config.jade.plugin_" + ENERGY + "." + family;
    }

    /**
     * Every key the energy providers use, including the config entries.
     *
     * @return all keys
     */
    public static List<String> all() {
        var keys = new ArrayList<String>();
        for (var field : JadeKeys.class.getFields()) {
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers()) || field.getName().equals("PREFIX") || field.getName().equals("ENERGY")) continue;
            try {
                keys.add((String) field.get(null));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        KITE_STATUSES.forEach(s -> keys.add(kiteStatus(s)));
        KITE_LIMITS.forEach(l -> keys.add(kiteLimit(l)));
        FAMILIES.forEach(f -> keys.add(config(f)));
        return keys;
    }
}
