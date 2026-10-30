package net.samtrion.dotcraft.energy.flow;

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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.TestSupport;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;

/**
 * GameTests of the flow generators, run with {@code ./gradlew :mods:energy:runGameTestServer}. The pools are glass pools, open at the top,
 * and every test builds at an own height, so water and animals never reach a neighbouring test.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class FlowGameTests {
    private static final Identifier REGISTRY = id("flow_registry");
    private static final Identifier WATER = id("flow_water");
    private static final Identifier FAUNA = id("flow_fauna");
    private static final Identifier AQUARIUM = id("flow_aquarium");
    private static final Identifier BUCKETED = id("flow_bucketed");
    private static final Identifier PUSH = id("flow_push");
    private static final Identifier SEALED = id("flow_sealed");
    private static final Identifier DRAIN = id("flow_drain");
    private static final Identifier FACING = id("flow_facing");
    private static final Identifier AXIS = id("flow_axis");

    private FlowGameTests() {}

    /** The id of a test in this mod's namespace. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, path);
    }

    /** The registry key of the function of a test. */
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
            registry.register(REGISTRY, TestSupport.ticking(FlowGameTests::registryTest));
            registry.register(WATER, TestSupport.ticking(FlowGameTests::water));
            registry.register(FAUNA, TestSupport.ticking(FlowGameTests::fauna));
            registry.register(AQUARIUM, TestSupport.ticking(FlowGameTests::aquarium));
            registry.register(BUCKETED, TestSupport.ticking(FlowGameTests::bucketed));
            registry.register(PUSH, TestSupport.ticking(FlowGameTests::push));
            registry.register(SEALED, TestSupport.ticking(FlowGameTests::sealed));
            registry.register(DRAIN, TestSupport.ticking(FlowGameTests::drain));
            registry.register(FACING, TestSupport.ticking(FlowGameTests::facing));
            registry.register(AXIS, TestSupport.ticking(FlowGameTests::axis));
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        register(event, REGISTRY, 30);
        register(event, WATER, 40);
        register(event, FAUNA, 80);
        register(event, AQUARIUM, 80);
        register(event, BUCKETED, 20);
        register(event, PUSH, 60);
        register(event, SEALED, 60);
        register(event, DRAIN, 40);
        register(event, FACING, 20);
        register(event, AXIS, 80);
    }

    /** Registers one test with an own environment and the empty template. */
    private static void register(RegisterGameTestsEvent event, Identifier id, int maxTicks) {
        var environment = event.registerEnvironment(id, new TestEnvironmentDefinition.AllOf());
        event.registerTest(id, new FunctionGameTestInstance(function(id),
                new TestData<>(environment, TestSupport.STRUCTURE, maxTicks, 0, true)));
    }

    /**
     * Builds a pool: a glass floor and walls, open at the top, whose inside, 5 x 5 x 5 blocks from relative x/z 1 and y
     * {@code y0 + 1}, is full of source water. It is open so it never shades a neighbouring test, and only 7 blocks wide so it
     * stays above its own test.
     */
    private static void pool(GameTestHelper helper, int y0) {
        for (int x = 0; x <= 6; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = 0; z <= 6; z++) {
                    boolean shell = x == 0 || x == 6 || y == 0 || z == 0 || z == 6;
                    helper.setBlock(new BlockPos(x, y0 + y, z), shell ? Blocks.GLASS : Blocks.WATER);
                }
            }
        }
    }

    /** The state of a generator that is waterlogged, as if it stood in water. */
    private static BlockState wet(int tier) {
        return FlowBlocks.block(tier).get().defaultBlockState().setValue(FlowGeneratorBlock.WATERLOGGED, true);
    }

    /** The block entity of a generator at a relative position; fails the test if there is none. */
    private static FlowGeneratorBlockEntity entity(GameTestHelper helper, BlockPos relative) {
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(be instanceof FlowGeneratorBlockEntity, "no flow generator at " + relative);
        return (FlowGeneratorBlockEntity) be;
    }

    /** The FE per tick a generator of the tier must make for a reading, from the math. */
    private static double expected(int tier, FlowScan.Reading reading) {
        return reading.output(tier, true) * EnergyCommonConfig.FLOW_GENERATORS.get(tier - 1).peak();
    }

    /** Lists the living entities near a position with their position and state, for the message of a failed assertion. */
    private static String dump(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(relative)).inflate(12), e -> true)
                .stream().map(e -> e.getType() + "@" + e.position() + " water=" + e.isInWater() + " alive=" + e.isAlive()).toList().toString();
    }

    /** The scan of the surroundings of a generator at a relative position. */
    private static FlowScan.Reading scan(GameTestHelper helper, BlockPos relative, int tier) {
        return FlowScan.scan(helper.getLevel(), helper.absolutePos(relative), tier);
    }

    /**
     * Every tier is a block with a block entity and an FE capability on every side but front and back (the flow axis) that cannot be charged and has the configured
     * capacity, a waterlogged state holds water, and a flow generator next to a solar panel does not join its pool.
     *
     * @param helper the game test helper
     */
    private static void registryTest(GameTestHelper helper) {
        var level = helper.getLevel();
        var blocks = FlowBlocks.blocks().toList();
        helper.assertTrue(blocks.size() == 5, "expected 5 flow generators, found " + blocks.size());
        int y = 70;
        int x = 0;
        for (var block : blocks) {
            var pos = new BlockPos(x++, y, 0);
            helper.setBlock(pos, block.get());
            helper.assertTrue(!((Object) block.get() instanceof GeneratorBlock), block.getId() + " is a panel");
            var abs = helper.absolutePos(pos);
            helper.assertTrue(level.getBlockEntity(abs) instanceof FlowGeneratorBlockEntity, block.getId() + " has no block entity");
            helper.assertTrue(!level.getBlockState(abs).getFluidState().is(FluidTags.WATER), "a dry generator holds water");
            var config = EnergyCommonConfig.FLOW_GENERATORS.get(block.get().tier() - 1);
            for (Direction side : Direction.values()) {
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, abs, side);
                if (side.getAxis() == level.getBlockState(abs).getValue(FlowGeneratorBlock.FACING).getAxis()) {
                    helper.assertTrue(handler == null, block.getId() + " has a capability on the flow axis (" + side + ")");
                    continue;
                }
                helper.assertTrue(handler != null, block.getId() + " has no capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, block.getId() + " accepted energy");
                }
                helper.assertTrue(handler.getCapacityAsInt() == config.capacity(), block.getId() + " has the wrong capacity");
            }
            helper.assertTrue(wet(block.get().tier()).getFluidState().is(FluidTags.WATER), "a waterlogged generator holds no water");
        }
        // next to a solar panel: no group, so the panel's pool is its own buffer only
        var solar = EnergyBlocks.SOLAR.block(1).get();
        helper.setBlock(new BlockPos(0, y, 3), solar);
        helper.setBlock(new BlockPos(1, y, 3), blocks.get(0).get());
        var panel = helper.absolutePos(new BlockPos(0, y, 3));
        helper.assertTrue(!level.getBlockState(panel).getValue(GeneratorBlock.EAST), "the panel joined the flow generator");
        EnergyHandler panelPool = level.getCapability(Capabilities.Energy.BLOCK, panel, Direction.DOWN);
        helper.assertTrue(panelPool != null && panelPool.getCapacityAsInt() == EnergyCommonConfig.SOLAR_PANELS.get(0).capacity(),
                "the panel's pool contains the flow generator");
        helper.succeed();
    }

    /**
     * A generator that is not waterlogged makes nothing; one waterlogged in a pool makes exactly what the scan and the math say,
     * buffers it and shows it on the comparator; less water makes less; flowing water is counted as such.
     *
     * @param helper the game test helper
     */
    private static void water(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 30;
        pool(helper, y0);
        var center = new BlockPos(3, y0 + 3, 3);
        helper.setBlock(center, wet(1));
        var dryPos = new BlockPos(3, y0 + 8, 3);
        helper.setBlock(dryPos, FlowBlocks.block(2).get());
        helper.runAfterDelay(5, () -> {
            var be = entity(helper, center);
            var reading = scan(helper, center, 1);
            helper.assertTrue(reading.sources() == FlowMath.CUBE_BLOCKS && reading.flowing() == 0,
                    "a full pool must read 124 sources: " + reading);
            helper.assertTrue(reading.faunaTypes() == 0, "no animals yet: " + reading);
            double output = expected(1, reading);
            helper.assertTrue(output > 0 && Math.abs(be.output() - output) < 1e-9, "wet output " + be.output() + ", expected " + output);
            helper.assertTrue(be.energy().getAmountAsLong() > 0, "a wet generator buffered no FE");
            var abs = helper.absolutePos(center);
            int signal = level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH);
            helper.assertTrue(signal >= 1 && signal == FlowMath.signal(output, EnergyCommonConfig.FLOW_GENERATORS.get(0).peak()), "wrong comparator signal " + signal);

            var dry = entity(helper, dryPos);
            dry.recompute(level);
            helper.assertTrue(dry.output() == 0 && dry.energy().getAmountAsLong() == 0, "a dry generator makes " + dry.output());

            // another generator next to it is no water, so a packed array of pods is no body of water either
            var neighbour = new BlockPos(4, y0 + 3, 3);
            helper.setBlock(neighbour, wet(1));
            helper.assertTrue(scan(helper, center, 1).sources() == FlowMath.CUBE_BLOCKS - 1, "a neighbouring generator counted as water");
            helper.setBlock(neighbour, Blocks.WATER);

            // two layers less water: less output
            for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) for (int y = 1; y <= 2; y++) helper.setBlock(new BlockPos(x, y0 + y, z), Blocks.GLASS);
            be.recompute(level);
            var less = scan(helper, center, 1);
            helper.assertTrue(less.sources() == FlowMath.CUBE_BLOCKS - 50, "wrong water count " + less);
            helper.assertTrue(be.output() < output && Math.abs(be.output() - expected(1, less)) < 1e-9,
                    "less water must make less: " + be.output() + " vs " + output);

            // a cube of flowing water counts as flowing (read in the same tick, before the water settles)
            var flowing = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
            for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) for (int y = 3; y <= 5; y++) {
                if (x != 3 || z != 3 || y != 3) helper.setBlock(new BlockPos(x, y0 + y, z), flowing);
            }
            var stream = scan(helper, center, 1);
            helper.assertTrue(stream.flowing() == 74 && stream.sources() == 0, "wrong flowing count " + stream);
            helper.succeed();
        });
    }

    /**
     * Animals raise the output by their distinct types: a wild and a bucketed cod are one type, the search radius of tier 1 (3
     * blocks) is shorter than that of tier 2 (6 blocks), and a dead animal does not count.
     *
     * @param helper the game test helper
     */
    private static void fauna(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 40;
        pool(helper, y0);
        var near = new BlockPos(1, y0 + 3, 1);
        var wide = new BlockPos(1, y0 + 3, 5);
        helper.setBlock(near, wet(1));
        helper.setBlock(wide, wet(2));
        // the glass wall in front of or behind a generator would block its flow axis: swap it for water held back by a glass pane further out
        helper.setBlock(new BlockPos(1, y0 + 3, 0), Blocks.WATER);
        helper.setBlock(new BlockPos(1, y0 + 3, -1), Blocks.GLASS);
        helper.setBlock(new BlockPos(1, y0 + 3, 6), Blocks.WATER);
        helper.setBlock(new BlockPos(1, y0 + 3, 7), Blocks.GLASS);
        // the cod are 1-2 blocks from tier 1, the salmon and the squid 4 blocks: out of its reach, in reach of tier 2
        helper.spawnWithNoFreeWill(EntityType.COD, 2.5F, y0 + 3.5F, 1.5F);
        var bucketed = helper.spawnWithNoFreeWill(EntityType.COD, 2.5F, y0 + 4.5F, 2.5F);
        bucketed.setFromBucket(true);
        var salmon = helper.spawnWithNoFreeWill(EntityType.SALMON, 5.5F, y0 + 3.5F, 1.5F);
        helper.spawnWithNoFreeWill(EntityType.SQUID, 5.5F, y0 + 2.5F, 2.5F);
        helper.runAfterDelay(10, () -> {
            var nearBe = entity(helper, near);
            var wideBe = entity(helper, wide);
            nearBe.recompute(level);
            wideBe.recompute(level);
            var nearReading = scan(helper, near, 1);
            var wideReading = scan(helper, wide, 2);
            helper.assertTrue(nearReading.faunaTypes() == 1, "tier 1 counts the two cod as one type and does not reach the others: " + nearReading + dump(helper, near));
            helper.assertTrue(wideReading.faunaTypes() == 3, "tier 2 reaches cod, salmon and squid: " + wideReading + dump(helper, wide));
            double noFauna = FlowMath.output(1, true, nearReading.sources(), nearReading.flowing(), nearReading.primeBiome(), 0)
                    * EnergyCommonConfig.FLOW_GENERATORS.get(0).peak();
            helper.assertTrue(Math.abs(nearBe.output() - expected(1, nearReading)) < 1e-9, "tier 1 output " + nearBe.output());
            helper.assertTrue(Math.abs(nearBe.output() - noFauna * (1 + FlowMath.FAUNA_STEP)) < 1e-3, "one type must add one step: " + nearBe.output() + " vs " + noFauna);
            helper.assertTrue(Math.abs(wideBe.output() - expected(2, wideReading)) < 1e-9, "tier 2 output " + wideBe.output());
            salmon.kill(level);
            helper.assertTrue(scan(helper, wide, 2).faunaTypes() == 2, "a dead salmon still counts");
            helper.succeed();
        });
    }

    /**
     * Stuffing a tank with many types does not help beyond the cap: six types give the bonus of five, at most 30 percent.
     *
     * @param helper the game test helper
     */
    private static void aquarium(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 50;
        pool(helper, y0);
        var center = new BlockPos(3, y0 + 3, 3);
        helper.setBlock(center, wet(3));
        helper.spawnWithNoFreeWill(EntityType.COD, 1.5F, y0 + 1.5F, 1.5F);
        helper.spawnWithNoFreeWill(EntityType.SALMON, 5.5F, y0 + 1.5F, 1.5F);
        helper.spawnWithNoFreeWill(EntityType.PUFFERFISH, 1.5F, y0 + 1.5F, 5.5F);
        helper.spawnWithNoFreeWill(EntityType.TROPICAL_FISH, 5.5F, y0 + 1.5F, 5.5F);
        helper.spawnWithNoFreeWill(EntityType.SQUID, 1.5F, y0 + 4.5F, 3.5F);
        helper.spawnWithNoFreeWill(EntityType.AXOLOTL, 5.5F, y0 + 4.5F, 3.5F);
        helper.runAfterDelay(10, () -> {
            var be = entity(helper, center);
            be.recompute(level);
            var reading = scan(helper, center, 3);
            helper.assertTrue(reading.faunaTypes() == 6, "expected six distinct types: " + reading + dump(helper, center));
            double base = FlowMath.output(3, true, reading.sources(), reading.flowing(), reading.primeBiome(), 0)
                    * EnergyCommonConfig.FLOW_GENERATORS.get(2).peak();
            helper.assertTrue(Math.abs(be.output() - base * (1 + FlowMath.FAUNA_MAX)) < 1e-3, "the bonus must be capped at 30 percent: " + be.output() + " vs " + base);
            helper.succeed();
        });
    }

    /**
     * Bucketed animals persist (they never despawn), wild ones may; the fauna scan counts both, so a tank of bucketed fish is a
     * lasting bonus.
     *
     * @param helper the game test helper
     */
    private static void bucketed(GameTestHelper helper) {
        Cod wild = helper.spawnWithNoFreeWill(EntityType.COD, 1.5F, 70.5F, 1.5F);
        Cod bucketed = helper.spawnWithNoFreeWill(EntityType.COD, 2.5F, 70.5F, 1.5F);
        bucketed.setFromBucket(true);
        Axolotl axolotl = helper.spawnWithNoFreeWill(EntityType.AXOLOTL, 3.5F, 70.5F, 1.5F);
        axolotl.setFromBucket(true);
        helper.assertTrue(wild.removeWhenFarAway(1e12) && !wild.requiresCustomPersistence(), "a wild cod does not despawn");
        helper.assertTrue(!bucketed.removeWhenFarAway(1e12) && bucketed.requiresCustomPersistence(), "a bucketed cod may despawn");
        helper.assertTrue(!axolotl.removeWhenFarAway(1e12) && axolotl.requiresCustomPersistence(), "a bucketed axolotl may despawn");
        helper.assertTrue(((Bucketable) bucketed).fromBucket() && ((Bucketable) axolotl).fromBucket(), "the bucket flag was lost");
        helper.assertTrue(bucketed.typeHolder().is(FlowTags.FAUNA) && wild.typeHolder().is(FlowTags.FAUNA) && axolotl.typeHolder().is(FlowTags.FAUNA),
                "the animals are not in the fauna tag");
        helper.succeed();
    }

    /**
     * A wet generator hands its FE to an adjacent acceptor (a power drain), so its buffer stays empty while it makes FE.
     *
     * @param helper the game test helper
     */
    private static void push(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 60;
        pool(helper, y0);
        var center = new BlockPos(3, y0 + 3, 3);
        helper.setBlock(center, wet(1));
        helper.setBlock(new BlockPos(4, y0 + 3, 3), EnergyBlocks.POWER_DRAIN.get());
        helper.runAfterDelay(10, () -> {
            var be = entity(helper, center);
            be.recompute(level);
            helper.assertTrue(be.output() > 0, "the generator makes nothing");
            helper.assertTrue(be.energy().getAmountAsLong() <= Math.ceil(be.output()), "the FE was not pushed away: " + be.energy().getAmountAsLong());
            helper.succeed();
        });
    }

    /**
     * A generator is a sealed pod: one bucket of water in dry air, and a change of a neighbour, make no current around it, so
     * even a tier 5 that is independent of the biome reads no water and makes nothing.
     *
     * @param helper the game test helper
     */
    private static void sealed(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 90;
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, y0, z), Blocks.GLASS);
        var center = new BlockPos(3, y0 + 1, 3);
        var block = FlowBlocks.block(5).get();
        helper.setBlock(center, block);
        var abs = helper.absolutePos(center);
        helper.assertTrue(block.placeLiquid(level, abs, level.getBlockState(abs), Fluids.WATER.getSource(false)), "the bucket was not accepted");
        helper.assertTrue(level.getBlockState(abs).getValue(FlowGeneratorBlock.WATERLOGGED), "the pod is not waterlogged");
        helper.setBlock(new BlockPos(4, y0 + 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(4, y0 + 1, 3), Blocks.AIR);
        helper.runAfterDelay(40, () -> {
            var reading = scan(helper, center, 5);
            helper.assertTrue(reading.sources() == 0 && reading.flowing() == 0, "a bucket in a pod made a current: " + reading);
            helper.assertTrue(level.getBlockState(abs.east()).isAir() && level.getBlockState(abs.north()).isAir(), "the pod spilled water");
            var be = entity(helper, center);
            be.recompute(level);
            helper.assertTrue(be.output() == 0, "a pod in dry air makes " + be.output());
            helper.succeed();
        });
    }

    /**
     * A generator is placed facing the look direction of the player (the flow direction), and rotate and mirror turn that facing.
     *
     * @param helper the game test helper
     */
    private static void facing(GameTestHelper helper) {
        var block = FlowBlocks.block(1).get();
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var pos = helper.absolutePos(new BlockPos(1, 70, 1));
        for (var look : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            player.setYRot(look.toYRot());
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos.below(), false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
            var state = block.getStateForPlacement(context);
            helper.assertTrue(state != null && state.getValue(FlowGeneratorBlock.FACING) == look, "placed looking " + look + " but got " + state);
        }
        var north = block.defaultBlockState().setValue(FlowGeneratorBlock.FACING, Direction.NORTH);
        helper.assertTrue(north.rotate(Rotation.CLOCKWISE_90).getValue(FlowGeneratorBlock.FACING) == Direction.EAST, "rotate did not turn the facing");
        helper.assertTrue(north.mirror(Mirror.LEFT_RIGHT).getValue(FlowGeneratorBlock.FACING) == Direction.SOUTH, "mirror did not turn the facing");
        helper.assertTrue(north.mirror(Mirror.FRONT_BACK).getValue(FlowGeneratorBlock.FACING) == Direction.NORTH, "the other mirror changed an axis-symmetric facing");
        helper.succeed();
    }

    /**
     * The flow axis: a wet generator facing east pushes FE only to the neighbours at up, down, north and south, never to front or
     * back (east and west), and has a capability only on those four sides; a solid block at the front or at the back makes the output
     * and the comparator 0, while water, air and a replaceable block do not.
     *
     * @param helper the game test helper
     */
    private static void axis(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 130;
        pool(helper, y0);
        var center = new BlockPos(3, y0 + 3, 3);
        var east = new BlockPos(4, y0 + 3, 3);
        var west = new BlockPos(2, y0 + 3, 3);
        var abs = helper.absolutePos(center);
        helper.setBlock(center, wet(1).setValue(FlowGeneratorBlock.FACING, Direction.EAST));
        // drains (full blocks) at the front and the back: the output is 0, but the buffered FE must still not leave through them
        helper.setBlock(east, EnergyBlocks.POWER_DRAIN.get());
        helper.setBlock(west, EnergyBlocks.POWER_DRAIN.get());
        for (var side : Direction.values()) {
            boolean flow = side.getAxis() == Direction.Axis.X;
            helper.assertTrue((level.getCapability(Capabilities.Energy.BLOCK, abs, side) == null) == flow, "wrong capability on " + side);
        }
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, null) == null, "a capability without a side");
        entity(helper, center).fill(100);
        helper.runAfterDelay(10, () -> {
            var be = entity(helper, center);
            helper.assertTrue(be.output() == 0, "a drain in front and behind must block the axis");
            helper.assertTrue(be.energy().getAmountAsLong() == 100, "FE was pushed to the front or back: " + be.energy().getAmountAsLong());
            // a drain at the side takes it
            helper.setBlock(new BlockPos(3, y0 + 3, 4), EnergyBlocks.POWER_DRAIN.get());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(be.energy().getAmountAsLong() == 0, "FE was not pushed to the side: " + be.energy().getAmountAsLong());
                helper.setBlock(new BlockPos(3, y0 + 3, 4), Blocks.WATER);
                // blocked in front: output 0; then freed with each kind of passable block
                helper.setBlock(east, Blocks.STONE);
                be.recompute(level);
                helper.assertTrue(be.output() == 0, "stone in front still gives " + be.output());
                helper.assertTrue(level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH) == 0, "comparator is not 0 when blocked");
                helper.setBlock(east, Blocks.WATER);
                helper.setBlock(west, Blocks.WATER);
                be.recompute(level);
                helper.assertTrue(be.output() > 0, "water in front blocks");
                helper.setBlock(west, Blocks.STONE);
                be.recompute(level);
                helper.assertTrue(be.output() == 0, "stone behind still gives " + be.output());
                helper.setBlock(west, Blocks.SHORT_GRASS);
                be.recompute(level);
                helper.assertTrue(be.output() > 0, "a replaceable block behind blocks");
                helper.setBlock(west, Blocks.AIR);
                be.recompute(level);
                helper.assertTrue(be.output() > 0, "air behind blocks");
                // the same stone at the side (up, down, left, right) does not block
                helper.setBlock(new BlockPos(3, y0 + 3, 2), Blocks.STONE);
                be.recompute(level);
                helper.assertTrue(be.output() > 0, "a solid block at the side blocks");
                helper.succeed();
            });
        });
    }

    /**
     * Draining the pod (a bucket) stops the output and the comparator at once, not at the next scan, and filling it starts them again.
     *
     * @param helper the game test helper
     */
    private static void drain(GameTestHelper helper) {
        var level = helper.getLevel();
        int y0 = 120;
        pool(helper, y0);
        var center = new BlockPos(3, y0 + 3, 3);
        helper.setBlock(center, wet(2));
        var abs = helper.absolutePos(center);
        helper.runAfterDelay(5, () -> {
            var be = entity(helper, center);
            helper.assertTrue(be.output() > 0, "a wet pod makes nothing");
            helper.setBlock(center, FlowBlocks.block(2).get());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(be.output() == 0, "a drained pod still makes " + be.output());
                helper.assertTrue(level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH) == 0, "the comparator is stale");
                helper.setBlock(center, wet(2));
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(be.output() > 0, "a refilled pod makes nothing");
                    helper.succeed();
                });
            });
        });
    }
}
