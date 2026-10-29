package net.samtrion.dotcraft.energy.current;

import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * What a current generator reads from the world: the water in the 5x5x5 cube around it, the biome and the animals near. It is
 * the only part of the output that needs a level; {@link CurrentMath} turns the {@link Reading} into FE.
 */
public final class CurrentScan {
    /** Not instantiable. */
    private CurrentScan() {}

    /**
     * One reading of the surroundings.
     *
     * @param sources    the blocks of source water in the cube (the generator's own block is not counted)
     * @param flowing    the blocks of flowing water in the cube
     * @param primeBiome whether the biome is in {@link CurrentTags#PRIME_BIOMES}
     * @param faunaTypes how many different types of animal swim near
     */
    public record Reading(int sources, int flowing, boolean primeBiome, int faunaTypes) {
        /**
         * The share of the peak this reading gives.
         *
         * @param tier      the tier
         * @param submerged whether the generator itself is in water
         * @return the multiplier of the peak
         */
        public double output(int tier, boolean submerged) {
            return CurrentMath.output(tier, submerged, sources, flowing, primeBiome, faunaTypes);
        }
    }

    /**
     * Whether the flow axis is blocked: the block in front of or behind the generator stops the water (see
     * {@link CurrentMath#blocksFlow}). An unloaded block does not block, and it is never force-loaded.
     *
     * @param level  the level
     * @param pos    the position of the generator
     * @param facing the horizontal facing of the generator, the direction of the flow
     * @return {@code true} if the output must be 0
     */
    public static boolean axisBlocked(ServerLevel level, BlockPos pos, Direction facing) {
        for (Direction d : new Direction[] {facing, facing.getOpposite()}) {
            var at = pos.relative(d);
            if (!level.isLoaded(at)) continue;
            var state = level.getBlockState(at);
            if (CurrentMath.blocksFlow(state.isCollisionShapeFullBlock(level, at), state.getFluidState().is(FluidTags.WATER), state.canBeReplaced())) return true;
        }
        return false;
    }

    /**
     * Reads the surroundings. Every block of the cube whose fluid is water counts (also a waterlogged block or kelp, but not another current generator): a source as
     * a source, the rest as flowing; that is at most 124 look-ups, which is why the block entity does it only every few
     * ticks. The animals count by <em>distinct type</em>: a tank full of cod is one type. Wild and bucketed animals count alike, as
     * long as they are alive and the block at the middle of their body is water (a look-up in the world, not the flag of the
     * entity, which is only updated when the entity ticks).
     *
     * @param level the level
     * @param pos   the position of the generator
     * @param tier  the tier, which sets the fauna radius
     * @return the reading
     */
    public static Reading scan(ServerLevel level, BlockPos pos, int tier) {
        int sources = 0;
        int flowing = 0;
        var cursor = new BlockPos.MutableBlockPos();
        for (int dx = -CurrentMath.SCAN_RADIUS; dx <= CurrentMath.SCAN_RADIUS; dx++) {
            for (int dy = -CurrentMath.SCAN_RADIUS; dy <= CurrentMath.SCAN_RADIUS; dy++) {
                for (int dz = -CurrentMath.SCAN_RADIUS; dz <= CurrentMath.SCAN_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.isLoaded(cursor)) continue; // never force-load a chunk
                    var state = level.getBlockState(cursor);
                    // another generator holds its own water: a packed array of pods is no body of water
                    if (state.getBlock() instanceof CurrentGeneratorBlock) continue;
                    var fluid = state.getFluidState();
                    if (!fluid.is(FluidTags.WATER)) continue;
                    if (fluid.isSource()) sources++;
                    else flowing++;
                }
            }
        }
        var types = new HashSet<EntityType<?>>();
        var box = new AABB(pos).inflate(CurrentMath.faunaRadius(tier));
        for (var animal : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.typeHolder().is(CurrentTags.FAUNA) && level.getFluidState(BlockPos.containing(e.getBoundingBox().getCenter())).is(FluidTags.WATER))) {
            types.add(animal.getType());
        }
        return new Reading(sources, flowing, level.getBiome(pos).is(CurrentTags.PRIME_BIOMES), types.size());
    }
}
