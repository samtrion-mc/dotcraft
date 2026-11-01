package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlockEntity;

/**
 * GameTests of the generator core that need a world: the save and load roundtrip of a generator block entity.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class GeneratorCoreGameTests {
    private static final Identifier SAVE_LOAD = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "generator_save_load");
    private static final ResourceKey<Consumer<GameTestHelper>> SAVE_LOAD_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, SAVE_LOAD);

    private GeneratorCoreGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(SAVE_LOAD, TestSupport.ticking(GeneratorCoreGameTests::saveLoad)));
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(SAVE_LOAD, new TestEnvironmentDefinition.AllOf());
        event.registerTest(SAVE_LOAD, new FunctionGameTestInstance(SAVE_LOAD_FUNCTION,
                new TestData<>(environment, TestSupport.STRUCTURE, 60, 0, true)));
    }

    /**
     * A generator that made FE at noon saves its buffer, output and carry and a block of the same tier reads back an identical
     * tag; a block of tier 1 that loads the tag of a tier 5 block with more FE than it holds is capped to its own capacity and
     * keeps the output.
     *
     * @param helper the game test helper
     */
    private static void saveLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var big = new BlockPos(1, 1, 30);
        var same = new BlockPos(5, 1, 30);
        var small = new BlockPos(9, 1, 30);
        for (int x = 0; x <= 10; x++) {
            for (int y = 1; y <= 8; y++) helper.setBlock(new BlockPos(x, y, 30), Blocks.AIR);
        }
        helper.setBlock(big, EnergyBlocks.SOLAR.block(5).get());
        helper.setBlock(same, EnergyBlocks.SOLAR.block(5).get());
        helper.setBlock(small, EnergyBlocks.SOLAR.block(1).get());
        level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
        var source = (GeneratorBlockEntity) level.getBlockEntity(helper.absolutePos(big));
        int smallCapacity = EnergyCommonConfig.SOLAR_PANELS.get(0).capacity();
        source.fill(smallCapacity * 2);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(source.output() > 0, "the source makes nothing at noon");
            var saved = source.saveCustomOnly(level.registryAccess());
            var target = (GeneratorBlockEntity) level.getBlockEntity(helper.absolutePos(same));
            target.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            helper.assertTrue(saved.equals(target.saveCustomOnly(level.registryAccess())), "save and load changed the tag: " + saved);
            helper.assertTrue(target.output() == source.output(), "the output was lost");

            var capped = (GeneratorBlockEntity) level.getBlockEntity(helper.absolutePos(small));
            capped.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            long stored = capped.energy().getAmountAsLong();
            helper.assertTrue(stored <= smallCapacity, "the loaded FE exceed the capacity: " + stored);
            helper.assertTrue(capped.output() == source.output(), "the output was lost on the smaller block");
            helper.succeed();
        });
    }
}
