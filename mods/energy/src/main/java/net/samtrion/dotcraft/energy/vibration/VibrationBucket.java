package net.samtrion.dotcraft.energy.vibration;

/**
 * The hard cap of a vibration generator: a token bucket of FE. It refills a fixed amount per tick up to a capacity and every grant
 * takes from it, so over any span of {@code t} ticks no more than {@code capacity + perTick * t} can be granted, whatever the events
 * do. It starts full (the first call only starts the clock), so a new generator answers its first events, and it keeps
 * no record of who or what made a vibration. The rate and the capacity are arguments of every call, so a changed config counts at
 * once.
 */
public final class VibrationBucket {
    /** The FE in the bucket; a value above the capacity is cut to it by the next call. */
    private long tokens;
    /** The game time of the last refill, negative until the clock is started. */
    private long lastTick;

    /** Creates a full bucket whose clock starts with the first call. */
    public VibrationBucket() {
        this(Long.MAX_VALUE, -1);
    }

    /**
     * Restores a saved bucket.
     *
     * @param tokens   the FE in the bucket
     * @param lastTick the game time of the last refill, negative if the clock was never started
     */
    public VibrationBucket(long tokens, long lastTick) {
        this.tokens = Math.max(0, tokens);
        this.lastTick = lastTick;
    }

    /**
     * Refills the bucket for the ticks since the last call, then grants as much of the wanted FE as it holds. A clock that went
     * backwards (a changed world time) only restarts and refills nothing.
     *
     * @param now      the game time
     * @param wanted   the FE wanted
     * @param perTick  the refill per tick
     * @param capacity the size of the bucket
     * @return the FE granted, between 0 and the wanted FE
     */
    public long take(long now, long wanted, long perTick, long capacity) {
        if (lastTick < 0 || now < lastTick) {
            lastTick = now;
        } else if (now > lastTick) {
            long elapsed = now - lastTick;
            // a refill that cannot fit fills the bucket; this also keeps the product below from overflowing
            tokens = perTick > 0 && elapsed > (capacity - Math.min(tokens, capacity)) / perTick + 1 ? capacity : Math.min(capacity, tokens + elapsed * perTick);
            lastTick = now;
        }
        tokens = Math.min(tokens, capacity);
        long granted = Math.max(0, Math.min(wanted, tokens));
        tokens -= granted;
        return granted;
    }

    /**
     * The FE in the bucket as of the last call.
     *
     * @return the tokens
     */
    public long tokens() {
        return tokens;
    }

    /**
     * The game time of the last refill.
     *
     * @return the tick, negative if the clock was never started
     */
    public long lastTick() {
        return lastTick;
    }
}
