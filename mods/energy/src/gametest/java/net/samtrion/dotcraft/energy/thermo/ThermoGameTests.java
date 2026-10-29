package net.samtrion.dotcraft.energy.thermo;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
import net.samtrion.dotcraft.energy.generator.GeneratorBlockEntity;

/**
 * GameTests of the thermoelectric generators, run with {@code ./gradlew :mods:energy:runGameTestServer}. The tests build
 * their cells high up ({@link #Y}) with the hot and the cold block walled in by stone, so lava and water cannot flow into the
 * neighbouring tests.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class ThermoGameTests {
    /** The height of the first cell; the cells of a test are {@link #SPACING} blocks apart. */
    private static final int Y = 8;
    /** The distance between two cells of a test: more than the three blocks a walled cell is high. */
    private static final int SPACING = 4;

    /** The test that checks the output of the tiers for several hot and cold blocks. */
    private static final Identifier OUTPUT = id("thermo_output");
    /** The test that checks buffering, pushing and the capability. */
    private static final Identifier ENERGY = id("thermo_energy");
    /** The test that checks that the generators stay out of the pool of the panels. */
    private static final Identifier NO_POOL = id("thermo_no_pool");
    /** The test that checks the heat and cold tags. */
    private static final Identifier TAGS = id("thermo_tags");

    /** Not instantiable. */
    private ThermoGameTests() {}

    /** The identifier of a test of this mod. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, path);
    }

    /** The key of the function of a test. */
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
            registry.register(OUTPUT, (Consumer<GameTestHelper>) ThermoGameTests::output);
            registry.register(ENERGY, (Consumer<GameTestHelper>) ThermoGameTests::energy);
            registry.register(NO_POOL, (Consumer<GameTestHelper>) ThermoGameTests::noPool);
            registry.register(TAGS, (Consumer<GameTestHelper>) ThermoGameTests::tags);
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (var test : List.of(OUTPUT, ENERGY, NO_POOL, TAGS)) {
            var environment = event.registerEnvironment(test, new TestEnvironmentDefinition.AllOf());
            event.registerTest(test, new FunctionGameTestInstance(function(test),
                    new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 80, 0, true)));
        }
    }

    /** Puts a block and walls it in with stone on every side but the one towards the generator. */
    private static void walled(GameTestHelper helper, BlockPos pos, BlockState state, Direction towardsGenerator) {
        helper.setBlock(pos, state);
        for (Direction d : Direction.values()) {
            if (d != towardsGenerator) helper.setBlock(pos.relative(d), Blocks.STONE);
        }
    }

    /**
     * Builds a cell at the given height: the generator at x 2, the hot block west and the cold block east of it, both walled in.
     *
     * @return the position of the generator, relative to the test
     */
    private static BlockPos cell(GameTestHelper helper, int index, int tier, @Nullable BlockState hot, @Nullable BlockState cold) {
        var gen = new BlockPos(2, Y + index * SPACING, 1);
        helper.setBlock(gen, ThermoBlocks.block(tier).get());
        if (hot != null) walled(helper, gen.west(), hot, Direction.EAST);
        if (cold != null) walled(helper, gen.east(), cold, Direction.WEST);
        return gen;
    }

    /** A scenario of {@link #output}: the tier, the blocks next to the generator and the levels they must give. */
    private record Scenario(int tier, @Nullable BlockState hot, @Nullable BlockState cold, int heat, int coldLevel) {}

    /**
     * Placed lava, magma, a lit campfire, water, packed ice and blue ice give the levels of the tags; the output is the peak of
     * the tier times the share of {@link ThermoMath}, with a comparator signal to match, and nothing at all with only a hot or only
     * a cold side or with an unlit campfire. The sources are still there afterwards: nothing is consumed.
     *
     * @param helper the game test helper
     */
    private static void output(GameTestHelper helper) {
        var scenarios = List.of(
                new Scenario(1, Blocks.LAVA.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(), 3, 3),
                new Scenario(1, Blocks.CAMPFIRE.defaultBlockState(), Blocks.WATER.defaultBlockState(), 1, 1),
                new Scenario(3, Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(), 2, 3),
                new Scenario(5, Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.WATER.defaultBlockState(), 2, 1),
                new Scenario(2, Blocks.LAVA.defaultBlockState(), null, 3, 0),
                new Scenario(2, null, Blocks.BLUE_ICE.defaultBlockState(), 0, 3),
                new Scenario(4, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), Blocks.BLUE_ICE.defaultBlockState(), 0, 3));
        var level = helper.getLevel();
        var generators = new BlockPos[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            var s = scenarios.get(i);
            generators[i] = cell(helper, i, s.tier(), s.hot(), s.cold());
        }
        helper.runAfterDelay(30, () -> {
            for (int i = 0; i < scenarios.size(); i++) {
                var s = scenarios.get(i);
                var abs = helper.absolutePos(generators[i]);
                var be = (ThermoBlockEntity) level.getBlockEntity(abs);
                helper.assertTrue(be != null, "no block entity in scenario " + i);
                // the unlit campfire is no heat source: the effective levels are the ones of the tags
                helper.assertTrue(be.heatLevel() == s.heat() && be.coldLevel() == s.coldLevel(),
                        "scenario " + i + ": heat " + be.heatLevel() + ", cold " + be.coldLevel() + ", expected " + s.heat() + ", " + s.coldLevel());
                float share = ThermoMath.share(s.tier(), s.heat(), s.coldLevel());
                double expected = EnergyCommonConfig.THERMO_GENERATORS.get(s.tier() - 1).peak() * (double) share;
                helper.assertTrue(Math.abs(be.output() - expected) < 1e-6, "scenario " + i + ": output " + be.output() + ", expected " + expected);
                int signal = level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH);
                helper.assertTrue(signal == ThermoMath.signal(share), "scenario " + i + ": comparator " + signal);
                if (s.hot() != null) helper.assertTrue(level.getBlockState(abs.west()).is(s.hot().getBlock()), "the hot block of scenario " + i + " was consumed");
                if (s.cold() != null) helper.assertTrue(level.getBlockState(abs.east()).is(s.cold().getBlock()), "the cold block of scenario " + i + " was consumed");
            }
            // the perk: the same weak sources give a higher tier a larger share of its (larger) peak
            helper.assertTrue(ThermoMath.share(5, 2, 1) > ThermoMath.share(1, 2, 1), "tier 5 does not get more out of magma and water");
            helper.succeed();
        });
    }

    /**
     * A generator buffers what it makes, can only be drained (on every side), is limited per operation by its config and gives
     * its FE to a power drain next to it, which leaves its buffer empty, while one with no neighbour that takes FE fills up.
     *
     * @param helper the game test helper
     */
    private static void energy(GameTestHelper helper) {
        var level = helper.getLevel();
        var lone = cell(helper, 0, 3, Blocks.LAVA.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState());
        var drained = cell(helper, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState());
        helper.setBlock(drained.north(), EnergyBlocks.POWER_DRAIN.get());
        var config = EnergyCommonConfig.THERMO_GENERATORS.get(2);
        helper.runAfterDelay(40, () -> {
            var loneAbs = helper.absolutePos(lone);
            var loneBe = (ThermoBlockEntity) level.getBlockEntity(loneAbs);
            var drainedBe = (ThermoBlockEntity) level.getBlockEntity(helper.absolutePos(drained));
            helper.assertTrue(loneBe.output() == config.peak(), "the lone generator does not make its peak: " + loneBe.output());
            helper.assertTrue(loneBe.energy().getAmountAsLong() > 0, "the lone generator buffered nothing");
            helper.assertTrue(loneBe.energy().getAmountAsLong() <= config.capacity(), "the buffer is larger than its capacity");
            helper.assertTrue(drainedBe.output() > 0 && drainedBe.energy().getAmountAsLong() == 0, "the generator next to the power drain kept " + drainedBe.energy().getAmountAsLong() + " FE");
            for (Direction side : Direction.values()) {
                var handler = level.getCapability(Capabilities.Energy.BLOCK, loneAbs, side);
                helper.assertTrue(handler != null, "no FE capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, "the generator takes FE on " + side);
                    int taken = handler.extract(Integer.MAX_VALUE, tx);
                    helper.assertTrue(taken > 0 && taken <= config.maxOutput(), "extraction on " + side + " gave " + taken + ", limit " + config.maxOutput());
                }
            }
            helper.succeed();
        });
    }

    /**
     * Thermoelectric generators are no panels: next to a solar panel they stay out of its pool (its capacity is its own), and the
     * block is neither a {@link GeneratorBlock} nor has a {@link GeneratorBlockEntity}.
     *
     * @param helper the game test helper
     */
    private static void noPool(GameTestHelper helper) {
        var level = helper.getLevel();
        var thermo = new BlockPos(1, Y, 1);
        var panel = new BlockPos(2, Y, 1);
        helper.setBlock(thermo, ThermoBlocks.block(1).get());
        helper.setBlock(panel, EnergyBlocks.SOLAR.block(1).get());
        helper.assertTrue(!(level.getBlockState(helper.absolutePos(thermo)).getBlock() instanceof GeneratorBlock), "a thermoelectric generator is a panel block");
        helper.assertTrue(!(level.getBlockEntity(helper.absolutePos(thermo)) instanceof GeneratorBlockEntity), "a thermoelectric generator has a panel block entity");
        var handler = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(panel), Direction.UP);
        helper.assertTrue(handler != null && handler.getCapacityAsLong() == EnergyCommonConfig.SOLAR_PANELS.get(0).capacity(),
                "the panel next to a thermoelectric generator has a pool of " + (handler == null ? -1 : handler.getCapacityAsLong()));
        helper.succeed();
    }

    /**
     * The heat and cold tags hold what the datagen wrote: levels of the common sources, nothing for stone, and a lit state that is
     * off makes a campfire cold.
     *
     * @param helper the game test helper
     */
    private static void tags(GameTestHelper helper) {
        check(helper, Blocks.LAVA, 3, 0);
        check(helper, Blocks.MAGMA_BLOCK, 2, 0);
        check(helper, Blocks.CAMPFIRE, 1, 0);
        check(helper, Blocks.WATER, 0, 1);
        check(helper, Blocks.SNOW_BLOCK, 0, 2);
        check(helper, Blocks.ICE, 0, 2);
        check(helper, Blocks.PACKED_ICE, 0, 3);
        check(helper, Blocks.BLUE_ICE, 0, 3);
        check(helper, Blocks.STONE, 0, 0);
        helper.assertTrue(ThermoTags.heat(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false)) == 0, "an unlit campfire is hot");
        helper.succeed();
    }

    /** Asserts the heat and cold level of a block. */
    private static void check(GameTestHelper helper, Block block, int heat, int cold) {
        var state = block.defaultBlockState();
        helper.assertTrue(ThermoTags.heat(state) == heat && ThermoTags.cold(state) == cold, block + ": heat " + ThermoTags.heat(state) + ", cold " + ThermoTags.cold(state));
    }
}
