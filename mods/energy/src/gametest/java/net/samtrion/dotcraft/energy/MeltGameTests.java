package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ProblemReporter;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;
import net.samtrion.dotcraft.energy.melt.MeltBlock;
import net.samtrion.dotcraft.energy.melt.MeltBlockEntity;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.energy.melt.MeltIce;
import net.samtrion.dotcraft.energy.melt.MeltMath;

/**
 * GameTests of the melt generators, run with {@code ./gradlew :mods:energy:runGameTestServer}. Every test gets its own structure,
 * so the generators of different tests cannot touch; within a test they stand four blocks apart.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class MeltGameTests {
    private static final Identifier MELT = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT);

    private static final Identifier MELT_TIERS = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt_tiers");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_TIERS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT_TIERS);

    private static final Identifier MELT_ALONE = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt_alone");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_ALONE_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT_ALONE);

    private static final Identifier MELT_LIMITS = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt_limits");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_LIMITS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT_LIMITS);

    private static final Identifier MELT_RECOVERY = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt_recovery");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_RECOVERY_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT_RECOVERY);

    private static final Identifier MELT_TANK = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "melt_tank");
    private static final ResourceKey<Consumer<GameTestHelper>> MELT_TANK_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MELT_TANK);

    /** The height the generators stand at, clear of the floor of the structure. */
    private static final int Y = 2;
    /** Ticks to wait: the first pulse of any tier comes within 20 ticks. */
    private static final int WAIT = 45;

    /** Not instantiable. */
    private MeltGameTests() {}

    /**
     * Wraps a test so that it first clears the space it uses: the batches of the tests start at the same places one after the other, so
     * the heat, ice and water an earlier test left there would change the result.
     */
    private static Consumer<GameTestHelper> clean(Consumer<GameTestHelper> test) {
        return helper -> {
            for (int x = 0; x <= 13; x++) {
                for (int y = Y - 1; y <= Y + 2; y++) {
                    for (int z = 0; z <= 14; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
            test.accept(helper);
        };
    }

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(MELT, TestSupport.ticking(clean(MeltGameTests::melt)));
            registry.register(MELT_TIERS, TestSupport.ticking(clean(MeltGameTests::tiers)));
            registry.register(MELT_ALONE, TestSupport.ticking(clean(MeltGameTests::alone)));
            registry.register(MELT_LIMITS, TestSupport.ticking(clean(MeltGameTests::limits)));
            registry.register(MELT_RECOVERY, TestSupport.ticking(clean(MeltGameTests::recovery)));
            registry.register(MELT_TANK, TestSupport.ticking(clean(MeltGameTests::tank)));
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var melt = event.registerEnvironment(MELT, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT, new FunctionGameTestInstance(MELT_FUNCTION, new TestData<>(melt, TestSupport.STRUCTURE, 70, 0, true)));
        var tiers = event.registerEnvironment(MELT_TIERS, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT_TIERS, new FunctionGameTestInstance(MELT_TIERS_FUNCTION, new TestData<>(tiers, TestSupport.STRUCTURE, 70, 0, true)));
        var alone = event.registerEnvironment(MELT_ALONE, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT_ALONE, new FunctionGameTestInstance(MELT_ALONE_FUNCTION, new TestData<>(alone, TestSupport.STRUCTURE, 20, 0, true)));
        var limits = event.registerEnvironment(MELT_LIMITS, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT_LIMITS, new FunctionGameTestInstance(MELT_LIMITS_FUNCTION, new TestData<>(limits, TestSupport.STRUCTURE, 100, 0, true)));
        var recovery = event.registerEnvironment(MELT_RECOVERY, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT_RECOVERY, new FunctionGameTestInstance(MELT_RECOVERY_FUNCTION, new TestData<>(recovery, TestSupport.STRUCTURE, 70, 0, true)));
        var tank = event.registerEnvironment(MELT_TANK, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MELT_TANK, new FunctionGameTestInstance(MELT_TANK_FUNCTION, new TestData<>(tank, TestSupport.STRUCTURE, 100, 0, true)));
    }

    /**
     * Tier 1 with magma on one side and ice on the other: the ice is consumed, the FE appears (reservoir plus buffer is exactly the
     * FE of the ice, and the buffer never gets more than the peak per tick), nothing can be inserted from any side and FE can be
     * drained. Without heat, ice stays; a blue ice next to a heated generator gives the FE of blue ice, 81 times the ice.
     *
     * @param helper the game test helper
     */
    private static void melt(GameTestHelper helper) {
        var level = helper.getLevel();
        var tier1 = MeltBlocks.block(1).get();
        var heated = new BlockPos(1, Y, 1);
        var cold = new BlockPos(5, Y, 1);
        var blue = new BlockPos(9, Y, 1);
        for (var pos : new BlockPos[] {heated, cold, blue}) {
            helper.setBlock(pos, tier1);
            helper.setBlock(pos.east(), Blocks.ICE);
        }
        helper.setBlock(heated.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(blue.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(blue.east(), Blocks.BLUE_ICE);
        var lastStored = new int[] {0};
        helper.onEachTick(() -> {
            if (!(level.getBlockEntity(helper.absolutePos(heated)) instanceof MeltBlockEntity be)) return;
            int delta = be.stored() - lastStored[0];
            lastStored[0] = be.stored();
            helper.assertTrue(delta <= EnergyCommonConfig.MELT_GENERATORS.get(0).peak(), "more than the peak in one tick: " + delta);
        });
        helper.runAfterDelay(WAIT, () -> {
            var peak = EnergyCommonConfig.MELT_GENERATORS.get(0).peak();
            helper.assertTrue(helper.getBlockState(heated.east()).isAir(), "the ice next to the heated generator was not consumed");
            var be = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(heated));
            helper.assertTrue(be.stored() > 0, "the heated generator made no FE");
            helper.assertTrue(be.stored() + be.pending() == MeltIce.ICE.yield(), "FE lost or made up: " + be.stored() + " + " + be.pending());
            helper.assertTrue(be.stored() <= (long) peak * WAIT, "more than the peak in all: " + be.stored());
            helper.assertTrue(be.signal() > 0, "no comparator signal");
            // the single ice block (1000 FE) is released in about 34 ticks at the tier 1 peak, so the glow is read at the blue ice generator, which still holds heat
            helper.assertTrue(helper.getBlockState(blue).getValue(MeltBlock.LIT), "the generator does not glow");
            for (Direction side : Direction.values()) {
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(heated), side);
                helper.assertTrue(handler != null, "no capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, "the generator accepted energy on " + side);
                }
            }
            EnergyHandler out = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(heated), Direction.DOWN);
            try (var tx = Transaction.openRoot()) {
                helper.assertTrue(out.extract(Integer.MAX_VALUE, tx) > 0, "the generator cannot be drained");
            }

            helper.assertTrue(helper.getBlockState(cold.east()).is(Blocks.ICE), "ice melted without a heat source");
            helper.assertTrue(((MeltBlockEntity) level.getBlockEntity(helper.absolutePos(cold))).pending() == 0, "a cold generator holds FE");

            helper.assertTrue(helper.getBlockState(blue.east()).isAir(), "the blue ice was not consumed");
            var blueBe = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(blue));
            helper.assertTrue(blueBe.stored() + blueBe.pending() == MeltIce.BLUE_ICE.yield(), "wrong FE for the blue ice: " + blueBe.stored() + " + " + blueBe.pending());
            helper.succeed();
        });
    }

    /**
     * Tier 2 melts two ice blocks at once (its slots), tier 4 puts a bucket of water into its tank and leaves air, tier 1 gives no water; frosted ice
     * and snow are no fuel.
     *
     * @param helper the game test helper
     */
    private static void tiers(GameTestHelper helper) {
        var two = new BlockPos(1, Y, 1);
        var water = new BlockPos(6, Y, 1);
        var air = new BlockPos(11, Y, 1);
        helper.setBlock(two, MeltBlocks.block(2).get());
        helper.setBlock(two.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(two.east(), Blocks.ICE);
        helper.setBlock(two.above(), Blocks.ICE);
        helper.setBlock(water, MeltBlocks.block(4).get());
        helper.setBlock(water.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(water.east(), Blocks.ICE);
        helper.setBlock(air, MeltBlocks.block(1).get());
        helper.setBlock(air.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(air.east(), Blocks.ICE);
        helper.assertTrue(MeltIce.of(Blocks.FROSTED_ICE.defaultBlockState()) == null, "frosted ice is fuel");
        helper.assertTrue(MeltIce.of(Blocks.SNOW_BLOCK.defaultBlockState()) == null, "snow is fuel");
        helper.assertTrue(MeltIce.of(Blocks.PACKED_ICE.defaultBlockState()) == MeltIce.PACKED_ICE, "packed ice is no packed ice");
        helper.runAfterDelay(WAIT, () -> {
            helper.assertTrue(helper.getBlockState(two.east()).isAir() && helper.getBlockState(two.above()).isAir(), "tier 2 did not melt both blocks");
            var be = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(two));
            helper.assertTrue(be.stored() + be.pending() == 2L * MeltIce.ICE.yield(), "wrong FE for two ice blocks: " + be.stored() + " + " + be.pending());
            helper.assertTrue(helper.getBlockState(water.east()).isAir(), "tier 4 placed a block instead of filling its tank");
            var tank4 = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(water));
            helper.assertTrue(tank4.water() == MeltMath.BUCKET_MB, "tier 4 tank holds " + tank4.water() + " mB, expected one bucket");
            helper.assertTrue(helper.getBlockState(air.east()).isAir(), "tier 1 left something but air");
            var tank1 = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(air));
            helper.assertTrue(tank1.water() == 0, "tier 1 holds water");
            helper.succeed();
        });
    }

    /**
     * Water recovery of a packed ice block: tier 4 puts one bucket into its tank, tier 5 all the water the block is worth, nine buckets
     * (the ice it is made of); no water block is placed in the world.
     *
     * @param helper the game test helper
     */
    private static void recovery(GameTestHelper helper) {
        var four = new BlockPos(6, Y, 2);
        var five = new BlockPos(6, Y, 10);
        helper.setBlock(four, MeltBlocks.block(4).get());
        helper.setBlock(five, MeltBlocks.block(5).get());
        for (var gen : new BlockPos[] {four, five}) {
            helper.setBlock(gen.west(), Blocks.MAGMA_BLOCK);
            helper.setBlock(gen.east(), Blocks.PACKED_ICE);
        }
        helper.runAfterDelay(WAIT, () -> {
            int expected = MeltIce.PACKED_ICE.yield() / MeltIce.ICE.yield();
            var fourBe = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(four));
            var fiveBe = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(five));
            helper.assertTrue(fourBe.water() == MeltMath.BUCKET_MB, "tier 4 tank holds " + fourBe.water() + " mB for packed ice, expected 1000");
            helper.assertTrue(fiveBe.water() == expected * MeltMath.BUCKET_MB, "tier 5 tank holds " + fiveBe.water() + " mB for packed ice, expected " + expected * MeltMath.BUCKET_MB);
            helper.assertTrue(sources(helper, 0, 14) == 0, "water was placed in the world");
            helper.succeed();
        });
    }

    /** Counts the water source blocks of the test between two z values (relative). */
    private static int sources(GameTestHelper helper, int fromZ, int toZ) {
        int count = 0;
        for (int x = 0; x <= 13; x++) {
            for (int y = Y - 4; y <= Y + 6; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).getFluidState().isSource()) count++;
                }
            }
        }
        return count;
    }

    /**
     * The water tank: blue ice at tier 5 is worth 81 buckets and fills it (32 buckets, the rest is lost), which cuts the FE of the next
     * ice to a quarter; the tank can be drained from every side but nothing can be inserted, and once there is room again the ice gives
     * its full FE; the water survives saving and loading.
     *
     * @param helper the game test helper
     */
    private static void tank(GameTestHelper helper) {
        var level = helper.getLevel();
        var gen = new BlockPos(1, Y, 1);
        var copy = new BlockPos(1, Y, 8);
        helper.setBlock(gen, MeltBlocks.block(5).get());
        helper.setBlock(gen.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(gen.above(), Blocks.BLUE_ICE);
        helper.setBlock(gen.east(), Blocks.ICE);
        helper.setBlock(copy, MeltBlocks.block(1).get());
        helper.runAfterDelay(25, () -> {
            var be = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(gen));
            helper.assertTrue(helper.getBlockState(gen.above()).isAir() && helper.getBlockState(gen.east()).isAir(), "the ice was not melted");
            helper.assertTrue(be.water() == MeltMath.TANK_CAPACITY_MB && be.tankFull(), "the tank is not full: " + be.water());
            long expected = MeltIce.BLUE_ICE.yield() + MeltMath.effectiveYield(MeltIce.ICE.yield(), true);
            helper.assertTrue(be.stored() + be.pending() == expected, "a full tank did not cut the ice to a quarter: " + be.stored() + " + " + be.pending() + " instead of " + expected);
            helper.assertTrue(MeltMath.effectiveYield(MeltIce.ICE.yield(), true) * 4 == MeltIce.ICE.yield(), "not a quarter");
            helper.assertTrue(sources(helper, 0, 14) == 0, "water was placed in the world");

            for (Direction side : Direction.values()) {
                var handler = level.getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(gen), side);
                helper.assertTrue(handler != null, "no fluid capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(FluidResource.of(Fluids.WATER), 1000, tx) == 0, "the tank accepted water on " + side);
                }
            }

            var saved = be.saveCustomOnly(level.registryAccess());
            var target = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(copy));
            target.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            helper.assertTrue(target.water() == be.water(), "save and load changed the water: " + target.water());

            var out = level.getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(gen), Direction.DOWN);
            try (var tx = Transaction.openRoot()) {
                helper.assertTrue(out.extract(FluidResource.of(Fluids.WATER), MeltMath.BUCKET_MB, tx) == MeltMath.BUCKET_MB, "the tank cannot be drained");
                tx.commit();
            }
            helper.assertTrue(be.water() == MeltMath.TANK_CAPACITY_MB - MeltMath.BUCKET_MB && !be.tankFull(), "draining did not free the tank");
            long before = be.stored() + be.pending();
            helper.setBlock(gen.east(), Blocks.ICE);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(helper.getBlockState(gen.east()).isAir(), "the ice was not melted after draining");
                long gained = be.stored() + be.pending() - before;
                helper.assertTrue(gained == MeltIce.ICE.yield(), "the ice gave " + gained + " FE after draining, expected the full " + MeltIce.ICE.yield());
                helper.assertTrue(be.tankFull(), "the tank was not refilled");
                helper.succeed();
            });
        });
    }

    /**
     * A melt generator is no panel: it does not join the pool of a solar panel next to it, nor does the panel show it as a neighbour.
     * Each keeps the capacity of its own config.
     *
     * @param helper the game test helper
     */
    private static void alone(GameTestHelper helper) {
        var level = helper.getLevel();
        var panel = new BlockPos(1, Y, 1);
        var melt = new BlockPos(2, Y, 1);
        helper.setBlock(panel, EnergyBlocks.SOLAR.block(1).get());
        helper.setBlock(melt, MeltBlocks.block(1).get());
        helper.setBlock(melt.east(), Blocks.MAGMA_BLOCK);
        helper.assertTrue(!(helper.getBlockState(melt).getBlock() instanceof GeneratorBlock), "a melt generator is a panel");
        helper.assertTrue(!helper.getBlockState(panel).getValue(GeneratorBlock.EAST), "the panel shows the melt generator as a neighbour");
        EnergyHandler pool = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(panel), Direction.DOWN);
        helper.assertTrue(pool != null && pool.getCapacityAsLong() == EnergyCommonConfig.SOLAR_PANELS.get(0).capacity(), "the pool of the panel holds the melt generator");
        EnergyHandler own = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(melt), Direction.DOWN);
        helper.assertTrue(own != null && own.getCapacityAsLong() == EnergyCommonConfig.MELT_GENERATORS.get(0).capacity(), "wrong capacity of the melt generator");
        helper.succeed();
    }

    /**
     * Tier 5 melts all five ice sides at once; a full reservoir stops melting (a blue ice burnt in tier 1 keeps the next block from
     * melting); the FE of a block entity survives saving and loading.
     *
     * @param helper the game test helper
     */
    private static void limits(GameTestHelper helper) {
        var level = helper.getLevel();
        var five = new BlockPos(1, Y, 1);
        var full = new BlockPos(1, Y, 5);
        var saved = new BlockPos(1, Y, 9);
        var copy = new BlockPos(1, Y, 13);
        helper.setBlock(five, MeltBlocks.block(5).get());
        helper.setBlock(five.west(), Blocks.MAGMA_BLOCK);
        for (var d : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST}) helper.setBlock(five.relative(d), Blocks.ICE);
        helper.setBlock(full, MeltBlocks.block(1).get());
        helper.setBlock(full.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(full.east(), Blocks.BLUE_ICE);
        helper.setBlock(full.above(), Blocks.PACKED_ICE); // drains over 300 ticks at peak 30, so the reservoir stays occupied for the whole test
        helper.setBlock(saved, MeltBlocks.block(1).get());
        helper.setBlock(saved.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(saved.east(), Blocks.PACKED_ICE);
        helper.setBlock(copy, MeltBlocks.block(1).get());
        helper.runAfterDelay(WAIT, () -> {
            for (var d : new Direction[] {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST}) {
                helper.assertTrue(helper.getBlockState(five.relative(d)).isAir(), "tier 5 did not melt " + d);
            }
            var fiveBe = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(five));
            helper.assertTrue(fiveBe.water() == 5 * MeltMath.BUCKET_MB, "the tank holds " + fiveBe.water() + " mB for five ice blocks");
            helper.assertTrue(fiveBe.stored() + fiveBe.pending() == 5L * MeltIce.ICE.yield(), "wrong FE for five ice blocks: " + fiveBe.stored() + " + " + fiveBe.pending());

            helper.assertTrue(helper.getBlockState(full.above()).isAir(), "the packed ice above was not consumed");
            helper.assertTrue(helper.getBlockState(full.east()).is(Blocks.BLUE_ICE), "a block melted into a full reservoir");

            var source = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(saved));
            helper.assertTrue(source.pending() > 0 && source.stored() > 0, "nothing to save");
            var tag = source.saveCustomOnly(level.registryAccess());
            var target = (MeltBlockEntity) level.getBlockEntity(helper.absolutePos(copy));
            target.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            helper.assertTrue(target.pending() == source.pending() && target.stored() == source.stored(),
                    "save and load changed the FE: " + target.pending() + "/" + target.stored() + " instead of " + source.pending() + "/" + source.stored());
            helper.succeed();
        });
    }
}
