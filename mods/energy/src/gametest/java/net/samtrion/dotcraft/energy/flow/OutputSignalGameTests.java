package net.samtrion.dotcraft.energy.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.TestSupport;
import net.samtrion.dotcraft.energy.melt.MeltBlockEntity;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;

/**
 * GameTest of the comparator updates of the flow and melt generators: a generator whose FE changes every tick must not tell its
 * neighbours about it every tick (that is a block update per generator per tick), only about a change of its comparator signal.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class OutputSignalGameTests {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "output_signal");
    private static final ResourceKey<Consumer<GameTestHelper>> FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, ID);
    private static final Identifier PROBE_ID = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "output_signal_probe");

    /** The ticks the generators run before the updates are counted. */
    private static final int WAIT = 45;
    /** The most updates one generator may send in {@link #WAIT} ticks: a few changes of its signal, never one per tick. */
    private static final int MAX_UPDATES = 5;

    /** The neighbour updates each probe received, by absolute position. */
    private static final Map<BlockPos, Integer> UPDATES = new HashMap<>();

    private OutputSignalGameTests() {}

    /** A block that counts the neighbour updates a block entity sends through {@code updateNeighbourForOutputSignal}. */
    private static final class Probe extends Block {
        /**
         * Creates the probe.
         *
         * @param properties the block properties
         */
        Probe(Properties properties) {
            super(properties);
        }

        @Override
        public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
            UPDATES.merge(pos.immutable(), 1, Integer::sum);
        }
    }

    /**
     * Registers the probe block and the test function.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, registry -> registry.register(PROBE_ID,
                new Probe(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, PROBE_ID)))));
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(ID, TestSupport.ticking(OutputSignalGameTests::test)));
    }

    /**
     * Registers the test instance.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(ID, new TestEnvironmentDefinition.AllOf());
        event.registerTest(ID, new FunctionGameTestInstance(FUNCTION, new TestData<>(environment, TestSupport.STRUCTURE, 70, 0, true)));
    }

    /**
     * A flow generator that is filled every tick and a heated melt generator that melts ice, each with a probe next to it: over
     * {@value #WAIT} ticks each probe gets only a few updates, not one per tick.
     *
     * @param helper the game test helper
     */
    private static void test(GameTestHelper helper) {
        var probeBlock = BuiltInRegistries.BLOCK.getValue(PROBE_ID);
        var flow = new BlockPos(20, 70, 10);
        var melt = new BlockPos(10, 70, 10);
        helper.setBlock(flow, FlowBlocks.block(1).get());
        helper.setBlock(melt, MeltBlocks.block(1).get());
        helper.setBlock(melt.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(melt.east(), Blocks.ICE);
        var probes = new BlockPos[] {flow.north(), melt.north()};
        for (var probe : probes) helper.setBlock(probe, probeBlock);
        for (var probe : probes) UPDATES.put(helper.absolutePos(probe), 0);
        var flowBe = (FlowGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(flow));
        helper.onEachTick(() -> flowBe.fill(1));
        helper.runAfterDelay(WAIT, () -> {
            var meltBe = (MeltBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(melt));
            helper.assertTrue(flowBe.energy().getAmountAsLong() >= WAIT - 5, "the flow generator was not filled");
            helper.assertTrue(meltBe.stored() > 0, "the melt generator made no FE");
            for (var probe : probes) {
                int updates = UPDATES.get(helper.absolutePos(probe));
                helper.assertTrue(updates <= MAX_UPDATES, "the generator next to " + probe + " sent " + updates + " comparator updates in " + WAIT + " ticks");
            }
            helper.succeed();
        });
    }
}
