package net.samtrion.dotcraft.energy.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** Unit tests for the draining of a neighbour by the power drain. */
class PowerDrainTest {
    /** The drain asks for everything but only gets what the neighbour's per-operation limit allows. */
    @Test
    void neighbourLimitCounts() {
        var source = new SimpleEnergyHandler(10_000, 0, 100, 10_000);
        assertEquals(100, PowerDrain.pull(source));
        assertEquals(9_900, source.getAmountAsInt());
        assertEquals(100, PowerDrain.pull(source));
    }

    /** A neighbour with less stored than its limit is emptied, an empty or missing one gives nothing. */
    @Test
    void emptiesAndMissing() {
        var source = new SimpleEnergyHandler(10_000, 0, 100, 30);
        assertEquals(30, PowerDrain.pull(source));
        assertEquals(0, PowerDrain.pull(source));
        assertEquals(0, PowerDrain.pull(null));
    }

    /** A neighbour that gives nothing by config is not drained. */
    @Test
    void zeroOutputConfig() {
        assertEquals(0, PowerDrain.pull(new SimpleEnergyHandler(10_000, 0, 0, 10_000)));
    }
}
