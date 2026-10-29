package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Helpers the game tests of several families share.
 */
public final class TestSupport {
    /** Not instantiable. */
    private TestSupport() {}

    /**
     * Wraps a test so that it starts only when the 3x3 chunks around its origin are loaded and entity ticking. The tests sit at far-away
     * places; a body that starts while the chunks still load on other threads places its block entities and spawns its mobs into
     * chunks that do not tick yet, so a bucket refills for fewer ticks than the test waits, a generator does not hear yet and a mob
     * stands still for a few ticks, and the assertions that count on the ticks of the test fail now and then. The wait blocks the
     * server thread while it runs the pending chunk tasks, so it costs no ticks of the test. The chunks stay forced: the game test
     * world is thrown away afterwards.
     *
     * @param test the test body
     * @return the body that first forces the chunks, waits until they tick and then runs the test
     */
    public static Consumer<GameTestHelper> ticking(Consumer<GameTestHelper> test) {
        return helper -> {
            forceChunks(helper);
            var level = helper.getLevel();
            var origin = helper.absolutePos(BlockPos.ZERO);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) level.getChunkSource().getChunk((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, ChunkStatus.FULL, true);
            }
            long deadline = System.nanoTime() + 10_000_000_000L;
            level.getServer().managedBlock(() -> ticking(helper) || System.nanoTime() > deadline);
            helper.assertTrue(ticking(helper), "the chunks of the test do not tick");
            test.accept(helper);
        };
    }

    /** Whether the 3x3 chunks around the origin of the test are entity ticking. */
    private static boolean ticking(GameTestHelper helper) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!helper.getLevel().isPositionEntityTicking(origin.offset(16 * dx, 0, 16 * dz))) return false;
            }
        }
        return true;
    }

    /**
     * Forces the 3x3 chunks around the origin of the test.
     *
     * @param helper the game test helper
     */
    public static void forceChunks(GameTestHelper helper) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) helper.getLevel().setChunkForced((origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, true);
        }
    }
}
