package net.samtrion.dotcraft.energy.melt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where the extra water of a complete recovery (tier 5) goes: the water of a melted packed or blue ice block does not fit into
 * the one block it stood in, so the rest fills the free space around it, nearest first. Free of any world access (the caller says
 * which positions are free), so a unit test can check it.
 */
public final class MeltWater {
    /** How far, in steps through free blocks, the water looks for room; the rest is lost (a sealed hole only holds so much). */
    public static final int MAX_STEPS = 6;

    /** Not instantiable. */
    private MeltWater() {}

    /**
     * Picks the positions for water sources: a breadth-first walk over the six neighbours from the origin through free blocks, so
     * the nearest free blocks come first. The origin itself is not returned (it holds the first source already).
     *
     * @param origin the block the ice stood in
     * @param count  how many sources are to be placed
     * @param free   whether a position is free to take water (air, loaded)
     * @return up to {@code count} distinct positions, fewer when there is not enough room within {@link #MAX_STEPS}
     */
    public static List<BlockPos> spill(BlockPos origin, int count, Predicate<BlockPos> free) {
        List<BlockPos> result = new ArrayList<>();
        var seen = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var start = origin.immutable();
        seen.add(start);
        queue.add(start);
        int steps = 0;
        while (!queue.isEmpty() && result.size() < count && steps++ < MAX_STEPS) {
            for (int n = queue.size(); n > 0 && result.size() < count; n--) {
                var pos = queue.poll();
                for (Direction d : Direction.values()) {
                    var next = pos.relative(d);
                    if (result.size() >= count) break;
                    if (!seen.add(next) || !free.test(next)) continue;
                    result.add(next);
                    queue.add(next);
                }
            }
        }
        return result;
    }
}
