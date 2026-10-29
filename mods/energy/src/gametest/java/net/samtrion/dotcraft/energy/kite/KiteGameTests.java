package net.samtrion.dotcraft.energy.kite;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * GameTests of the kite wind generator, run with {@code ./gradlew :mods:energy:runGameTestServer}. Every test has its own
 * environment, so the tests run one after another (the weather is global), and each one keeps its blocks in its own area.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class KiteGameTests {
    /** The names of the tests; every one has a function, an environment and an instance. */
    private static final String[] NAMES = {"kite_blocks", "kite_flight", "kite_above_build_height", "kite_break_and_reel_in", "kite_respawn_and_save",
            "kite_free_space", "kite_thin_roof", "kite_comparator_and_status", "kite_thunder"};

    /** Not instantiable. */
    private KiteGameTests() {}

    /** The identifier of a test, in our namespace. */
    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, name);
    }

    /** The key of the registered test function of a test. */
    private static ResourceKey<Consumer<GameTestHelper>> function(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(name));
    }

    /** The body of a test. */
    private static Consumer<GameTestHelper> body(String name) {
        return switch (name) {
            case "kite_blocks" -> KiteGameTests::blocks;
            case "kite_flight" -> KiteGameTests::flight;
            case "kite_above_build_height" -> KiteGameTests::aboveBuildHeight;
            case "kite_break_and_reel_in" -> KiteGameTests::breakAndReelIn;
            case "kite_respawn_and_save" -> KiteGameTests::respawnAndSave;
            case "kite_free_space" -> KiteGameTests::freeSpace;
            case "kite_thin_roof" -> KiteGameTests::thinRoof;
            case "kite_comparator_and_status" -> KiteGameTests::comparatorAndStatus;
            default -> KiteGameTests::thunder;
        };
    }

    /** The timeout of a test in ticks. */
    private static int timeout(String name) {
        return switch (name) {
            case "kite_blocks" -> 60;
            case "kite_flight", "kite_above_build_height" -> 700;
            default -> 400;
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
            for (String name : NAMES) registry.register(id(name), body(name));
        });
    }

    /**
     * Registers the test instances; every one runs in clear weather (the thunder test sets its own).
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : NAMES) {
            var environment = event.registerEnvironment(id(name), new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.CLEAR));
            event.registerTest(id(name), new FunctionGameTestInstance(function(name),
                    new TestData<>(environment, Identifier.withDefaultNamespace("empty"), timeout(name), 0, true)));
        }
    }

    /** The kite block entity at a position relative to the test, which must be there. */
    private static KiteBlockEntity entity(GameTestHelper helper, BlockPos relative) {
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof KiteBlockEntity, "no kite block entity at " + relative);
        return (KiteBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }

    /** A mock player that right-clicks the block with a kite (or with an empty hand). */
    private static net.minecraft.world.entity.player.Player player(GameTestHelper helper, boolean withKite) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, withKite ? new ItemStack(KiteBlocks.KITE.get(), 2) : ItemStack.EMPTY);
        return player;
    }

    /**
     * Five blocks, one block entity each; the buffer can be drained from every side but never filled; the block is no solar or lunar
     * panel and a panel beside it does not see it as a group member.
     *
     * @param helper the game test helper
     */
    private static void blocks(GameTestHelper helper) {
        var blocks = KiteBlocks.blocks().toList();
        helper.assertTrue(blocks.size() == 5, "expected 5 kite generators, found " + blocks.size());
        int x = 0;
        for (var block : blocks) {
            helper.assertTrue(!((Object) block.get() instanceof GeneratorBlock), "a kite generator is a panel");
            helper.setBlock(new BlockPos(2 * x++, 1, 1), block.get());
        }
        helper.setBlock(new BlockPos(0, 1, 3), EnergyBlocks.SOLAR.block(1).get());
        helper.setBlock(new BlockPos(0, 1, 2), blocks.get(0).get());
        var panel = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(0, 1, 3)));
        helper.assertTrue(!panel.getValue(GeneratorBlock.NORTH), "a panel groups with a kite generator");
        int i = 0;
        for (var block : blocks) {
            var pos = helper.absolutePos(new BlockPos(2 * i++, 1, 1));
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof KiteBlockEntity, block.getId() + " has no block entity");
            for (Direction side : Direction.values()) {
                EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, pos, side);
                helper.assertTrue(handler != null, block.getId() + " has no capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, block.getId() + " accepted energy");
                }
            }
        }
        helper.succeed();
    }

    /**
     * Right-click with a kite puts it in (one item used up) and it flies: the kite entity rises to the full tether of the tier on a
     * free sky, the generator makes FE from its altitude, buffers it and gives it to a neighbour that can take it.
     *
     * @param helper the game test helper
     */
    private static void flight(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(1).get());
        helper.setBlock(new BlockPos(2, 1, 1), EnergyBlocks.POWER_DRAIN.get());
        var player = player(helper, true);
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the kite item was not used up");
        var be = entity(helper, pos);
        helper.assertTrue(be.hasKite() && be.kite() != null && !be.kite().isRemoved(), "no kite flies");
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a second kite was put in");
        var peak = EnergyCommonConfig.KITE_GENERATORS.get(0).peak();
        helper.succeedWhen(() -> {
            var kite = be.kite();
            helper.assertTrue(kite != null && kite.altitude() >= KiteMath.tether(1) - 0.5, "the kite has not reached the tether yet");
            helper.assertTrue(be.output() > 0 && be.output() <= peak, "wrong output " + be.output());
            helper.assertTrue(helper.getLevel().getEntity(kite.getUUID()) == kite, "the kite is not in the world");
        });
    }

    /**
     * The kite is not bound to the build height: a tier 5 kite that is put above the top of the world lives, keeps ticking, climbs on
     * and the block makes FE from that altitude.
     *
     * @param helper the game test helper
     */
    private static void aboveBuildHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(5).get());
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        var kite = be.kite();
        helper.assertTrue(kite != null, "no kite");
        double needed = level.getMaxY() + 50 - helper.absolutePos(pos).getY();
        helper.assertTrue(needed > 0 && needed < KiteMath.tether(5), "the tether does not reach above the build height: " + needed);
        kite.setAltitude(needed);
        helper.assertTrue(kite.getY() > level.getMaxY(), "the kite is not above the build height");
        int age = kite.tickCount;
        helper.succeedWhen(() -> {
            helper.assertTrue(!kite.isRemoved() && level.getEntity(kite.getUUID()) == kite, "the kite above the build height was removed");
            helper.assertTrue(kite.tickCount > age + 40, "the kite above the build height does not tick");
            helper.assertTrue(kite.getY() > level.getMaxY() + 50, "the kite does not climb on above the build height: " + kite.getY());
            double expectedMin = EnergyCommonConfig.KITE_GENERATORS.get(4).peak() * KiteMath.share(5, needed) * 0.4;
            helper.assertTrue(be.output() > expectedMin, "output " + be.output() + " below " + expectedMin);
        });
    }

    /**
     * Sneaking with an empty hand reels the kite in and gives the item back; breaking the block with a kite in drops the kite item
     * and removes the entity.
     *
     * @param helper the game test helper
     */
    private static void breakAndReelIn(GameTestHelper helper) {
        var first = new BlockPos(1, 1, 1);
        var second = new BlockPos(5, 1, 1);
        helper.setBlock(first, KiteBlocks.block(2).get());
        helper.setBlock(second, KiteBlocks.block(2).get());
        var player = player(helper, true);
        helper.useBlock(first, player);
        helper.useBlock(second, player);
        var firstBe = entity(helper, first);
        var secondBe = entity(helper, second);
        var firstKite = firstBe.kite();
        var secondKite = secondBe.kite();
        helper.assertTrue(firstKite != null && secondKite != null, "no kites");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        helper.useBlock(first, player);
        helper.assertTrue(!firstBe.hasKite() && firstKite.isRemoved(), "the kite was not reeled in");
        helper.assertTrue(player.getInventory().contains(s -> s.is(KiteBlocks.KITE.get())), "the kite item did not come back");
        helper.setBlock(second, Blocks.AIR);
        helper.assertItemEntityPresent(KiteBlocks.KITE.get(), second, 2.0);
        helper.assertTrue(secondKite.isRemoved(), "the kite stays after its block is gone");
        helper.succeed();
    }

    /**
     * A kite that vanishes (killed) is replaced by the block entity; what is saved (the kite flag and the FE) comes back through a
     * save and load of the block entity, and a loaded block entity without an entity spawns the kite again.
     *
     * @param helper the game test helper
     */
    private static void respawnAndSave(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(1).get());
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        var old = be.kite();
        helper.assertTrue(old != null, "no kite");
        var saved = be.saveWithFullMetadata(level.registryAccess());
        var absolute = helper.absolutePos(pos);
        BlockEntity loaded = BlockEntity.loadStatic(absolute, level.getBlockState(absolute), saved, level.registryAccess());
        helper.assertTrue(loaded instanceof KiteBlockEntity copy && copy.hasKite() && copy.kite() == null, "the kite flag is not saved");
        old.kill(level);
        helper.succeedWhen(() -> {
            var now = be.kite();
            helper.assertTrue(now != null && now != old && !now.isRemoved() && level.getEntity(now.getUUID()) == now, "the kite was not replaced");
        });
    }

    /**
     * A roof over the generator keeps the kite down: it stops at the last free sample below the roof, so a kite under a roof makes
     * (almost) nothing, a kite in the open flies up to its tether.
     *
     * @param helper the game test helper
     */
    private static void freeSpace(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(3).get());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int h = 3; h <= 6; h++) helper.setBlock(pos.offset(dx, h, dz), Blocks.STONE);
            }
        }
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        helper.runAfterDelay(120, () -> {
            var kite = be.kite();
            helper.assertTrue(kite != null && kite.altitude() <= 2.01, "the kite flies through the roof: " + (kite == null ? -1 : kite.altitude()));
            helper.succeed();
        });
    }

    /**
     * A single block thick plate far up stops the kite right below it; no sample may skip it. Tier 1 needs a free cross, so a plate
     * that only covers a neighbour of the column stops it, while tier 3 flies past that (a shaft of one block is enough).
     *
     * @param helper the game test helper
     */
    private static void thinRoof(GameTestHelper helper) {
        var wide = new BlockPos(1, 1, 1);
        var slim = new BlockPos(9, 1, 1);
        helper.setBlock(wide, KiteBlocks.block(1).get());
        helper.setBlock(slim, KiteBlocks.block(3).get());
        helper.setBlock(wide.offset(0, 10, 0), Blocks.STONE);
        helper.setBlock(slim.offset(1, 10, 0), Blocks.STONE);
        var player = player(helper, true);
        helper.useBlock(wide, player);
        helper.useBlock(slim, player);
        var wideBe = entity(helper, wide);
        var slimBe = entity(helper, slim);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(wideBe.kite().altitude() <= 9.01 && wideBe.kite().altitude() >= 8.5, "wrong ceiling: " + wideBe.kite().altitude());
            helper.assertTrue(slimBe.kite().altitude() > 20, "the slim line is stopped by a plate beside the column: " + slimBe.kite().altitude());
            helper.succeed();
        });
    }

    /**
     * The status message names the reason for a low output and the comparator signal follows the output (0 without a kite, 1..15 with).
     *
     * @param helper the game test helper
     */
    private static void comparatorAndStatus(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(1).get());
        var absolute = helper.absolutePos(pos);
        helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 0, "a signal without a kite");
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        be.kite().setAltitude(KiteMath.tether(1));
        helper.succeedWhen(() -> {
            int signal = level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH);
            helper.assertTrue(signal >= 1 && signal <= 15 && signal == be.signal(), "no comparator signal: " + signal);
            helper.assertTrue(be.limit() == KiteMath.Limit.NONE, "unexpected limit " + be.limit());
        });
    }

    /**
     * In a thunderstorm a tier 1 kite is reeled in and makes nothing; a tier 5 kite stays up and keeps its output. Nothing is hit by
     * lightning because of the kite.
     *
     * @param helper the game test helper
     */
    private static void thunder(GameTestHelper helper) {
        var level = helper.getLevel();
        var weak = new BlockPos(1, 1, 1);
        var strong = new BlockPos(5, 1, 1);
        helper.setBlock(weak, KiteBlocks.block(1).get());
        helper.setBlock(strong, KiteBlocks.block(5).get());
        var player = player(helper, true);
        helper.useBlock(weak, player);
        helper.useBlock(strong, player);
        var weakBe = entity(helper, weak);
        var strongBe = entity(helper, strong);
        weakBe.kite().setAltitude(20);
        strongBe.kite().setAltitude(20);
        level.getServer().setWeatherParameters(0, 100_000, true, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(level.isThundering(), "no thunderstorm");
            helper.assertTrue(level.isRainingAt(helper.absolutePos(weak).above(20)), "the test biome has no precipitation");
            helper.assertTrue(weakBe.kite().altitude() < 1 && weakBe.output() == 0, "the tier 1 kite is not reeled in: " + weakBe.kite().altitude() + ", " + weakBe.output());
            helper.assertTrue(strongBe.kite().altitude() >= 20 && strongBe.output() > 0, "the tier 5 kite does not fly in the storm: " + strongBe.output());
        });
    }
}
