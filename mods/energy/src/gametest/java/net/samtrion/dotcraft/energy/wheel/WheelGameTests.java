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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;

/**
 * GameTests of the mob wheels, run with {@code ./gradlew :mods:energy:runGameTestServer}. A test structure is tiny and only its
 * chunk ticks, so every test builds in the chunk of its origin, from the chunk's corner on ({@link Area}), at y = 20 above the
 * structure: the zones of the wheels then never reach the blocks and mobs of the other tests, and the test removes what it built
 * (the other tests look at the sky above them). The mobs have no free will, so a test moves them by hand between two calls of
 * {@link WheelBlockEntity#scan} (a scan compares the position with the one before).
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class WheelGameTests {
    /** The height of the floor of all tests, relative to the test; the wheels and the mobs stand on y + 1. */
    private static final int FLOOR = 19;
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
            registry.register(RUN, (Consumer<GameTestHelper>) WheelGameTests::run);
            registry.register(WEIGHTS, (Consumer<GameTestHelper>) WheelGameTests::weights);
            registry.register(CLAIMS, (Consumer<GameTestHelper>) WheelGameTests::claims);
            registry.register(ENERGY, (Consumer<GameTestHelper>) WheelGameTests::energy);
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

    /**
     * A wheel makes nothing for mobs that stand, FE for mobs that moved between two scans, and nothing again when they stand
     * again (tier 2 has no flywheel); the comparator reads the load.
     *
     * @param helper the game test helper
     */
    private static void run(GameTestHelper helper) {
        var area = Area.create(helper);
        var wheel = area.wheel(2, 4, 4);
        var pig = area.spawn(EntityType.PIG, 2.5F, 4.5F);
        var level = area.level();
        area.after(20, () -> {
            settled(helper, pig);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0, "standing pigs make FE: " + wheel.output());
            helper.assertTrue(wheel.signal() == 0, "the comparator reads a standing wheel");
            step(pig, 1);
            wheel.scan(level);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(1).peak();
            helper.assertTrue(wheel.runners() == 1, "one pig is running, wheel counts " + wheel.runners());
            helper.assertTrue(wheel.output() > 0 && wheel.output() < peak, "a single pig makes a part of the peak: " + wheel.output());
            helper.assertTrue(wheel.signal() > 0, "the comparator reads nothing");
            var pos = area.absolute(4, FLOOR + 1, 4);
            helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos, Direction.NORTH) == wheel.signal(), "the block does not report the signal");
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig that stands again still makes FE: " + wheel.output());
            area.finish(pig);
        }, pig);
    }

    /**
     * A horse makes more than a pig, a horse and a pig reach the peak of tier 1, and a creeper, a baby and a hurt pig count nothing.
     *
     * @param helper the game test helper
     */
    private static void weights(GameTestHelper helper) {
        var area = Area.create(helper);
        var wheel = area.wheel(1, 4, 4);
        var level = area.level();
        var pig = area.spawn(EntityType.PIG, 2.5F, 2.5F);
        var horse = area.spawn(EntityType.HORSE, 5.5F, 2.5F);
        var creeper = area.spawn(EntityType.CREEPER, 2.5F, 6.5F);
        var baby = area.spawn(EntityType.PIG, 4.5F, 6.5F);
        baby.setBaby(true);
        var hurt = area.spawn(EntityType.PIG, 2.5F, 4.5F);
        hurt.setHealth(1F);
        area.after(20, () -> {
            settled(helper, pig, horse, creeper, baby, hurt);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(0).peak();
            wheel.scan(level);
            for (var mob : new Mob[] {creeper, baby, hurt}) step(mob, 1);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0, "creeper, baby and hurt pig make FE: " + wheel.output());
            step(pig, 1);
            wheel.scan(level);
            double pigOutput = wheel.output();
            helper.assertTrue(pigOutput > 0 && pigOutput < peak, "a pig makes a part of the peak: " + pigOutput);
            step(horse, 1);
            wheel.scan(level);
            helper.assertTrue(wheel.output() > pigOutput, "a horse does not make more than a pig: " + wheel.output() + " <= " + pigOutput);
            step(pig, -1);
            step(horse, -1);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == peak && wheel.runners() == 2, "a horse and a pig do not reach the peak: " + wheel.output());
            area.finish(pig, horse, creeper, baby, hurt);
        }, pig, horse, creeper, baby, hurt);
    }

    /**
     * A mob in the zone of two wheels drives one of them only, more mobs than the cap of the tier do not make more than the peak,
     * and a tier 1 wheel claims no more than its cap of 2 mobs, so the tier 5 wheel next to it still gets the other three.
     *
     * @param helper the game test helper
     */
    private static void claims(GameTestHelper helper) {
        var area = Area.create(helper);
        var first = area.wheel(1, 3, 4);
        var second = area.wheel(5, 5, 4);
        var level = area.level();
        var pigs = new Mob[5];
        for (int i = 0; i < pigs.length; i++) pigs[i] = area.spawn(EntityType.PIG, i < 3 ? 3F + i : 3.5F + i - 3, i < 3 ? 2.5F : 6.5F);
        area.after(20, () -> {
            settled(helper, pigs);
            first.scan(level);
            second.scan(level);
            for (var pig : pigs) step(pig, 0.6);
            first.scan(level);
            second.scan(level);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(0).peak();
            helper.assertTrue(first.output() == peak && first.runners() == 2, "five pigs make " + first.output() + " with " + first.runners()
                    + " runners on the first wheel, not the peak with the cap of 2");
            helper.assertTrue(second.runners() == 3 && second.output() > 0, "the stronger wheel must get the three pigs the cap of the first leaves: " + second.runners());
            area.finish(pigs);
        }, pigs);
    }

    /**
     * The buffer of a wheel cannot be charged but drained, a wheel pushes into a neighbour that takes FE (the power drain), a solar
     * panel next to a wheel does not become part of a panel group, and a tier 5 wheel coasts when the mobs stop.
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
            drained.scan(level);
            lone.scan(level);
            step(pigA, 1);
            step(pigB, 1);
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
                lone.scan(level);
                double coasting = lone.output();
                helper.assertTrue(coasting > 0 && coasting < peak, "the flywheel of tier 5 does not coast: " + coasting);
                area.finish(pigA, pigB);
            }, pigA, pigB);
        }, pigA, pigB);
    }
}
