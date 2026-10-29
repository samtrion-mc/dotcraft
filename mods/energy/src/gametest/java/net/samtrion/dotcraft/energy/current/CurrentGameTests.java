package net.samtrion.dotcraft.energy.current;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;

/**
 * GameTests of the current generators, run with {@code ./gradlew :mods:energy:runGameTestServer}. The pools are glass pools, open at the top,
 * and every test builds at an own height, so water and animals never reach a neighbouring test.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class CurrentGameTests {
    private static final Identifier REGISTRY = id("current_registry");
    private static final Identifier WATER = id("current_water");
    private static final Identifier FAUNA = id("current_fauna");
    private static final Identifier AQUARIUM = id("current_aquarium");
    private static final Identifier BUCKETED = id("current_bucketed");
    private static final Identifier PUSH = id("current_push");

    private CurrentGameTests() {}

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
            registry.register(REGISTRY, (Consumer<GameTestHelper>) CurrentGameTests::registryTest);
            registry.register(WATER, (Consumer<GameTestHelper>) CurrentGameTests::water);
            registry.register(FAUNA, (Consumer<GameTestHelper>) CurrentGameTests::fauna);
            registry.register(AQUARIUM, (Consumer<GameTestHelper>) CurrentGameTests::aquarium);
            registry.register(BUCKETED, (Consumer<GameTestHelper>) CurrentGameTests::bucketed);
            registry.register(PUSH, (Consumer<GameTestHelper>) CurrentGameTests::push);
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
    }

    private static void register(RegisterGameTestsEvent event, Identifier id, int maxTicks) {
        var environment = event.registerEnvironment(id, new TestEnvironmentDefinition.AllOf());
        event.registerTest(id, new FunctionGameTestInstance(function(id),
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), maxTicks, 0, true)));
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

    private static BlockState wet(int tier) {
        return CurrentBlocks.block(tier).get().defaultBlockState().setValue(CurrentGeneratorBlock.WATERLOGGED, true);
    }

    private static CurrentGeneratorBlockEntity entity(GameTestHelper helper, BlockPos relative) {
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(be instanceof CurrentGeneratorBlockEntity, "no current generator at " + relative);
        return (CurrentGeneratorBlockEntity) be;
    }

    /** The FE per tick a generator of the tier must make for a reading, from the math. */
    private static double expected(GameTestHelper helper, BlockPos relative, int tier, CurrentScan.Reading reading) {
        return reading.output(tier, true) * EnergyCommonConfig.CURRENT_GENERATORS.get(tier - 1).peak();
    }

    private static String dump(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(relative)).inflate(12), e -> true)
                .stream().map(e -> e.getType() + "@" + e.position() + " water=" + e.isInWater() + " alive=" + e.isAlive()).toList().toString();
    }

    private static CurrentScan.Reading scan(GameTestHelper helper, BlockPos relative, int tier) {
        return CurrentScan.scan(helper.getLevel(), helper.absolutePos(relative), tier);
    }

    /**
     * Every tier is a block with a block entity and an FE capability on every side that cannot be charged and has the configured
     * capacity, a waterlogged state holds water, and a current generator next to a solar panel does not join its pool.
     *
     * @param helper the game test helper
     */
    private static void registryTest(GameTestHelper helper) {
        var level = helper.getLevel();
        var blocks = CurrentBlocks.blocks().toList();
        helper.assertTrue(blocks.size() == 5, "expected 5 current generators, found " + blocks.size());
        int y = 70;
        int x = 0;
        for (var block : blocks) {
            var pos = new BlockPos(2 * x++, y, 0);
            helper.setBlock(pos, block.get());
            helper.assertTrue(!((Object) block.get() instanceof GeneratorBlock), block.getId() + " is a panel");
            var abs = helper.absolutePos(pos);
            helper.assertTrue(level.getBlockEntity(abs) instanceof CurrentGeneratorBlockEntity, block.getId() + " has no block entity");
            helper.assertTrue(!level.getBlockState(abs).getFluidState().is(FluidTags.WATER), "a dry generator holds water");
            var config = EnergyCommonConfig.CURRENT_GENERATORS.get(block.get().tier() - 1);
            for (Direction side : Direction.values()) {
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, abs, side);
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
        helper.setBlock(new BlockPos(20, y, 0), solar);
        helper.setBlock(new BlockPos(21, y, 0), blocks.get(0).get());
        var panel = helper.absolutePos(new BlockPos(20, y, 0));
        helper.assertTrue(!level.getBlockState(panel).getValue(GeneratorBlock.EAST), "the panel joined the current generator");
        EnergyHandler panelPool = level.getCapability(Capabilities.Energy.BLOCK, panel, Direction.DOWN);
        helper.assertTrue(panelPool != null && panelPool.getCapacityAsInt() == EnergyCommonConfig.SOLAR_PANELS.get(0).capacity(),
                "the panel's pool contains the current generator");
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
        helper.setBlock(dryPos, CurrentBlocks.block(2).get());
        helper.runAfterDelay(5, () -> {
            var be = entity(helper, center);
            var reading = scan(helper, center, 1);
            helper.assertTrue(reading.sources() == CurrentMath.CUBE_BLOCKS && reading.flowing() == 0,
                    "a full pool must read 124 sources: " + reading);
            helper.assertTrue(reading.faunaTypes() == 0, "no animals yet: " + reading);
            double output = expected(helper, center, 1, reading);
            helper.assertTrue(output > 0 && Math.abs(be.output() - output) < 1e-9, "wet output " + be.output() + ", expected " + output);
            helper.assertTrue(be.energy().getAmountAsLong() > 0, "a wet generator buffered no FE");
            var abs = helper.absolutePos(center);
            int signal = level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH);
            helper.assertTrue(signal >= 1 && signal == CurrentMath.signal(output, EnergyCommonConfig.CURRENT_GENERATORS.get(0).peak()), "wrong comparator signal " + signal);

            var dry = entity(helper, dryPos);
            dry.recompute(level);
            helper.assertTrue(dry.output() == 0 && dry.energy().getAmountAsLong() == 0, "a dry generator makes " + dry.output());

            // two layers less water: less output
            for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) for (int y = 1; y <= 2; y++) helper.setBlock(new BlockPos(x, y0 + y, z), Blocks.GLASS);
            be.recompute(level);
            var less = scan(helper, center, 1);
            helper.assertTrue(less.sources() == CurrentMath.CUBE_BLOCKS - 50, "wrong water count " + less);
            helper.assertTrue(be.output() < output && Math.abs(be.output() - expected(helper, center, 1, less)) < 1e-9,
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
            double noFauna = CurrentMath.output(1, true, nearReading.sources(), nearReading.flowing(), nearReading.primeBiome(), 0)
                    * EnergyCommonConfig.CURRENT_GENERATORS.get(0).peak();
            helper.assertTrue(Math.abs(nearBe.output() - expected(helper, near, 1, nearReading)) < 1e-9, "tier 1 output " + nearBe.output());
            helper.assertTrue(Math.abs(nearBe.output() - noFauna * (1 + CurrentMath.FAUNA_STEP)) < 1e-3, "one type must add one step: " + nearBe.output() + " vs " + noFauna);
            helper.assertTrue(Math.abs(wideBe.output() - expected(helper, wide, 2, wideReading)) < 1e-9, "tier 2 output " + wideBe.output());
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
            double base = CurrentMath.output(3, true, reading.sources(), reading.flowing(), reading.primeBiome(), 0)
                    * EnergyCommonConfig.CURRENT_GENERATORS.get(2).peak();
            helper.assertTrue(Math.abs(be.output() - base * (1 + CurrentMath.FAUNA_MAX)) < 1e-3, "the bonus must be capped at 30 percent: " + be.output() + " vs " + base);
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
        helper.assertTrue(bucketed.typeHolder().is(CurrentTags.FAUNA) && wild.typeHolder().is(CurrentTags.FAUNA) && axolotl.typeHolder().is(CurrentTags.FAUNA),
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
}
