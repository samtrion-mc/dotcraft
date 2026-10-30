package net.samtrion.dotcraft.energy.thermo;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** The FE buffer of a thermoelectric generator: nothing can be inserted from outside, the block entity fills it; extraction is limited by the config. */
final class ThermoEnergy extends SimpleEnergyHandler {
    /** Called when the stored FE changes. */
    private final Runnable changed;

    /**
     * Creates an empty buffer.
     *
     * @param capacity  the capacity
     * @param maxOutput the most FE extracted per operation
     * @param changed   called when the stored FE changes
     */
    ThermoEnergy(int capacity, int maxOutput, Runnable changed) {
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

    @Override
    protected void onEnergyChanged(int previousAmount) {
        changed.run();
    }
}
