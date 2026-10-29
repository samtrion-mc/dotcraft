package net.samtrion.dotcraft.energy.generator;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The FE share of one generator block: nothing can be inserted from outside, the block entity fills it; extraction is limited by the
 * config, which is applied by {@link #configure}.
 */
final class GeneratorEnergy extends SimpleEnergyHandler {
    private final Runnable changed;

    /**
     * Creates an empty buffer.
     *
     * @param capacity  the capacity
     * @param maxOutput the most FE extracted per operation
     * @param changed   called when the stored FE changes
     */
    GeneratorEnergy(int capacity, int maxOutput, Runnable changed) {
        super(capacity, 0, maxOutput);
        this.changed = changed;
    }

    /**
     * Applies the config values; the stored FE is capped to a smaller capacity.
     *
     * @param newCapacity  the capacity
     * @param newMaxOutput the most FE extracted per operation
     */
    void configure(int newCapacity, int newMaxOutput) {
        capacity = newCapacity;
        maxExtract = newMaxOutput;
        if (energy > capacity) set(capacity);
    }

    /**
     * Adds produced FE up to the capacity.
     *
     * @param amount the FE made
     * @return the FE that fitted
     */
    int produce(int amount) {
        if (amount <= 0 || energy >= capacity) return 0;
        int before = energy;
        set((int) Math.min(capacity, (long) energy + amount));
        return energy - before;
    }

    /**
     * The most FE a single extraction gives, as configured.
     *
     * @return the limit per operation
     */
    int maxOutput() {
        return maxExtract;
    }

    /**
     * Extracts FE of this share for the group's pool. The limit that counts is the one of the block that is drained, which the
     * caller applies, so the per-operation limit of this share does not.
     *
     * @param amount the wanted FE
     * @param tx     the transaction
     * @return the FE taken
     */
    int extractUnlimited(int amount, TransactionContext tx) {
        int saved = maxExtract;
        maxExtract = Integer.MAX_VALUE;
        try {
            return extract(amount, tx);
        } finally {
            maxExtract = saved;
        }
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        changed.run();
    }
}
