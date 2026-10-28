package net.samtrion.dotcraft.resources.worldgen;

import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.samtrion.dotcraft.resources.common.OreExposure;

/**
 * An ore vein built like the vanilla ore feature builds it (the same chain of spheres), with one difference: a block of
 * the vein is only placed if at least one of its 6 neighbours matches one of the {@link ExposedOreConfiguration#exposure()
 * exposures}, e.g. air for ore that shows on cave walls and on the surface. A vein in rock without such a neighbour
 * places nothing. The neighbours are read from the world while the feature runs; a block that the feature placed itself
 * never counts, as ore is neither air nor a fluid.
 */
public class ExposedOreFeature extends Feature<ExposedOreConfiguration> {
    /** Creates the feature. */
    public ExposedOreFeature() {
        super(ExposedOreConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ExposedOreConfiguration> context) {
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        ExposedOreConfiguration config = context.config();
        int size = config.ore().size;
        float dir = random.nextFloat() * (float) Math.PI;
        float spreadXY = size / 8.0F;
        int maxRadius = Mth.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double x0 = origin.getX() + Math.sin(dir) * spreadXY;
        double x1 = origin.getX() - Math.sin(dir) * spreadXY;
        double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
        double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
        int yStart = origin.getY() - 2 - maxRadius;
        int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
        int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);
        int sizeY = 2 * (2 + maxRadius);

        for (int xprobe = xStart; xprobe <= xStart + sizeXZ; xprobe++) {
            for (int zprobe = zStart; zprobe <= zStart + sizeXZ; zprobe++) {
                if (yStart <= level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, xprobe, zprobe)) {
                    return doPlace(level, random, config, new double[] {x0, x1, z0, z1, y0, y1},
                            new int[] {xStart, yStart, zStart, sizeXZ, sizeY});
                }
            }
        }
        return false;
    }

    /**
     * Builds the vein: the spheres along the line from the first to the second end, and every block of them that is
     * a target, has an exposed neighbour and is inside the build height is replaced.
     *
     * @param level  the level to place in
     * @param random the random source
     * @param config the config of the feature
     * @param ends   {@code x0, x1, z0, z1, y0, y1}, the two ends of the line
     * @param box    {@code xStart, yStart, zStart, sizeXZ, sizeY}, the box the vein fits in
     * @return {@code true} if at least one block was placed
     */
    private boolean doPlace(WorldGenLevel level, RandomSource random, ExposedOreConfiguration config, double[] ends, int[] box) {
        int xStart = box[0];
        int yStart = box[1];
        int zStart = box[2];
        int sizeXZ = box[3];
        int sizeY = box[4];
        OreConfiguration ore = config.ore();
        int placed = 0;
        BitSet tested = new BitSet(sizeXZ * sizeY * sizeXZ);
        BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        int size = ore.size;
        double[] data = new double[size * 4];

        for (int i = 0; i < size; i++) {
            float step = (float) i / size;
            double ss = random.nextDouble() * size / 16.0;
            data[i * 4] = Mth.lerp((double) step, ends[0], ends[1]);
            data[i * 4 + 1] = Mth.lerp((double) step, ends[4], ends[5]);
            data[i * 4 + 2] = Mth.lerp((double) step, ends[2], ends[3]);
            data[i * 4 + 3] = ((Mth.sin((float) Math.PI * step) + 1.0F) * ss + 1.0) / 2.0;
        }

        for (int i1 = 0; i1 < size - 1; i1++) {
            if (data[i1 * 4 + 3] <= 0.0) continue;
            for (int i2 = i1 + 1; i2 < size; i2++) {
                if (data[i2 * 4 + 3] <= 0.0) continue;
                double dx = data[i1 * 4] - data[i2 * 4];
                double dy = data[i1 * 4 + 1] - data[i2 * 4 + 1];
                double dz = data[i1 * 4 + 2] - data[i2 * 4 + 2];
                double dr = data[i1 * 4 + 3] - data[i2 * 4 + 3];
                if (dr * dr > dx * dx + dy * dy + dz * dz) {
                    data[(dr > 0.0 ? i2 : i1) * 4 + 3] = -1.0;
                }
            }
        }

        try (BulkSectionAccess sections = new BulkSectionAccess(level)) {
            for (int i = 0; i < size; i++) {
                double r = data[i * 4 + 3];
                if (r < 0.0) continue;
                double xx = data[i * 4];
                double yy = data[i * 4 + 1];
                double zz = data[i * 4 + 2];
                int xMin = Math.max(Mth.floor(xx - r), xStart);
                int yMin = Math.max(Mth.floor(yy - r), yStart);
                int zMin = Math.max(Mth.floor(zz - r), zStart);
                int xMax = Math.max(Mth.floor(xx + r), xMin);
                int yMax = Math.max(Mth.floor(yy + r), yMin);
                int zMax = Math.max(Mth.floor(zz + r), zMin);

                for (int x = xMin; x <= xMax; x++) {
                    double xd = (x + 0.5 - xx) / r;
                    if (xd * xd >= 1.0) continue;
                    for (int y = yMin; y <= yMax; y++) {
                        double yd = (y + 0.5 - yy) / r;
                        if (xd * xd + yd * yd >= 1.0) continue;
                        for (int z = zMin; z <= zMax; z++) {
                            double zd = (z + 0.5 - zz) / r;
                            if (xd * xd + yd * yd + zd * zd >= 1.0 || level.isOutsideBuildHeight(y)) continue;
                            int index = x - xStart + (y - yStart) * sizeXZ + (z - zStart) * sizeXZ * sizeY;
                            if (tested.get(index)) continue;
                            tested.set(index);
                            orePos.set(x, y, z);
                            if (!level.ensureCanWrite(orePos)) continue;
                            LevelChunkSection section = sections.getSection(orePos);
                            if (section == null) continue;
                            int sx = SectionPos.sectionRelative(x);
                            int sy = SectionPos.sectionRelative(y);
                            int sz = SectionPos.sectionRelative(z);
                            BlockState current = section.getBlockState(sx, sy, sz);
                            for (OreConfiguration.TargetBlockState target : ore.targetStates) {
                                if (target.target.test(current, random)
                                        && isExposed(sections::getBlockState, orePos, neighbour, config.exposure())) {
                                    section.setBlockState(sx, sy, sz, target.state, false);
                                    placed++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return placed > 0;
    }

    /**
     * Tells whether a neighbour of a position is of one of the kinds.
     *
     * @param getter    reads the block state at a position
     * @param pos       the position of the block that would be placed
     * @param neighbour scratch position
     * @param exposure  the kinds that count
     * @return {@code true} if at least one of the 6 neighbours matches any kind
     */
    static boolean isExposed(Function<BlockPos, BlockState> getter, BlockPos pos, BlockPos.MutableBlockPos neighbour,
                             List<OreExposure> exposure) {
        for (Direction direction : Direction.values()) {
            BlockState state = getter.apply(neighbour.setWithOffset(pos, direction));
            for (OreExposure kind : exposure) {
                if (kind.matches(state)) return true;
            }
        }
        return false;
    }
}
