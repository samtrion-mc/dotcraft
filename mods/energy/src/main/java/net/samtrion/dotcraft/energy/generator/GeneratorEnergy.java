package net.samtrion.dotcraft.energy.generator;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The FE share of one generator block: nothing can be inserted from outside, the block entity fills it; extraction is limited by the
 * config, which is applied by {@link #configure}.
 */
public final class GeneratorEnergy extends SimpleEnergyHandler {
    private final Runnable changed;

    /**
     * Creates an empty buffer.
     *
     * @param capacity  the capacity
     * @param maxOutput the most FE extracted per operation
     * @param changed   called when the stored FE changes
     */
    public GeneratorEnergy(int capacity, int maxOutput, Runnable changed) {
        super(capacity, 0, maxOutput);
        this.changed = changed;
    }

    /**
     * Applies the config values; the stored FE is capped to a smaller capacity.
     *
     * @param newCapacity  the capacity
     * @param newMaxOutput the most FE extracted per operation
     */
    public void configure(int newCapacity, int newMaxOutput) {
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
    public int produce(int amount) {
        if (amount <= 0 || energy >= capacity) return 0;
        int before = energy;
        set((int) Math.min(capacity, (long) energy + amount));
        return energy - before;
    }

    /**
     * Pushes FE of a source into the neighbours of a block, one side after the other, until the source is empty. The capability
     * caches are created on the first push and kept in the given map.
     *
     * @param caches the capability caches of the block, by side
     * @param level  the level
     * @param pos    the position of the block
     * @param source the FE to push
     * @param skip   a side that gets nothing, or {@code null} for all six
     */
    public static void push(Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> caches, ServerLevel level, BlockPos pos,
            EnergyHandler source, @Nullable Direction skip) {
        if (source.getAmountAsLong() <= 0) return;
        for (Direction d : Direction.values()) {
            if (d == skip) continue;
            var cache = caches.computeIfAbsent(d, dir -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos.relative(dir), dir.getOpposite()));
            EnergyHandlerUtil.move(source, cache.getCapability(), Integer.MAX_VALUE, null);
            if (source.getAmountAsLong() <= 0) return;
        }
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
