package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Helpers the game tests of several families share.
 */
public final class TestSupport {
    /** Not instantiable. */
    private TestSupport() {}

    /**
     * Wraps a test so that the 3x3 chunks around its origin tick before the ticks of the test run on. The tests sit at far-away places
     * where chunks load on other threads while the server ticks flat out, so without this a block entity or an entity may stand still
     * for the whole timeout. The chunks stay forced: the game test world is thrown away afterwards.
     *
     * @param test the test body
     * @return the body that first forces the chunks and then runs the test
     */
    public static Consumer<GameTestHelper> ticking(Consumer<GameTestHelper> test) {
        return helper -> {
            forceChunks(helper);
            test.accept(helper);
        };
    }

    /**
     * Forces the 3x3 chunks around the origin of the test and slows the ticks of the test down until they all tick.
     *
     * @param helper the game test helper
     */
    public static void forceChunks(GameTestHelper helper) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) helper.getLevel().setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, true);
        }
        helper.onEachTick(() -> {
            boolean ticking = true;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) ticking &= helper.getLevel().isPositionEntityTicking(origin.offset(16 * dx, 0, 16 * dz));
            }
            if (ticking) return;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
