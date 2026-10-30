package net.samtrion.dotcraft.energy.melt;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The water tank of a melt generator: one tank of {@link MeltMath#TANK_CAPACITY_MB} that only holds water. Nothing can be inserted from
 * outside (pipes and buckets can only drain it), the block entity fills it with the water of the melted ice through {@link #fill}.
 */
final class MeltTank extends FluidStacksResourceHandler {
    private final Runnable changed;

    /**
     * Creates an empty tank.
     *
     * @param changed called when the content changes
     */
    MeltTank(Runnable changed) {
        super(1, MeltMath.TANK_CAPACITY_MB);
        this.changed = changed;
    }

    /**
     * The water in the tank.
     *
     * @return the amount in mB
     */
    int amount() {
        return getAmountAsInt(0);
    }

    /**
     * Whether the tank cannot take any more water.
     *
     * @return {@code true} if it is full
     */
    boolean isFull() {
        return amount() >= MeltMath.TANK_CAPACITY_MB;
    }

    /**
     * Puts water into the tank; what does not fit is lost.
     *
     * @param mb the water in mB
     * @return the mB that fitted
     */
    int fill(int mb) {
        int fit = MeltMath.tankFit(amount(), mb);
        if (fit > 0) set(0, FluidResource.of(Fluids.WATER), amount() + fit);
        return fit;
    }

    /** Outside nothing can be inserted. */
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return 0;
    }

    @Override
    protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack previousContents) {
        changed.run();
    }
}
