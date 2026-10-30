package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Helpers the game tests of several families share.
 */
public final class TestSupport {
    /**
     * The empty structure every test of this mod uses: 48 x 24 x 48 blocks of air (see {@code data/dotcraftenergy/structure/spaced.nbt}).
     * The runner puts the tests side by side at a distance of the size of their structure, and the vanilla empty structure is
     * one block, so neighbouring tests were a few blocks apart: the mobs a finished test left (a wither and a warden fighting each
     * other), its lightning and its kites reached the generators of the next test, which hear game events up to 16 blocks away.
     */
    public static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "spaced");

    /** Not instantiable. */
    private TestSupport() {}

    /** The side of the structure in blocks, see {@link #STRUCTURE}. */
    private static final int SIZE = 48;

    /**
     * Wraps a test so that it starts only when the chunks of its whole area (the structure plus one chunk around it) are loaded and
     * entity ticking. The tests sit at far-away places; a body that starts while the chunks still load on other threads places its
     * block entities and spawns its mobs into chunks that do not tick yet, so a bucket refills for fewer ticks than the test waits, a
     * generator does not hear yet and a mob stands still for a few ticks, and the assertions that count on the ticks of the test fail
     * now and then. The area has to cover the whole structure, not only the chunks around its corner: the origin lies anywhere in its
     * chunk, so a block 20 blocks from it (the second generator of a kite test) may be two chunks away and then in a chunk that
     * does not tick, where a kite never rises. The wait blocks the server thread while it runs the pending chunk tasks, so it costs
     * no ticks of the test. The chunks stay forced: the game test world is thrown away afterwards.
     *
     * @param test the test body
     * @return the body that first forces the chunks, waits until they tick and then runs the test
     */
    public static Consumer<GameTestHelper> ticking(Consumer<GameTestHelper> test) {
        return helper -> {
            forceChunks(helper);
            var level = helper.getLevel();
            forEachChunk(helper, (cx, cz) -> level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, true));
            long deadline = System.nanoTime() + 10_000_000_000L;
            level.getServer().managedBlock(() -> ticking(helper) || System.nanoTime() > deadline);
            helper.assertTrue(ticking(helper), "the chunks of the test do not tick");
            test.accept(helper);
        };
    }

    /** Whether all chunks of the area of the test are entity ticking. */
    private static boolean ticking(GameTestHelper helper) {
        boolean[] all = {true};
        forEachChunk(helper, (cx, cz) -> all[0] &= helper.getLevel().isPositionEntityTicking(new BlockPos(cx << 4, 0, cz << 4)));
        return all[0];
    }

    /** Runs an action for every chunk of the area of the test: the structure and one chunk around it. */
    private static void forEachChunk(GameTestHelper helper, java.util.function.BiConsumer<Integer, Integer> action) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (int cx = (origin.getX() >> 4) - 1; cx <= ((origin.getX() + SIZE) >> 4) + 1; cx++) {
            for (int cz = (origin.getZ() >> 4) - 1; cz <= ((origin.getZ() + SIZE) >> 4) + 1; cz++) action.accept(cx, cz);
        }
    }

    /**
     * Forces the chunks of the area of the test.
     *
     * @param helper the game test helper
     */
    public static void forceChunks(GameTestHelper helper) {
        forEachChunk(helper, (cx, cz) -> helper.getLevel().setChunkForced(cx, cz, true));
    }
}
