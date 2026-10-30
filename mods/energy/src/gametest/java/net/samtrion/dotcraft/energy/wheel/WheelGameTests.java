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
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * ({@link WheelPlate}) by hand, makes them want to walk and calls {@link WheelBlockEntity#scan} itself (a captured mob is pinned, it
 * never really moves).
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
    private static final Identifier SLOT = id("wheel_slot");
    private static final Identifier PIN = id("wheel_pin");
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
            registry.register(SLOT, TestSupport.ticking(WheelGameTests::slot));
            registry.register(PIN, TestSupport.ticking(WheelGameTests::pin));
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
        for (var id : new Identifier[] {RUN, WEIGHTS, PIN, CLAIMS, SLOT, ENERGY}) {
            var environment = event.registerEnvironment(id, new TestEnvironmentDefinition.AllOf());
            event.registerTest(id, new FunctionGameTestInstance(function(id),
                    new TestData<>(environment, TestSupport.STRUCTURE, 100, 0, true)));
        }
    }

    /**
     * The area of a test: a stone floor of {@link #SIZE} blocks square in the chunk of the origin of the test, from its corner (plus
     * one block) on. The coordinates of its methods are local to the area.
     *
     * @param helper the game test helper
     * @param dx     the offset of the area from the origin of the test in x
     * @param dz     the offset in z
     * @param lift   the height of the lane of the test above the floor level
     */
    private record Area(GameTestHelper helper, int dx, int dz, int lift) {
        /** Builds the floor and forces the chunk, so that mobs and block entities tick; every test has its own lane (6 blocks higher), as the tests of a batch run at the same time and their origins may share a chunk. */
        static Area create(GameTestHelper helper, int lane) {
            var origin = helper.absolutePos(BlockPos.ZERO);
            var area = new Area(helper, (origin.getX() >> 4 << 4) + 1 - origin.getX(), (origin.getZ() >> 4 << 4) + 1 - origin.getZ(), 6 * lane);
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
            return new BlockPos(x + dx, y + lift, z + dz);
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
            return helper.spawnWithNoFreeWill(type, x + dx, FLOOR + 1F + lift, z + dz);
        }

        /** Puts a mob on the running plate of the wheel in the block that contains (x, z), by hand: it keeps its ground flag until its next tick. */
        void place(Mob mob, double x, double z) {
            mob.setPos(helper.absoluteVec(new Vec3(x + dx, FLOOR + 1 + lift + WheelPlate.TOP, z + dz)));
        }

        /** Puts a mob on the stone floor, where no plate is. */
        void ground(Mob mob, double x, double z) {
            mob.setPos(helper.absoluteVec(new Vec3(x + dx, FLOOR + 1 + lift, z + dz)));
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
        }
    }

    /** Fails unless the mobs have landed: a mob that does not tick (chunk not ticking) never gets its ground flag. */
    private static void settled(GameTestHelper helper, Mob... mobs) {
        for (var mob : mobs) {
            helper.assertTrue(mob.onGround(), "a " + mob.getType() + " has not landed, ticks: " + mob.tickCount + " at " + mob.position() + " delta " + mob.getDeltaMovement() + " below " + mob.level().getBlockState(mob.blockPosition().below()));
        }
    }

    /** Moves a mob sideways by hand, as a scan compares positions: a mob without free will does not move by itself. */
    private static void step(Mob mob, double dx) {
        mob.setPos(mob.getX() + dx, mob.getY(), mob.getZ());
    }

    /** A spot on the left side of the plate in x, as a share of the block. */
    private static final double LEFT = 0.2;

    /** Makes the mob want to walk to a spot a few blocks away, which is what its goals do; it never gets there on a wheel. */
    private static void wantToWalk(Mob mob) {
        mob.getMoveControl().setWantedPosition(mob.getX() + 5, mob.getY(), mob.getZ(), 1.0);
    }

    /** The share of the peak a mob of the type makes at tier 1 when it runs or stands, from the math. */
    private static double expected(Mob mob, double peak, boolean running) {
        return peak * WheelMath.share(WheelMath.weight(mob.getMaxHealth(), mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 1), running);
    }

    /**
     * A wheel makes nothing for a pig that stands beside it or three blocks away, captures a pig on its plate (a standing pig makes
     * the standing share, a pig that wants to walk the running share of the peak, see {@link WheelMath#share}), makes nothing again
     * when the pig leaves the plate (tier 2 has no flywheel); the comparator reads the load.
     *
     * @param helper the game test helper
     */
    private static void run(GameTestHelper helper) {
        var area = Area.create(helper, 1);
        var wheel = area.wheel(2, 4, 4);
        var level = area.level();
        var pig = area.spawn(EntityType.PIG, 1.5F, 4.5F);
        area.after(20, () -> {
            settled(helper, pig);
            double peak = EnergyCommonConfig.MOB_WHEELS.get(1).peak();
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig three blocks away makes FE: " + wheel.output());
            area.ground(pig, 5.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a pig beside the wheel makes FE: " + wheel.output());
            area.place(pig, 4.5, 4.5);
            wheel.scan(level);
            double pigAsTier2 = peak * WheelMath.share(WheelMath.weight(pig.getMaxHealth(), pig.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 2), false);
            helper.assertTrue(wheel.runners() == 1 && wheel.heldMob() == pig, "the pig on the plate is not captured");
            helper.assertTrue(Math.abs(wheel.output() - pigAsTier2) < 1e-9, "a standing pig makes " + wheel.output() + ", not " + pigAsTier2);
            helper.assertTrue(wheel.signal() > 0, "the comparator reads nothing");
            wantToWalk(pig);
            wheel.scan(level);
            double running = peak * WheelMath.share(WheelMath.weight(pig.getMaxHealth(), pig.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), 2), true);
            helper.assertTrue(Math.abs(wheel.output() - running) < 1e-9 && running > pigAsTier2, "a pig that wants to walk makes " + wheel.output() + ", not " + running);
            area.ground(pig, 1.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0 && wheel.heldMob() == null, "a pig that left the plate still makes FE: " + wheel.output());
            area.finish(pig);
        }, pig);
    }

    /**
     * Any mob counts and the weight comes from its attributes: a baby is not captured, a creeper (hostile) and a hurt pig are, and a
     * running horse makes more than a running pig.
     *
     * @param helper the game test helper
     */
    private static void weights(GameTestHelper helper) {
        var area = Area.create(helper, 2);
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
            area.place(baby, 4.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.output() == 0 && wheel.runners() == 0, "a baby makes FE: " + wheel.output());
            area.ground(baby, 1.5, 1.5);
            area.place(creeper, 4.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == creeper && wheel.output() > 0, "a hostile mob on the plate does not count: " + wheel.output());
            area.ground(creeper, 1.5, 1.5);
            wheel.scan(level);
            helper.assertTrue(wheel.runners() == 0, "the wheel keeps a creeper that left it");
            area.place(hurt, 4.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == hurt && wheel.output() > 0, "a hurt pig makes nothing: there is no health rule");
            area.ground(hurt, 1.5, 1.5);
            area.place(pig, 4.5, 4.5);
            wantToWalk(pig);
            wheel.scan(level);
            double pigOutput = wheel.output();
            helper.assertTrue(Math.abs(pigOutput - expected(pig, peak, true)) < 1e-9 && pigOutput < peak, "a running pig makes a part of the peak: " + pigOutput);
            area.ground(pig, 1.5, 1.5);
            area.place(horse, 4.5, 4.5);
            wantToWalk(horse);
            wheel.scan(level);
            helper.assertTrue(wheel.output() > pigOutput, "a horse does not make more than a pig: " + wheel.output() + " <= " + pigOutput);
            helper.assertTrue(Math.abs(wheel.output() - expected(horse, peak, true)) < 1e-9, "a running horse makes " + wheel.output());
            area.finish(pig, horse, creeper, baby, hurt);
        }, pig, horse, creeper, baby, hurt);
    }

    /**
     * A wheel holds one mob: of two pigs on the plate only the one nearer to the middle is held, the other is ignored until the first
     * one dies, then the slot is free and the second is captured.
     *
     * @param helper the game test helper
     */
    private static void slot(GameTestHelper helper) {
        var area = Area.create(helper, 3);
        var wheel = area.wheel(1, 4, 4);
        var level = area.level();
        var first = area.spawn(EntityType.PIG, 2.5F, 2.5F);
        var second = area.spawn(EntityType.PIG, 6.5F, 2.5F);
        area.after(20, () -> {
            settled(helper, first, second);
            area.place(second, 4 + LEFT, 4.5);
            area.place(first, 4.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == first && wheel.runners() == 1, "the pig in the middle is not the one that is held");
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == first, "the wheel let go of its pig for another one on the plate");
            double output = wheel.output();
            first.kill(level);
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == second, "the dead pig still blocks the slot");
            helper.assertTrue(wheel.output() > 0 && wheel.output() <= output + 1e-9, "the output after the change is " + wheel.output());
            area.finish(first, second);
        }, first, second);
    }

    /**
     * A captured mob cannot leave: pushed, knocked back and wanting to walk away it has no horizontal movement and the same spot on the
     * plate 20 ticks later, and it then counts as running. The wheel makes it persistent (a mob that was not before) and gives the old
     * state back when the wheel is broken, which frees the slot.
     *
     * @param helper the game test helper
     */
    private static void pin(GameTestHelper helper) {
        var area = Area.create(helper, 4);
        var wheel = area.wheel(1, 4, 4);
        var level = area.level();
        var pig = area.spawn(EntityType.PIG, 2.5F, 2.5F);
        area.after(20, () -> {
            settled(helper, pig);
            WheelBlockEntity.clearPersistence(pig);
            helper.assertTrue(!pig.isPersistenceRequired(), "the persistence flag cannot be cleared");
            area.place(pig, 4.5, 4.5);
            wheel.scan(level);
            helper.assertTrue(wheel.heldMob() == pig, "the pig is not captured");
            helper.assertTrue(pig.isPersistenceRequired(), "a captured mob may despawn");
            double x = pig.getX();
            double z = pig.getZ();
            wantToWalk(pig);
            pig.setDeltaMovement(0.6, 0, 0.3);
            pig.knockback(1.0, 1, 0);
            area.after(20, () -> {
                helper.assertTrue(Math.abs(pig.getX() - x) < 1e-3 && Math.abs(pig.getZ() - z) < 1e-3, "the pig moved to " + pig.position() + " from " + x + ", " + z);
                var delta = pig.getDeltaMovement();
                helper.assertTrue(Math.abs(delta.x) < 1e-3 && Math.abs(delta.z) < 1e-3, "the pig still has a movement: " + delta);
                var p = pig.position();
                helper.assertTrue(WheelPlate.carries(false, p.x - wheel.getBlockPos().getX(), p.y - wheel.getBlockPos().getY(), p.z - wheel.getBlockPos().getZ()),
                        "the pig is not on the plate any more: " + p);
                // what the goals of a mob leave behind after a tick (a forward impulse) is noted by the wheel when the mob has ticked
                pig.zza = 0.3F;
                wheel.hold(pig);
                wheel.scan(level);
                helper.assertTrue(wheel.heldMob() == pig, "the wheel lost its pig");
                helper.assertTrue(wheel.output() > expected(pig, EnergyCommonConfig.MOB_WHEELS.get(0).peak(), false) + 1e-9, "a pig that tries to walk does not count as running: " + wheel.output());
                helper.setBlock(area.relative(4, FLOOR + 1, 4), Blocks.AIR);
                helper.assertTrue(wheel.heldMob() == null, "breaking the wheel does not free the pig");
                helper.assertTrue(!pig.isPersistenceRequired(), "the pig keeps the persistence flag of the wheel");
                area.finish(pig);
            }, pig);
        }, pig);
    }

    /**
     * Two wheels do not share a mob: a mob that moves on to the next wheel is claimed by the first one for a moment (one mob feeds one
     * wheel) and is captured by the next wheel when the claim has lapsed.
     *
     * @param helper the game test helper
     */
    private static void claims(GameTestHelper helper) {
        var area = Area.create(helper, 5);
        var first = area.wheel(1, 3, 4);
        var second = area.wheel(5, 6, 4);
        var level = area.level();
        var pig = area.spawn(EntityType.PIG, 3.5F, 8.5F);
        area.after(20, () -> {
            settled(helper, pig);
            area.place(pig, 3.5, 4.5);
            first.scan(level);
            helper.assertTrue(first.heldMob() == pig, "the first wheel does not hold the pig");
            area.place(pig, 6.5, 4.5);
            first.scan(level);
            helper.assertTrue(first.runners() == 0 && first.heldMob() == null, "the first wheel still holds a pig that was carried away");
            second.scan(level);
            helper.assertTrue(second.runners() == 0, "a pig the first wheel just held is taken by the second one as well");
            area.after((int) WheelClaims.LAPSE_TICKS + 1, () -> {
                area.place(pig, 6.5, 4.5);
                second.scan(level);
                helper.assertTrue(second.heldMob() == pig && second.output() > 0, "the pig is not held by the second wheel after the claim lapsed");
                first.scan(level);
                helper.assertTrue(first.runners() == 0, "the first wheel counts a pig that left it");
                area.finish(pig);
            }, pig);
        }, pig);
    }

    /**
     * The buffer of a wheel cannot be charged but drained, a wheel pushes into a neighbour that takes FE (the power drain), a solar
     * panel next to a wheel does not become part of a panel group, and a tier 5 wheel coasts when the mob leaves the plate.
     *
     * @param helper the game test helper
     */
    private static void energy(GameTestHelper helper) {
        var area = Area.create(helper, 6);
        var drained = area.wheel(5, 3, 4);
        area.set(4, FLOOR + 1, 4, EnergyBlocks.POWER_DRAIN.get());
        var lone = area.wheel(5, 11, 4);
        area.set(12, FLOOR + 1, 4, EnergyBlocks.SOLAR.block(1).get());
        var level = area.level();
        var pigA = area.spawn(EntityType.PIG, 1.5F, 4.5F);
        var pigB = area.spawn(EntityType.PIG, 9.5F, 4.5F);
        area.after(20, () -> {
            settled(helper, pigA, pigB);
            area.place(pigA, 3.5, 4.5);
            area.place(pigB, 11.5, 4.5);
            wantToWalk(pigA);
            wantToWalk(pigB);
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
