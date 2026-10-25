package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * GameTests of the energy mod, run with {@code ./gradlew :mods:energy:runGameTestServer}.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class EnergyGameTests {
    private static final Identifier MOD_LOADED = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "mod_loaded");
    private static final ResourceKey<Consumer<GameTestHelper>> MOD_LOADED_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, MOD_LOADED);

    private EnergyGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry ->
                registry.register(MOD_LOADED, (Consumer<GameTestHelper>) EnergyGameTests::modLoaded));
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(MOD_LOADED, new TestEnvironmentDefinition.AllOf());
        event.registerTest(MOD_LOADED, new FunctionGameTestInstance(MOD_LOADED_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    /**
     * The energy mod must be loaded in the test server.
     *
     * @param helper the game test helper
     */
    private static void modLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(EnergyMod.MODID), EnergyMod.MODID + " is not loaded");
        helper.succeed();
    }
}
