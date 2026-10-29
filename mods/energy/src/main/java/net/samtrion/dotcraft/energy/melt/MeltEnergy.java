package net.samtrion.dotcraft.energy.melt;

import java.util.Collection;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The buffer of a melt generator: nothing can be inserted from outside, the block entity fills it, and extraction is limited by the config. */
final class MeltEnergy extends SimpleEnergyHandler {
    private final Runnable changed;

    /**
     * Creates an empty buffer.
     *
     * @param capacity  the capacity
     * @param maxOutput the most FE extracted per operation
     * @param changed   called when the stored FE changes
     */
    MeltEnergy(int capacity, int maxOutput, Runnable changed) {
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
     * The free space.
     *
     * @return the FE that still fit
     */
    int room() {
        return Math.max(0, capacity - energy);
    }

    /**
     * Adds produced FE up to the capacity.
     *
     * @param amount the FE made
     * @return the FE that fitted
     */
    int produce(int amount) {
        int fit = Math.min(Math.max(amount, 0), room());
        if (fit > 0) set(energy + fit);
        return fit;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        changed.run();
    }

    /**
     * Hands FE to the given handlers one after the other, for a generator that is removed: what they do not take is lost.
     *
     * @param total   the FE to hand on
     * @param targets the handlers that may take FE
     * @return the FE that was not taken
     */
    static int handOff(int total, Collection<? extends EnergyHandler> targets) {
        int left = total;
        for (var target : targets) {
            if (left <= 0) break;
            try (var tx = Transaction.openRoot()) {
                left -= target.insert(left, tx);
                tx.commit();
            }
        }
        return left;
    }
}
