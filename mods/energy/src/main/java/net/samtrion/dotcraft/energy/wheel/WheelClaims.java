package net.samtrion.dotcraft.energy.wheel;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lets a mob drive only one wheel at a time, so wheels with overlapping zones cannot count the same mob twice. A wheel that
 * counts a mob claims it; the claim is renewed with every scan and lapses when the wheel stops scanning (removed, unloaded), so
 * another wheel takes over after {@link #LAPSE_TICKS}. Server thread only. The mobs are held weakly.
 *
 * @param <E> the type of the mobs
 * @param <W> the type that identifies a wheel
 */
final class WheelClaims<E, W> {
    /** Ticks a claim holds without being renewed: a bit more than the {@link WheelMath#SCAN_TICKS scan interval}. */
    static final long LAPSE_TICKS = WheelMath.SCAN_TICKS + 5;

    private record Claim<W>(W wheel, long time) {}

    private final Map<E, Claim<W>> claims = new WeakHashMap<>();

    /**
     * Drops every claim of a wheel, as it was removed or unloaded: its mobs are free at once.
     *
     * @param wheel the wheel
     */
    void release(W wheel) {
        claims.values().removeIf(c -> c.wheel().equals(wheel));
    }

    /**
     * Claims a mob for a wheel.
     *
     * @param mob   the mob
     * @param wheel the wheel that wants it
     * @param now   the game time
     * @return {@code true} if the wheel may count the mob: it is free, claimed by this wheel or the claim has lapsed
     */
    boolean claim(E mob, W wheel, long now) {
        var held = claims.get(mob);
        if (held != null && !held.wheel().equals(wheel) && now - held.time() <= LAPSE_TICKS) return false;
        claims.put(mob, new Claim<>(wheel, now));
        return true;
    }
}
