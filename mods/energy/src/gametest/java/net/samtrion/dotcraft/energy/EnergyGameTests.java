package net.samtrion.dotcraft.energy;

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
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;
import net.samtrion.dotcraft.energy.generator.GeneratorBlockEntity;
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

    private static final Identifier PANELS = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "panels");
    private static final ResourceKey<Consumer<GameTestHelper>> PANELS_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, PANELS);

    private static final Identifier POWER_DRAIN = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "power_drain");
    private static final ResourceKey<Consumer<GameTestHelper>> POWER_DRAIN_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, POWER_DRAIN);

    private static final Identifier GROUP = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "group");
    private static final ResourceKey<Consumer<GameTestHelper>> GROUP_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, GROUP);

    private static final Identifier FLAT = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "flat_panel");
    private static final ResourceKey<Consumer<GameTestHelper>> FLAT_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, FLAT);

    private static final Identifier DAY_NIGHT = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "day_night");
    private static final ResourceKey<Consumer<GameTestHelper>> DAY_NIGHT_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, DAY_NIGHT);

    private EnergyGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(MOD_LOADED, TestSupport.ticking(EnergyGameTests::modLoaded));
            registry.register(BATTERIES, TestSupport.ticking(EnergyGameTests::batteries));
            registry.register(PANELS, TestSupport.ticking(EnergyGameTests::panels));
            registry.register(FLAT, TestSupport.ticking(EnergyGameTests::flatPanel));
            registry.register(DAY_NIGHT, TestSupport.ticking(EnergyGameTests::dayNight));
            registry.register(GROUP, TestSupport.ticking(EnergyGameTests::group));
            registry.register(POWER_DRAIN, TestSupport.ticking(EnergyGameTests::powerDrain));
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
                new TestData<>(environment, TestSupport.STRUCTURE, 20, 0, true)));
        var batteriesEnvironment = event.registerEnvironment(BATTERIES, new TestEnvironmentDefinition.AllOf());
        event.registerTest(BATTERIES, new FunctionGameTestInstance(BATTERIES_FUNCTION,
                new TestData<>(batteriesEnvironment, TestSupport.STRUCTURE, 20, 0, true)));
        var drainEnvironment = event.registerEnvironment(POWER_DRAIN, new TestEnvironmentDefinition.AllOf());
        event.registerTest(POWER_DRAIN, new FunctionGameTestInstance(POWER_DRAIN_FUNCTION,
                new TestData<>(drainEnvironment, TestSupport.STRUCTURE, 20, 0, true)));
        var flatEnvironment = event.registerEnvironment(FLAT, new TestEnvironmentDefinition.AllOf());
        event.registerTest(FLAT, new FunctionGameTestInstance(FLAT_FUNCTION,
                new TestData<>(flatEnvironment, TestSupport.STRUCTURE, 20, 0, true)));
        var dayNightEnvironment = event.registerEnvironment(DAY_NIGHT, new TestEnvironmentDefinition.AllOf());
        event.registerTest(DAY_NIGHT, new FunctionGameTestInstance(DAY_NIGHT_FUNCTION,
                new TestData<>(dayNightEnvironment, TestSupport.STRUCTURE, 80, 0, true)));
        var groupEnvironment = event.registerEnvironment(GROUP, new TestEnvironmentDefinition.AllOf());
        event.registerTest(GROUP, new FunctionGameTestInstance(GROUP_FUNCTION,
                new TestData<>(groupEnvironment, TestSupport.STRUCTURE, 20, 0, true)));
        var panelsEnvironment = event.registerEnvironment(PANELS, new TestEnvironmentDefinition.AllOf());
        event.registerTest(PANELS, new FunctionGameTestInstance(PANELS_FUNCTION,
                new TestData<>(panelsEnvironment, TestSupport.STRUCTURE, 60, 0, true)));
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

        // the copper tier 1 battery: its own FE capability, twice the capacity of the potato one
        player.getInventory().setItem(2, new ItemStack(EnergyItems.BATTERY_TIER_1.get()));
        EnergyHandler tier1 = ItemAccess.forPlayerSlot(player, 2).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(tier1 != null && tier1.getAmountAsInt() == EnergyCommonConfig.BATTERY_TIER_1.capacity()
                && tier1.getCapacityAsInt() == 2 * RECHARGEABLE.capacity(), "tier 1 battery is not a full 40 000 FE battery");
        helper.succeed();
    }

    /**
     * Every solar and lunar panel gets a block entity with an FE capability on every side that cannot be charged from outside but
     * can be drained; the panels tick for a while without error and never exceed their capacity.
     *
     * @param helper the game test helper
     */
    private static void panels(GameTestHelper helper) {
        var level = helper.getLevel();
        var blocks = EnergyBlocks.blocks().toList();
        helper.assertTrue(blocks.size() == 10, "expected 10 panels, found " + blocks.size());
        int x = 0;
        for (var block : blocks) helper.setBlock(new BlockPos(2 * x++, 1, 0), block.get());
        helper.runAfterDelay(45, () -> {
            int i = 0;
            for (var block : blocks) {
                var pos = helper.absolutePos(new BlockPos(2 * i++, 1, 0));
                helper.assertTrue(level.getBlockEntity(pos) instanceof GeneratorBlockEntity, block.getId() + " has no block entity");
                for (Direction side : Direction.values()) {
                    EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
                    helper.assertTrue(handler != null, block.getId() + " has no capability on " + side);
                    try (var tx = Transaction.openRoot()) {
                        helper.assertTrue(handler.insert(1000, tx) == 0, block.getId() + " accepted energy");
                    }
                    helper.assertTrue(handler.getAmountAsInt() <= handler.getCapacityAsInt(), block.getId() + " over capacity");
                }
            }
            helper.succeed();
        });
    }

    /**
     * At noon only a solar panel makes FE and at midnight only a lunar panel, in a clear sky, with the real sun angle of the
     * overworld clock.
     *
     * @param helper the game test helper
     */
    private static void dayNight(GameTestHelper helper) {
        var level = helper.getLevel();
        var clock = level.dimensionType().defaultClock().orElseThrow();
        var solar = EnergyBlocks.SOLAR.block(3).get();
        var lunar = EnergyBlocks.LUNAR.block(3).get();
        var solarPos = new BlockPos(1, 1, 1);
        var lunarPos = new BlockPos(5, 1, 1);
        // the batches of the tests start at the same places one after the other: clear what an earlier test left above the panels
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 8; y++) helper.setBlock(new BlockPos(x, y, 1), net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.setBlock(solarPos, solar);
        helper.setBlock(lunarPos, lunar);
        var solarAbs = helper.absolutePos(solarPos);
        var lunarAbs = helper.absolutePos(lunarPos);
        level.getServer().clockManager().moveToTimeMarker(clock, net.minecraft.world.clock.ClockTimeMarkers.NOON);
        helper.runAfterDelay(5, () -> {
            float angle = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, solarAbs);
            double sun = solar.kind().output(level, solarAbs, 3);
            double moon = lunar.kind().output(level, lunarAbs, 3);
            String at = " at noon: sun angle " + angle + ", solar " + sun + ", lunar " + moon + ", sees sky " + level.canSeeSky(solarAbs.above())
                    + ", sky " + level.dimensionType().hasSkyLight();
            helper.assertTrue(sun > 0 && moon == 0, "wrong output" + at);
            level.getServer().clockManager().moveToTimeMarker(clock, net.minecraft.world.clock.ClockTimeMarkers.MIDNIGHT);
            helper.runAfterDelay(5, () -> {
                float night = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, solarAbs);
                double sun2 = solar.kind().output(level, solarAbs, 3);
                double moon2 = lunar.kind().output(level, lunarAbs, 3);
                helper.assertTrue(sun2 == 0 && moon2 > 0, "wrong output at midnight: sun angle " + night + ", solar " + sun2 + ", lunar " + moon2 + at);
                helper.succeed();
            });
        });
    }

    /**
     * A panel is a flat plate of an eighth of a block, nothing can spawn on it, and a living entity on it shades it.
     *
     * @param helper the game test helper
     */
    private static void flatPanel(GameTestHelper helper) {
        var panel = EnergyBlocks.SOLAR.block(1).get();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, panel);
        var absolute = helper.absolutePos(pos);
        var state = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(state.getShape(helper.getLevel(), absolute).max(Direction.Axis.Y) == 0.125, "the panel is not an eighth of a block high");
        helper.assertTrue(state.getCollisionShape(helper.getLevel(), absolute).max(Direction.Axis.Y) == 0.125, "the collision box is not an eighth of a block high");
        for (var type : new net.minecraft.world.entity.EntityType<?>[] {net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.PIG}) {
            helper.assertTrue(!state.isValidSpawn(helper.getLevel(), absolute, type), type + " may spawn on a panel");
        }
        helper.assertTrue(!state.getValue(GeneratorBlock.WATERLOGGED), "a dry panel is waterlogged");
        helper.setBlock(new BlockPos(3, 1, 1), net.minecraft.world.level.block.Blocks.WATER);
        helper.setBlock(new BlockPos(3, 1, 1), panel.defaultBlockState().setValue(GeneratorBlock.WATERLOGGED, true));
        var wet = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 1)));
        helper.assertTrue(wet.getValue(GeneratorBlock.WATERLOGGED) && wet.getFluidState().is(net.minecraft.tags.FluidTags.WATER), "a waterlogged panel holds no water");
        var be = (GeneratorBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(!be.isOccupied(), "an empty panel is occupied");
        helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, 1.5F, 1.125F, 1.5F);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.isOccupied(), "a pig on the panel does not shade it");
            helper.succeed();
        });
    }

    /**
     * Panels in a row (solar tier 1, lunar tier 2, solar tier 1) share one pool: its capacity is the sum, the side properties show the
     * neighbours, an extraction is limited by the tier of the block it is taken from but draws from all of them, and a removed block
     * hands its FE on.
     *
     * @param helper the game test helper
     */
    private static void group(GameTestHelper helper) {
        var level = helper.getLevel();
        var solar = EnergyBlocks.SOLAR.block(1).get();
        var lunar = EnergyBlocks.LUNAR.block(2).get();
        helper.setBlock(new BlockPos(0, 5, 0), solar);
        helper.setBlock(new BlockPos(1, 5, 0), lunar);
        helper.setBlock(new BlockPos(2, 5, 0), solar);
        var first = helper.absolutePos(new BlockPos(0, 5, 0));
        var middle = helper.absolutePos(new BlockPos(1, 5, 0));
        var last = helper.absolutePos(new BlockPos(2, 5, 0));
        helper.assertTrue(level.getBlockState(middle).getValue(GeneratorBlock.EAST) && level.getBlockState(middle).getValue(GeneratorBlock.WEST),
                "the middle panel does not show both neighbours");
        helper.assertTrue(!level.getBlockState(first).getValue(GeneratorBlock.WEST) && level.getBlockState(first).getValue(GeneratorBlock.EAST),
                "the first panel shows a wrong neighbour");

        var solarConfig = EnergyCommonConfig.SOLAR_PANELS.get(0);
        var lunarConfig = EnergyCommonConfig.LUNAR_PANELS.get(1);
        EnergyHandler fromFirst = level.getCapability(Capabilities.Energy.BLOCK, first, Direction.WEST);
        EnergyHandler fromMiddle = level.getCapability(Capabilities.Energy.BLOCK, middle, Direction.DOWN);
        helper.assertTrue(fromFirst != null && fromMiddle != null, "no capability");
        long capacity = 2L * solarConfig.capacity() + lunarConfig.capacity();
        helper.assertTrue(fromFirst.getCapacityAsLong() == capacity, "capacity is not the sum: " + fromFirst.getCapacityAsLong());
        helper.assertTrue(fromMiddle.getCapacityAsLong() == capacity, "capacity differs between members");

        var middleBe = (GeneratorBlockEntity) level.getBlockEntity(middle);
        var lastBe = (GeneratorBlockEntity) level.getBlockEntity(last);
        int offered = lunarConfig.capacity() / 2;
        helper.assertTrue(middleBe.fill(offered) == offered, "buffer too small");
        helper.assertTrue(fromFirst.getAmountAsLong() >= offered, "the first panel does not see the FE of the middle one");
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(fromFirst.extract(Integer.MAX_VALUE, tx) == Math.min(solarConfig.maxOutput(), offered), "extraction not limited by the tier 1 panel");
            helper.assertTrue(fromMiddle.extract(Integer.MAX_VALUE, tx) == Math.min(lunarConfig.maxOutput(), offered), "extraction not limited by the tier 2 panel");
        }
        long before = fromFirst.getAmountAsLong() + lastBe.fill(Math.min(1_000, solarConfig.capacity()));
        helper.setBlock(new BlockPos(2, 5, 0), net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(!level.getBlockState(middle).getValue(GeneratorBlock.EAST), "the middle panel still shows the removed neighbour");
        helper.assertTrue(fromFirst.getAmountAsLong() >= before - 1, "the FE of the removed panel was lost: " + fromFirst.getAmountAsLong() + " < " + before);
        helper.succeed();
    }

    /**
     * The power drain accepts every offered FE from every side, even {@link Integer#MAX_VALUE}, and keeps none of it.
     *
     * @param helper the game test helper
     */
    private static void powerDrain(GameTestHelper helper) {
        var pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, EnergyBlocks.POWER_DRAIN.get());
        for (Direction side : Direction.values()) {
            EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), side);
            helper.assertTrue(handler != null, "no capability on " + side);
            try (var tx = Transaction.openRoot()) {
                helper.assertTrue(handler.insert(Integer.MAX_VALUE, tx) == Integer.MAX_VALUE, "drain did not take everything on " + side);
                helper.assertTrue(handler.extract(1000, tx) == 0, "drain gave energy on " + side);
                tx.commit();
            }
            helper.assertTrue(handler.getAmountAsLong() == 0, "drain kept energy on " + side);
        }
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
