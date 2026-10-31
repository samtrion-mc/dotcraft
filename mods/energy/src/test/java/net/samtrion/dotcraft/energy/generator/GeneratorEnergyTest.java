package net.samtrion.dotcraft.energy.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Unit tests for the FE buffer of one generator block. */
class GeneratorEnergyTest {
    /** Production stops at the capacity, reports what fitted and rejects non-positive amounts; every change is announced. */
    @Test
    void produceFillsUpToCapacity() {
        var changes = new AtomicInteger();
        var energy = new GeneratorEnergy(100, 10, changes::incrementAndGet);
        assertEquals(60, energy.produce(60));
        assertEquals(40, energy.produce(60));
        assertEquals(0, energy.produce(1));
        assertEquals(0, energy.produce(0));
        assertEquals(0, energy.produce(-5));
        assertEquals(100, energy.getAmountAsInt());
        assertEquals(2, changes.get());
    }

    /** A huge production does not overflow the int buffer. */
    @Test
    void produceDoesNotOverflow() {
        var energy = new GeneratorEnergy(Integer.MAX_VALUE, 1, () -> {});
        assertEquals(Integer.MAX_VALUE - 1, energy.produce(Integer.MAX_VALUE - 1));
        assertEquals(1, energy.produce(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, energy.getAmountAsInt());
    }

    /** A smaller configured capacity caps the stored FE, a larger one keeps it; the output limit follows the config. */
    @Test
    void configureCapsStoredEnergy() {
        var energy = new GeneratorEnergy(1_000, 50, () -> {});
        energy.produce(800);
        energy.configure(2_000, 70);
        assertEquals(800, energy.getAmountAsInt());
        assertEquals(70, energy.maxOutput());
        energy.configure(300, 20);
        assertEquals(300, energy.getAmountAsInt());
        assertEquals(300, energy.getCapacityAsInt());
        assertEquals(20, energy.maxOutput());
    }

    /** The extremes of the config: a capacity of 1 holds 1 FE, a max output of 0 gives nothing. */
    @Test
    void configureExtremes() {
        var energy = new GeneratorEnergy(100, 10, () -> {});
        energy.produce(100);
        energy.configure(1, 0);
        assertEquals(1, energy.getAmountAsInt());
        try (var tx = Transaction.openRoot()) {
            assertEquals(0, energy.extract(10, tx));
            assertEquals(1, energy.extractUnlimited(10, tx));
        }
    }

    /** Nothing can be inserted from outside, whatever the amount. */
    @Test
    void insertIsRejected() {
        var energy = new GeneratorEnergy(100, 10, () -> {});
        try (var tx = Transaction.openRoot()) {
            assertEquals(0, energy.insert(Integer.MAX_VALUE, tx));
        }
        assertEquals(0, energy.getAmountAsInt());
    }

    /** The pool extraction ignores the per-operation limit of the share and restores it afterwards, even when it throws. */
    @Test
    void extractUnlimitedRestoresLimit() {
        var energy = new GeneratorEnergy(1_000, 10, () -> {});
        energy.produce(500);
        try (var tx = Transaction.openRoot()) {
            assertEquals(10, energy.extract(500, tx));
            assertEquals(490, energy.extractUnlimited(500, tx));
            assertEquals(0, energy.extract(500, tx));
        }
        try (var tx = Transaction.openRoot()) {
            try {
                energy.extractUnlimited(-1, tx);
            } catch (IllegalArgumentException expected) {
                // the negative amount is refused
            }
            assertEquals(10, energy.maxOutput());
        }
    }
}
