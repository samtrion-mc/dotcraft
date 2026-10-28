package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.samtrion.dotcraft.energy.item.EnergyItems;

/**
 * GameTests of the energy mod, run with {@code ./gradlew :mods:energy:runGameTestServer}.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class EnergyGameTests {
    private static final EnergyCommonConfig.Battery POTATO = EnergyCommonConfig.POTATO_BATTERY;
    private static final EnergyCommonConfig.Battery RECHARGEABLE = EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY;

    private static final Identifier MOD_LOADED = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "mod_loaded");
    private static final ResourceKey<Consumer<GameTestHelper>> MOD_LOADED_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, MOD_LOADED);

    private static final Identifier BATTERIES = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "batteries");
    private static final ResourceKey<Consumer<GameTestHelper>> BATTERIES_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, BATTERIES);

    private EnergyGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(MOD_LOADED, (Consumer<GameTestHelper>) EnergyGameTests::modLoaded);
            registry.register(BATTERIES, (Consumer<GameTestHelper>) EnergyGameTests::batteries);
        });
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
        var batteriesEnvironment = event.registerEnvironment(BATTERIES, new TestEnvironmentDefinition.AllOf());
        event.registerTest(BATTERIES, new FunctionGameTestInstance(BATTERIES_FUNCTION,
                new TestData<>(batteriesEnvironment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    /**
     * Both batteries stack to 1, have the FE capability and start full; the single-use one drains in steps of its extract limit,
     * rejects charging and becomes a baked potato at 0 FE, the rechargeable one accepts charging and stays itself.
     *
     * @param helper the game test helper
     */
    private static void batteries(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var potato = new ItemStack(EnergyItems.POTATO_BATTERY.get());
        var rechargeable = new ItemStack(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get());
        helper.assertTrue(potato.getMaxStackSize() == 1, "potato battery stacks");
        helper.assertTrue(rechargeable.getMaxStackSize() == 1, "rechargeable battery stacks");
        player.getInventory().setItem(0, potato);
        player.getInventory().setItem(1, rechargeable);
        var potatoAccess = ItemAccess.forPlayerSlot(player, 0);
        var rechargeableAccess = ItemAccess.forPlayerSlot(player, 1);
        EnergyHandler single = potatoAccess.getCapability(Capabilities.Energy.ITEM);
        EnergyHandler multi = rechargeableAccess.getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(single != null && multi != null, "a battery has no energy capability");
        helper.assertTrue(single.getAmountAsInt() == POTATO.capacity(), "potato battery not full");
        helper.assertTrue(multi.getAmountAsInt() == RECHARGEABLE.capacity(), "rechargeable battery not full");

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(single.insert(1000, tx) == 0, "potato battery accepted energy");
            helper.assertTrue(single.extract(1000, tx) == POTATO.maxOutput(), "extract not limited");
            tx.commit();
        }
        helper.assertTrue(potatoAccess.getResource().is(EnergyItems.POTATO_BATTERY.get()), "potato battery changed too early");
        helper.assertTrue(single.getAmountAsInt() == POTATO.capacity() - POTATO.maxOutput(),
                "extract did not drain");
        try (var tx = Transaction.openRoot()) {
            for (int i = 0; i < POTATO.capacity() / POTATO.maxOutput(); i++) {
                single.extract(POTATO.maxOutput(), tx);
            }
            tx.commit();
        }
        helper.assertTrue(potatoAccess.getResource().is(Items.BAKED_POTATO), "drained potato battery is not a baked potato");

        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(multi.extract(1000, tx) == RECHARGEABLE.maxOutput(), "extract not limited");
            tx.commit();
        }
        helper.assertTrue(multi.getAmountAsInt() == RECHARGEABLE.capacity() - RECHARGEABLE.maxOutput(),
                "extract did not drain");
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(multi.insert(1000, tx) == RECHARGEABLE.maxInput(), "insert not accepted");
            tx.commit();
        }
        helper.assertTrue(multi.getAmountAsInt() == RECHARGEABLE.capacity(), "rechargeable battery not recharged");
        try (var tx = Transaction.openRoot()) {
            for (int i = 0; i < RECHARGEABLE.capacity() / RECHARGEABLE.maxOutput(); i++) {
                multi.extract(RECHARGEABLE.maxOutput(), tx);
            }
            tx.commit();
        }
        helper.assertTrue(rechargeableAccess.getResource().is(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get()), "rechargeable battery changed");
        helper.assertTrue(multi.getAmountAsInt() == 0, "rechargeable battery not empty");
        helper.succeed();
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
