package net.samtrion.dotcraft.energy.generator;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.VoidingEnergyHandler;

/** What the {@link PowerDrainBlock} does with a neighbour; free of Minecraft types so a unit test can run it. */
final class PowerDrain {
    /** Not instantiable. */
    private PowerDrain() {}

    /**
     * Drains a neighbour once: asks for everything, so the neighbour's own config decides how much it gives per operation.
     *
     * @param neighbour the neighbour's handler on the side towards the drain, may be {@code null}
     * @return the FE destroyed
     */
    static int pull(@Nullable EnergyHandler neighbour) {
        return EnergyHandlerUtil.move(neighbour, VoidingEnergyHandler.INSTANCE, Integer.MAX_VALUE, null);
    }
}
