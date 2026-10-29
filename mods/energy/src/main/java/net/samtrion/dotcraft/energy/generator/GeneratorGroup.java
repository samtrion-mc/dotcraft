package net.samtrion.dotcraft.energy.generator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The group of generator blocks that touch each other sideways in one plane, whatever their family and tier: they share one
 * pool of FE. A group is never stored; it is found by walking from a block, and the result is cached per block until the
 * {@link #version()} changes, which every placed, removed, loaded or unloaded generator does.
 */
final class GeneratorGroup {
    /** The most blocks of a group, a 16x16 area. */
    static final int MAX_SIZE = 256;

    private static int version;

    /** Not instantiable. */
    private GeneratorGroup() {}

    /**
     * Counts every change of the set of generators; a cached group is stale when it differs.
     *
     * @return the version
     */
    static int version() {
        return version;
    }

    /** Marks every cached group as stale. */
    static void invalidate() {
        version++;
    }

    /**
     * Walks the group of a block.
     * <p>
     * ponytail: the walk stops at {@link #MAX_SIZE} blocks in the order it finds them, so in a larger area the group of a block
     * is its 256 nearest, which may differ between two blocks; no FE is created or lost by that, only the shared view differs.
     *
     * @param level   the level
     * @param start   the block to start at
     * @param exclude a position to treat as empty, e.g. a block that is just removed, or {@code null}
     * @return the blocks of the group, the start block first; empty if there is no generator at the start
     */
    static List<GeneratorBlockEntity> members(Level level, BlockPos start, @Nullable BlockPos exclude) {
        var members = new ArrayList<GeneratorBlockEntity>();
        if (start.equals(exclude) || !(level.getBlockEntity(start) instanceof GeneratorBlockEntity first)) return members;
        Set<BlockPos> seen = new HashSet<>();
        var queue = new ArrayDeque<Found>();
        seen.add(start);
        queue.add(new Found(start, first));
        while (!queue.isEmpty() && members.size() < MAX_SIZE) {
            var at = queue.poll();
            members.add(at.entity());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                var next = at.pos().relative(d);
                if (next.equals(exclude) || !seen.add(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof GeneratorBlockEntity be) queue.add(new Found(next, be));
            }
        }
        return members;
    }

    private record Found(BlockPos pos, GeneratorBlockEntity entity) {}
}
