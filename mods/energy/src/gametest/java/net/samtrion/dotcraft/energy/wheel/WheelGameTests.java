package net.samtrion.dotcraft.energy.wheel;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.TestSupport;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;

/**
 * GameTests of the mob wheels, run with {@code ./gradlew :mods:energy:runGameTestServer}. A test structure is tiny and only its
 * chunk ticks, so every test builds in the chunk of its origin, from the chunk's corner on ({@link Area}), high above the
 * structure: the wheels then never reach the blocks and mobs of the other tests, and the test removes what it built
 * (the other tests look at the sky above them). The mobs have no free will, so a test puts them on the running plate of a wheel
 * ({@link WheelPlate}) and moves them by hand between two calls of {@link WheelBlockEntity#scan} (a scan compares the position
 * with the one before).
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class WheelGameTests {
    /** The height of the floor of all tests, relative to the test (high above what the other families build, kites fly up the columns below); the wheels and the mobs stand on y + 1. */
    private static final int FLOOR = 150;
    /** The edge of the area of a test: it must fit into a chunk. */
    private static final int SIZE = 14;

    private static final Identifier RUN = id("wheel_run");
    private static final Identifier WEIGHTS = id("wheel_weights");
    private static final Identifier CLAIMS = id("wheel_claims");
    private static final Identifier ENERGY = id("wheel_energy");

    private WheelGameTests() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, path);
    }

    private static ResourceKey<Consumer<GameTestHelper>> function(Identifier id) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id);
    }

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(RUN, TestSupport.ticking(WheelGameTests::run));
            registry.register(WEIGHTS, TestSupport.ticking(WheelGameTests::weights));
            registry.register(CLAIMS, TestSupport.ticking(WheelGameTests::claims));
            registry.register(ENERGY, TestSupport.ticking(WheelGameTests::energy));
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (var id : new Identifier[] {RUN, WEIGHTS, CLAIMS, ENERGY}) {
            var environment = event.registerEnvironment(id, new TestEnvironmentDefinition.AllOf());
            event.registerTest(id, new FunctionGameTestInstance(function(id),
                    new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        }
    }

    /**
     * The area of a test: a stone floor of {@link #SIZE} blocks square in the chunk of the origin of the test, from its corner (plus
     * one block) on. The coordinates of its methods are local to the area.
     *
     * @param helper the game test helper
     * @param dx     the offset of the area from the origin of the test in x
     * @param dz     the offset in z
     */
    private record Area(GameTestHelper helper, int dx, int dz) {
        /** Builds the floor and forces the chunk, so that mobs and block entities tick. */
        static Area create(GameTestHelper helper) {
            var origin = helper.absolutePos(BlockPos.ZERO);
            var area = new Area(helper, (origin.getX() >> 4 << 4) + 1 - origin.getX(), (origin.getZ() >> 4 << 4) + 1 - origin.getZ());
            var corner = area.absolute(0, 0, 0);
            helper.getLevel().setChunkForced(corner.getX() >> 4, corner.getZ() >> 4, true);
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) area.set(x, FLOOR, z, Blocks.STONE);
            }
            return area;
        }

        ServerLevel level() {
            return helper.getLevel();
        }

        BlockPos relative(int x, int y, int z) {
            return new BlockPos(x + dx, y, z + dz);
        }

        BlockPos absolute(int x, int y, int z) {
            return helper.absolutePos(relative(x, y, z));
        }

        void set(int x, int y, int z, Block block) {
            helper.setBlock(relative(x, y, z), block);
        }

        /** Places a wheel on the floor and returns its block entity. */
        WheelBlockEntity wheel(int tier, int x, int z) {
            set(x, FLOOR + 1, z, WheelBlocks.block(tier).get());
            return (WheelBlockEntity) level().getBlockEntity(absolute(x, FLOOR + 1, z));
        }

        /** Spawns a mob without free will on the floor. */
        <E extends Mob> E spawn(EntityType<E> type, float x, float z) {
            return helper.spawnWithNoFreeWill(type, x + dx, FLOOR + 1F, z + dz);
        }

        /** Puts a mob on the running plate of the wheel in the block that contains (x, z), by hand: it keeps its ground flag until its next tick. */
        void place(Mob mob, double x, double z) {
            mob.setPos(helper.absoluteVec(new Vec3(x + dx, FLOOR + 1 + WheelPlate.TOP, z + dz)));
        }

        /** Puts a mob on the stone floor, where no plate is. */
        void ground(Mob mob, double x, double z) {
            mob.setPos(helper.absoluteVec(new Vec3(x + dx, FLOOR + 1, z + dz)));
        }

        /** Runs a part of a test after a delay; when it fails, the area is cleaned up before the failure is rethrown. */
        void after(int ticks, Runnable body, Mob... mobs) {
            helper.runAfterDelay(ticks, () -> {
                try {
                    body.run();
                } catch (RuntimeException | Error e) {
                    cleanup(mobs);
                    throw e;
                }
            });
        }

        /** Cleans up and ends the test successfully. */
        void finish(Mob... mobs) {
            cleanup(mobs);
            helper.succeed();
        }

        /** Removes what the test built and its mobs, so the other tests are not disturbed. */
        void cleanup(Mob... mobs) {
            for (var mob : mobs) mob.discard();
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    for (int y = FLOOR; y <= FLOOR + 2; y++) set(x, y, z, Blocks.AIR);
                }
            }
            var corner = absolute(0, 0, 0);
            level().setChunkForced(corner.getX() >> 4, corner.getZ() >> 4, false);
        }
    }

    /** Fails unless the mobs have landed: a mob that does not tick (chunk not ticking) never gets its ground flag. */
    private static void settled(GameTestHelper helper, Mob... mobs) {
        for (var mob : mobs) helper.assertTrue(mob.onGround(), "a " + mob.getType() + " has not landed, ticks: " + mob.tickCount);
    }

    /** Moves a mob sideways by hand, as a scan compares positions: a mob without free will does not move by itself. */
    private static void step(Mob mob, double dx) {
        mob.setPos(mob.getX() + dx, mob.getY(), mob.getZ());
    }

    /** The left end of the plate in x, as a share of the block: a mob that goes from here to {@link #RIGHT} has run far enough. */
    private static final double LEFT = 0.2;
    /** The right end of the plate in x, as a share of the block. */
    private static final double RIGHT = 0.8;

    /**
     * A wheel makes nothing for a pig that walks beside it or three blocks away, half for a pig that stands on its plate, the
     * full weight for a pig that ran on it, and nothing again when the pig leaves the plate (tier 2 has no flywheel); the
     * comparator reads the load.
     *
     * @param helper the game test helper
     */
    private static void run(GameTestHelper helper) {
        var area = Area.create(helper);
        var wheel = area.wheel(2, 4, 4);
        var pig = area.spawn(EntityType.PIG, 1.5F, 4.5F);
        var level = area.level();
        area.after(20, () -> {
            settled(helper, pig);
            wheel.scan(level);
            step(pig, 0.7);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig three blocks away makes FE: " + wheel.output());
            area.ground(pig, 5.5, 4.5);
            wheel.scan(level);
            step(pig, 0.7);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig that runs next to the wheel makes FE: " + wheel.output());
            helper.assertTrue(wheel.signal() == 0, "the comparator reads a wheel nothing stands on");
            area.place(pig, 4 + LEFT, 4.5);
            wheel.scan(level);
            double standing = wheel.output();
            double peak = EnergyCommonConfig.MOB_WHEELS.get(1).peak();
            helper.assertTrue(wheel.runners() == 1, "one pig stands on the plate, wheel counts " + wheel.runners());
            helper.assertTrue(standing > 0 && standing < peak, "a standing pig makes a part of the peak: " + standing);
            area.place(pig, 4 + RIGHT, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.runners() == 1 && wheel.output() > standing, "a running pig makes more than a standing one: " + wheel.output() + " <= " + standing);
            helper.assertTrue(wheel.signal() > 0, "the comparator reads nothing");
            var pos = area.absolute(4, FLOOR + 1, 4);
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos, Direction.NORTH) == wheel.signal(), "the block does not report the signal");
            area.ground(pig, 5.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig that left the plate still makes FE: " + wheel.output());
            area.finish(pig);
        }, pig);
    }

    /**
     * A horse makes more than a pig, a horse and a pig that run reach the peak of tier 1, and a creeper, a baby and a hurt pig on the
     * plate count nothing.
     *
     * @param helper the game test helper
     */
    private static void weights(GameTestHelper helper) {
        var area = Area.create(helper);
        var wheel = area.wheel(1, 4, 4);
        var level = area.level();
        var pig = area.spawn(EntityType.PIG, 2.5F, 2.5F);
        var horse = area.spawn(EntityType.HORSE, 6.5F, 2.5F);
        var creeper = area.spawn(EntityType.CREEPER, 2.5F, 6.5F);
        var baby = area.spawn(EntityType.PIG, 4.5F, 6.5F);
        baby.setBaby(true);
        var hurt = area.spawn(EntityType.PIG, 2.5F, 4.5F);
        hurt.setHealth(1F);
        area.after(20, () -> {
            settled(helper, pig, horse, creeper, baby, hurt);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(0).peak();
            for (var mob : new Mob[] {creeper, baby, hurt}) area.place(mob, 4.5, 4.5);
            wheel.scan(level);
            for (var mob : new Mob[] {creeper, baby, hurt}) area.place(mob, 4.5, 4.7);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0, "creeper, baby and hurt pig make FE: " + wheel.output());
            for (var mob : new Mob[] {creeper, baby, hurt}) area.ground(mob, 1.5, 1.5);
            area.place(pig, 4 + LEFT, 4.5);
            wheel.scan(level);
            area.place(pig, 4 + RIGHT, 4.5);
            wheel.scan(level);
            double pigOutput = wheel.output();
            helper.assertTrue(pigOutput > 0 && pigOutput < peak, "a pig makes a part of the peak: " + pigOutput);
            area.place(pig, 4 + LEFT, 4.5);
            area.place(horse, 4 + LEFT, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() > pigOutput, "a horse does not make more than a pig: " + wheel.output() + " <= " + pigOutput);
            area.place(pig, 4 + RIGHT, 4.5);
            area.place(horse, 4 + RIGHT, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == peak && wheel.runners() == 2, "a horse and a pig do not reach the peak: " + wheel.output());
            area.finish(pig, horse, creeper, baby, hurt);
        }, pig, horse, creeper, baby, hurt);
    }

    /**
     * A tier 1 wheel counts no more than its cap of 2 of three pigs on its plate (the peak), a mob that moves on to the next wheel
     * is claimed by the first one for a moment (one mob feeds one wheel) and counts for the next wheel when the claim has lapsed.
     *
     * @param helper the game test helper
     */
    private static void claims(GameTestHelper helper) {
        var area = Area.create(helper);
        var first = area.wheel(1, 3, 4);
        var second = area.wheel(5, 5, 4);
        var level = area.level();
        var pigs = new Mob[3];
        for (int i = 0; i < pigs.length; i++) pigs[i] = area.spawn(EntityType.PIG, 3.5F, 8.5F + i * 0.2F);
        area.after(20, () -> {
            settled(helper, pigs);
            for (int i = 0; i < pigs.length; i++) area.place(pigs[i], 3 + LEFT, 4.3 + i * 0.2);
            first.scan(level);
            for (int i = 0; i < pigs.length; i++) area.place(pigs[i], 3 + RIGHT, 4.3 + i * 0.2);
            first.scan(level);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(0).peak();
            helper.assertTrue(first.output() == peak && first.runners() == 2, "three pigs make " + first.output() + " with " + first.runners()
                    + " runners, not the peak with the cap of 2");
            for (int i = 1; i < pigs.length; i++) area.ground(pigs[i], 1.5, 10.5);
            area.place(pigs[0], 5 + LEFT, 4.5);
            second.scan(level);
            helper.assertTrue(second.runners() == 0, "a pig the first wheel just counted drives the second one as well: " + second.runners());
            area.after((int) WheelClaims.LAPSE_TICKS + 1, () -> {
                area.place(pigs[0], 5 + LEFT, 4.5);
                second.scan(level);
                helper.assertTrue(second.runners() == 1 && second.output() > 0, "the pig does not drive the second wheel after the claim lapsed: " + second.runners());
                first.scan(level);
                helper.assertTrue(first.runners() == 0, "the first wheel still counts a pig that left it: " + first.runners());
                area.finish(pigs);
            }, pigs);
        }, pigs);
    }

    /**
     * The buffer of a wheel cannot be charged but drained, a wheel pushes into a neighbour that takes FE (the power drain), a solar
     * panel next to a wheel does not become part of a panel group, and a tier 5 wheel coasts when the mobs leave the plate.
     *
     * @param helper the game test helper
     */
    private static void energy(GameTestHelper helper) {
        var area = Area.create(helper);
        var drained = area.wheel(5, 3, 4);
        area.set(4, FLOOR + 1, 4, EnergyBlocks.POWER_DRAIN.get());
        var lone = area.wheel(5, 11, 4);
        area.set(12, FLOOR + 1, 4, EnergyBlocks.SOLAR.block(1).get());
        var level = area.level();
        var pigA = area.spawn(EntityType.PIG, 1.5F, 4.5F);
        var pigB = area.spawn(EntityType.PIG, 9.5F, 4.5F);
        area.after(20, () -> {
            settled(helper, pigA, pigB);
            area.place(pigA, 3 + LEFT, 4.5);
            area.place(pigB, 11 + LEFT, 4.5);
            drained.scan(level);
            lone.scan(level);
            area.place(pigA, 3 + RIGHT, 4.5);
            area.place(pigB, 11 + RIGHT, 4.5);
            drained.scan(level);
            lone.scan(level);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(4).peak();
            helper.assertTrue(drained.output() > 0 && lone.output() > 0, "tier 5 wheels with a running pig make nothing: " + drained.output() + ", " + lone.output());
            var handler = level.getCapability(Capabilities.Energy.BLOCK, area.absolute(11, FLOOR + 1, 4), null);
            helper.assertTrue(handler != null, "the wheel has no energy capability");
            try (var tx = Transaction.openRoot()) {
                helper.assertTrue(handler.insert(1000, tx) == 0, "the wheel accepted FE");
            }
            var panel = level.getBlockState(area.absolute(12, FLOOR + 1, 4));
            helper.assertTrue(!panel.getValue(GeneratorBlock.WEST), "a solar panel next to a wheel joins a panel group");
            area.after(8, () -> {
                helper.assertTrue(drained.energy().getAmountAsLong() == 0, "the wheel next to the drain keeps FE: " + drained.energy().getAmountAsLong());
                helper.assertTrue(lone.energy().getAmountAsLong() > 0, "the lone wheel made no FE");
                try (var tx = Transaction.openRoot()) {
                    int taken = lone.energy().extract(Integer.MAX_VALUE, tx);
                    helper.assertTrue(taken > 0 && taken <= EnergyCommonConfig.MOB_WHEELS.get(4).maxOutput(), "extraction not limited by the config: " + taken);
                }
                area.ground(pigB, 9.5, 4.5);
                lone.scan(level);
                double coasting = lone.output();
                helper.assertTrue(coasting > 0 && coasting < peak, "the flywheel of tier 5 does not coast: " + coasting);
                area.finish(pigA, pigB);
            }, pigA, pigB);
        }, pigA, pigB);
    }
}
