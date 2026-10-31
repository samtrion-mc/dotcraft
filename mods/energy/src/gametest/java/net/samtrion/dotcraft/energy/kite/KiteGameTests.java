package net.samtrion.dotcraft.energy.kite;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.samtrion.dotcraft.energy.TestSupport;
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
            "kite_free_space", "kite_thin_roof", "kite_comparator_and_status", "kite_thunder", "kite_ignores_obstructions", "kite_two_kites",
            "kite_lightning", "kite_wire", "kite_short_wire", "kite_too_close", "kite_play", "kite_storm_radius", "kite_bolts", "kite_recipe_wire", "kite_diagonal_roof"};

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
        var test = TestSupport.ticking(rawBody(name));
        return helper -> {
            clear(helper);
            test.accept(helper);
        };
    }

    /**
     * Clears the space the test uses, because the batches of the tests start at the same places and leave things behind.
     */
    private static void clear(GameTestHelper helper) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        // a generator that an earlier test left (anywhere in the level) would block this test through the minimum distance
        for (var be : KiteBlockEntity.loaded(helper.getLevel())) helper.getLevel().setBlock(be.getBlockPos(), Blocks.AIR.defaultBlockState(), 2);
        // the batches of the tests start at the same places one after the other, so leftovers (water, stone) of an earlier test would
        // be in the way of this one: clear the space the kite tests use
        for (int x = -3; x <= 24; x++) {
            for (int y = 0; y <= 30; y++) {
                for (int z = -3; z <= 6; z++) helper.getLevel().setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /** The body of a test without the chunk forcing. */
    private static Consumer<GameTestHelper> rawBody(String name) {
        return switch (name) {
            case "kite_blocks" -> KiteGameTests::blocks;
            case "kite_flight" -> KiteGameTests::flight;
            case "kite_above_build_height" -> KiteGameTests::aboveBuildHeight;
            case "kite_break_and_reel_in" -> KiteGameTests::breakAndReelIn;
            case "kite_respawn_and_save" -> KiteGameTests::respawnAndSave;
            case "kite_free_space" -> KiteGameTests::freeSpace;
            case "kite_thin_roof" -> KiteGameTests::thinRoof;
            case "kite_comparator_and_status" -> KiteGameTests::comparatorAndStatus;
            case "kite_ignores_obstructions" -> KiteGameTests::ignoresObstructions;
            case "kite_two_kites" -> KiteGameTests::twoKites;
            case "kite_lightning" -> KiteGameTests::lightning;
            case "kite_wire" -> KiteGameTests::wire;
            case "kite_short_wire" -> KiteGameTests::shortWire;
            case "kite_too_close" -> KiteGameTests::tooClose;
            case "kite_play" -> KiteGameTests::play;
            case "kite_storm_radius" -> KiteGameTests::stormRadius;
            case "kite_bolts" -> KiteGameTests::bolts;
            case "kite_recipe_wire" -> KiteGameTests::recipeWire;
            case "kite_diagonal_roof" -> KiteGameTests::diagonalRoof;
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
                    new TestData<>(environment, TestSupport.STRUCTURE, timeout(name), 0, true)));
        }
    }

    /** The kite block entity at a position relative to the test, which must be there. */
    private static KiteBlockEntity entity(GameTestHelper helper, BlockPos relative) {
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof KiteBlockEntity, "no kite block entity at " + relative);
        return (KiteBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
    }

    /** The second generator of a test: further than {@link KiteMath#MIN_DISTANCE} from the one at x = 1, so the two do not block each other. */
    private static final int FAR_X = 20;

    /** Sets a generator block of a tier with its tether full, so a kite put in flies at once. */
    private static void place(GameTestHelper helper, BlockPos pos, int tier) {
        helper.setBlock(pos, KiteBlocks.block(tier).get());
        entity(helper, pos).insertWire(KiteMath.tether(tier));
    }

    /** A stack of the iron wire of the resources mod. */
    private static ItemStack wireStack(int count) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("dotcraftresources", "iron_wire")), count);
    }

    /** The horizontal offset of a kite from the axis of its block. */
    private static double offset(GameTestHelper helper, BlockPos block, KiteEntity kite) {
        var abs = helper.absolutePos(block);
        return Math.hypot(kite.getX() - (abs.getX() + 0.5), kite.getZ() - (abs.getZ() + 0.5));
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
        place(helper, pos, 1);
        helper.setBlock(new BlockPos(2, 1, 1), EnergyBlocks.POWER_DRAIN.get());
        var player = player(helper, true);
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the kite item was not used up");
        var be = entity(helper, pos);
        helper.assertTrue(be.hasKite() && be.kite() != null && !be.kite().isRemoved(), "no kite flies");
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "a second kite was put in");
        var peak = KiteMath.peak(EnergyCommonConfig.KITE_GENERATORS.get(0).peak(), 1);
        helper.succeedWhen(() -> {
            var kite = be.kite();
            helper.assertTrue(kite != null && kite.altitude() >= KiteMath.tether(1) - 0.5, "the kite has not reached the tether yet");
            helper.assertTrue(be.output() > 0 && be.output() <= peak, "wrong output " + be.output());
            helper.assertTrue(helper.getLevel().getEntity(kite.getUUID()) == kite, "the kite is not in the world");
        });
    }

    /**
     * The kite is not bound to the build height: a tier 5 kite on a block just below the top of the world, put above it, lives, keeps ticking, climbs on
     * and the block makes FE from that altitude.
     *
     * @param helper the game test helper
     */
    private static void aboveBuildHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, level.getMaxY() - 10 - helper.absolutePos(BlockPos.ZERO).getY(), 1);
        place(helper, pos, 5);
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        var kite = be.kite();
        helper.assertTrue(kite != null, "no kite");
        double needed = level.getMaxY() + 30 - helper.absolutePos(pos).getY();
        helper.assertTrue(needed > 0 && needed < KiteMath.tether(5), "the tether does not reach above the build height: " + needed);
        kite.setAltitude(needed);
        helper.assertTrue(kite.getY() > level.getMaxY(), "the kite is not above the build height");
        int age = kite.tickCount;
        helper.succeedWhen(() -> {
            helper.assertTrue(!kite.isRemoved() && level.getEntity(kite.getUUID()) == kite, "the kite above the build height was removed");
            helper.assertTrue(kite.tickCount > age + 40, "the kite above the build height does not tick");
            helper.assertTrue(kite.getY() > level.getMaxY() + 30, "the kite does not climb on above the build height: " + kite.getY());
            double expectedMin = EnergyCommonConfig.KITE_GENERATORS.get(4).peak() * KiteMath.lengthFactor(5, needed) * 0.4;
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
        var second = new BlockPos(FAR_X, 1, 1);
        place(helper, first, 2);
        place(helper, second, 2);
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
        place(helper, pos, 1);
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
        place(helper, pos, 2);
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
     * A single block thick plate far up stops the kite right below it; no sample may skip it. Tier 1 needs a free 3x3, so a plate
     * that only covers a neighbour of the column stops it, while tier 4 flies past that (a shaft of one block is enough).
     *
     * @param helper the game test helper
     */
    private static void thinRoof(GameTestHelper helper) {
        var wide = new BlockPos(1, 1, 1);
        var slim = new BlockPos(FAR_X, 1, 1);
        place(helper, wide, 1);
        place(helper, slim, 4);
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
     * Tier 1 and 2 need a free 3x3 way up, as the tooltip says: a plate that only covers a diagonal neighbour of the column stops
     * the kite below it, while tier 3 (a column of one block) flies past it.
     *
     * @param helper the game test helper
     */
    private static void diagonalRoof(GameTestHelper helper) {
        var wide = new BlockPos(1, 1, 1);
        var slim = new BlockPos(FAR_X, 1, 1);
        place(helper, wide, 1);
        place(helper, slim, 4);
        helper.setBlock(wide.offset(1, 10, 1), Blocks.STONE);
        helper.setBlock(slim.offset(1, 10, 1), Blocks.STONE);
        var player = player(helper, true);
        helper.useBlock(wide, player);
        helper.useBlock(slim, player);
        var wideBe = entity(helper, wide);
        var slimBe = entity(helper, slim);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(wideBe.kite().altitude() <= 9.01, "the wide line is not stopped by a diagonal block: " + wideBe.kite().altitude());
            helper.assertTrue(slimBe.kite().altitude() > 20, "the slim line is stopped by a diagonal block: " + slimBe.kite().altitude());
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
        place(helper, pos, 1);
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
        var strong = new BlockPos(FAR_X, 1, 1);
        place(helper, weak, 1);
        place(helper, strong, 5);
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
            // the weather is global: leave clear weather behind for the tests that follow
            level.getServer().setWeatherParameters(6000, 0, false, false);
        });
    }

    /**
     * The tier 3 kite ignores obstructions: under a thick roof it still flies to its tether and makes the output of it, while a
     * tier 2 kite under the same roof stays down. The kite stays above the generator block (the tether never runs through it).
     *
     * @param helper the game test helper
     */
    private static void ignoresObstructions(GameTestHelper helper) {
        var free = new BlockPos(1, 1, 1);
        var capped = new BlockPos(FAR_X, 1, 1);
        place(helper, free, 3);
        place(helper, capped, 2);
        for (var base : new BlockPos[] {free, capped}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int h = 3; h <= 6; h++) helper.setBlock(base.offset(dx, h, dz), Blocks.STONE);
                }
            }
        }
        var player = player(helper, true);
        helper.useBlock(free, player);
        helper.useBlock(capped, player);
        var freeBe = entity(helper, free);
        var cappedBe = entity(helper, capped);
        helper.runAfterDelay(150, () -> {
            var kite = freeBe.kite();
            helper.assertTrue(kite != null && kite.altitude() > 20, "the tier 3 kite is stopped by the roof: " + (kite == null ? -1 : kite.altitude()));
            helper.assertTrue(kite.getY() >= helper.absolutePos(free).getY() + 1, "the kite is inside its generator block");
            helper.assertTrue(freeBe.limit() == KiteMath.Limit.NONE, "unexpected limit " + freeBe.limit());
            helper.assertTrue(freeBe.output() > 0, "no output under the roof");
            helper.assertTrue(cappedBe.kite().altitude() <= 2.01, "the tier 2 kite flies through the roof: " + cappedBe.kite().altitude());
            helper.assertTrue(freeBe.kite(1) == null, "tier 3 has a second kite");
            helper.succeed();
        });
    }

    /**
     * Tier 4 and 5 fly two kites on tethers from the same block: they are two different entities in slot 0 and 1, a second use of
     * the item changes nothing (no duplicate), a missing kite is replaced without touching the other one, and the output of the pair
     * stays within the peak of the tier. Tier 1 has one kite only.
     *
     * @param helper the game test helper
     */
    private static void twoKites(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        var single = new BlockPos(FAR_X, 1, 1);
        place(helper, pos, 4);
        place(helper, single, 1);
        var player = player(helper, true);
        helper.useBlock(pos, player);
        helper.useBlock(single, player);
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 0, "a second kite item was used up");
        var be = entity(helper, pos);
        var first = be.kite(0);
        var second = be.kite(1);
        helper.assertTrue(first != null && second != null && first != second, "tier 4 has not two kites");
        helper.assertTrue(first.slot() == 0 && second.slot() == 1, "wrong slots");
        helper.assertTrue(entity(helper, single).kite(1) == null, "tier 1 has a second kite");
        int kites = level.getEntities(KiteBlocks.KITE_ENTITY.get(), new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(8, 600, 8), k -> true).size();
        helper.assertTrue(kites == 2, "expected two kite entities, found " + kites);
        double peak = KiteMath.peak(EnergyCommonConfig.KITE_GENERATORS.get(3).peak(), 4);
        second.kill(level);
        helper.succeedWhen(() -> {
            var now = be.kite(1);
            helper.assertTrue(now != null && now != second && !now.isRemoved() && level.getEntity(now.getUUID()) == now, "the second kite was not replaced");
            helper.assertTrue(be.kite(0) == first && !first.isRemoved(), "the first kite was replaced");
            helper.assertTrue(now.altitude() > 5 && first.altitude() > 20, "the kites do not climb");
            helper.assertTrue(be.output() > 0 && be.output() <= peak, "wrong output " + be.output());
            helper.assertTrue(level.getEntities(KiteBlocks.KITE_ENTITY.get(), new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(8, 600, 8), k -> true).size() == 2,
                    "a duplicate kite flies");
        });
    }

    /**
     * A lightning strike on a tier 5 kite spawns a purely visual bolt and credits 1 000 000 FE to the block, one block for both kites;
     * a full buffer hands the bolt to a neighbouring drain as a burst (the push limit per tick does not apply), and without a
     * neighbour what does not fit is lost. A tier 1 kite is never struck (it is reeled in during a storm).
     *
     * @param helper the game test helper
     */
    private static void lightning(GameTestHelper helper) {
        var level = helper.getLevel();
        var lone = new BlockPos(1, 1, 1);
        var next = new BlockPos(FAR_X, 1, 1);
        place(helper, lone, 5);
        place(helper, next, 4);
        helper.setBlock(next.east(), EnergyBlocks.POWER_DRAIN.get());
        var player = player(helper, true);
        helper.useBlock(lone, player);
        helper.useBlock(next, player);
        var be = entity(helper, lone);
        var bigBe = entity(helper, next);
        int bolt = EnergyCommonConfig.kiteLightningEnergy();
        helper.assertTrue(bolt == 1_000_000, "the bolt is not 1 000 000 FE: " + bolt);
        var kite = be.kite(0);
        helper.assertTrue(kite != null, "no kite");
        var area = new net.minecraft.world.phys.AABB(kite.blockPosition()).inflate(3);
        var credit = be.strike(level, 0);
        helper.assertTrue(credit != null && credit.stored() == bolt && credit.pushed() == 0 && credit.lost() == 0, "wrong credit " + credit);
        helper.assertTrue(be.energy().getAmountAsLong() == bolt, "the buffer did not get the bolt: " + be.energy().getAmountAsLong());
        var bolts = level.getEntities(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, area, b -> true);
        helper.assertTrue(bolts.size() == 1 && bolts.get(0).getPersistentData() != null, "no bolt at the kite");
        helper.assertTrue(!(level.getBlockState(kite.blockPosition()).is(Blocks.FIRE)), "the bolt set fire");
        // a full buffer: the bolt goes to the drain next to it in one burst, far above the push limit per tick
        int capacity = EnergyCommonConfig.KITE_GENERATORS.get(3).capacity();
        bigBe.creditLightning(level, capacity);
        helper.assertTrue(bigBe.energy().getAmountAsLong() == capacity, "the buffer is not full");
        var burst = bigBe.creditLightning(level, bolt);
        helper.assertTrue(burst.stored() == 0 && burst.pushed() == bolt && burst.lost() == 0, "the bolt did not go to the drain: " + burst);
        helper.assertTrue(bolt > EnergyCommonConfig.KITE_GENERATORS.get(3).maxOutput(), "the burst is not above the push limit");
        // without a neighbour what does not fit is lost
        be.creditLightning(level, EnergyCommonConfig.KITE_GENERATORS.get(4).capacity());
        var lost = be.creditLightning(level, bolt);
        helper.assertTrue(lost.stored() == 0 && lost.pushed() == 0 && lost.lost() == bolt, "wrong overflow " + lost);
        helper.assertTrue(!KiteMath.struck(1, KiteMath.Weather.THUNDER, 0, 0), "a tier 1 kite can be struck");
        helper.succeed();
    }

    /**
     * Wire is the tether: without it a kite is put in but does not fly (output 0, status NO_WIRE); right-click with iron wire stores
     * 1 wire = 1 block, never more than the tether of the tier (the rest stays in the hand); a kite then flies no higher than the wire;
     * the wire is saved and breaking the block drops it as an item.
     *
     * @param helper the game test helper
     */
    private static void wire(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(1).get());
        var be = entity(helper, pos);
        helper.assertTrue(be.wire() == 0 && be.wireCapacity() == 16, "a generator starts with wire or has no capacity of 16");
        var kitePlayer = player(helper, true);
        helper.useBlock(pos, kitePlayer);
        helper.assertTrue(be.hasKite() && be.kite() == null, "a kite flies without wire");
        helper.assertTrue(be.diagnostics().status() == KiteBlockEntity.Status.NO_WIRE, "status " + be.diagnostics().status());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, wireStack(10));
        helper.useBlock(pos, player);
        helper.assertTrue(be.wire() == 10 && player.getMainHandItem().isEmpty(), "10 wire were not stored: " + be.wire());
        player.setItemInHand(InteractionHand.MAIN_HAND, wireStack(20));
        helper.useBlock(pos, player);
        helper.assertTrue(be.wire() == 16 && player.getMainHandItem().getCount() == 14, "the wire is not capped at the tether: " + be.wire() + ", rest "
                + player.getMainHandItem().getCount());
        helper.useBlock(pos, player);
        helper.assertTrue(be.wire() == 16 && player.getMainHandItem().getCount() == 14, "a full generator took wire");
        var saved = be.saveWithFullMetadata(level.registryAccess());
        var absolute = helper.absolutePos(pos);
        var loaded = BlockEntity.loadStatic(absolute, level.getBlockState(absolute), saved, level.registryAccess());
        helper.assertTrue(loaded instanceof KiteBlockEntity copy && copy.wire() == 16, "the wire is not saved");
        helper.assertTrue(be.diagnostics().wire() == 16 && be.diagnostics().wireCapacity() == 16, "the diagnostics do not show the wire");
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(be.kite() != null && be.kite().altitude() >= 15.5, "the kite does not fly on the full wire");
            helper.assertTrue(be.output() > 0 && be.diagnostics().status() == KiteBlockEntity.Status.FLYING, "no output " + be.diagnostics());
            helper.assertTrue(be.diagnostics().lengthFactor() > 0.2, "the length factor does not follow the wire: " + be.diagnostics());
            // breaking the block drops the wire
            level.destroyBlock(absolute, true);
            helper.assertItemEntityCountIs(wireStack(1).getItem(), pos, 2.0, 16);
            helper.succeed();
        });
    }

    /**
     * A kite only climbs as far as the wire: with 6 wire the kite of tier 4 stays at 6 blocks, not at its tether of 48.
     *
     * @param helper the game test helper
     */
    private static void shortWire(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, KiteBlocks.block(4).get());
        var be = entity(helper, pos);
        be.insertWire(6);
        helper.useBlock(pos, player(helper, true));
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(be.kite(0).altitude() <= 6.01 && be.kite(0).altitude() >= 5.5, "wrong altitude " + be.kite(0).altitude());
            helper.succeed();
        });
    }

    /**
     * Two generators with a kite and wire closer than 16 blocks block each other: both kites are discarded, the output is 0 and the
     * status is TOO_CLOSE with the distance of the other one; when one is gone the other flies again. Generators 16 blocks apart do not
     * block each other, and one without wire counts as no neighbour.
     *
     * @param helper the game test helper
     */
    private static void tooClose(GameTestHelper helper) {
        var a = new BlockPos(1, 1, 1);
        var b = new BlockPos(10, 1, 1);
        var c = new BlockPos(1, 1, 5);
        place(helper, a, 1);
        place(helper, b, 2);
        helper.setBlock(c, KiteBlocks.block(1).get());
        var player = player(helper, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KiteBlocks.KITE.get(), 3));
        helper.useBlock(c, player); // a kite but no wire: no neighbour for the others
        helper.useBlock(a, player);
        var first = entity(helper, a);
        var second = entity(helper, b);
        helper.useBlock(b, player);
        helper.runAfterDelay(60, () -> {
            for (var be : new KiteBlockEntity[] {first, second}) {
                var d = be.diagnostics();
                helper.assertTrue(d.status() == KiteBlockEntity.Status.TOO_CLOSE, "not blocked: " + d);
                helper.assertTrue(be.kite() == null && d.kites() == 0 && be.output() == 0, "a blocked generator flies or makes FE: " + d);
            }
            helper.assertTrue(Math.abs(first.diagnostics().nearestOffender() - 9) < 1e-6 && first.diagnostics().minDistance() == KiteMath.MIN_DISTANCE,
                    "wrong distance " + first.diagnostics());
            helper.setBlock(b, Blocks.AIR);
            helper.succeedWhen(() -> {
                helper.assertTrue(first.kite() != null && first.diagnostics().status() == KiteBlockEntity.Status.FLYING, "the other did not fly again: "
                        + first.diagnostics());
                helper.assertTrue(first.diagnostics().nearestOffender() < 0, "still an offender");
            });
        });
    }

    /**
     * The two kites of tier 4 and 5 stay inside +-4 blocks of the axis of the block on both axes, at least 3 blocks apart, and they do
     * wander; the single kite of tier 1 hangs on the axis.
     *
     * @param helper the game test helper
     */
    private static void play(GameTestHelper helper) {
        var pair = new BlockPos(1, 1, 1);
        var single = new BlockPos(FAR_X, 1, 1);
        place(helper, pair, 5);
        place(helper, single, 1);
        var player = player(helper, true);
        helper.useBlock(pair, player);
        helper.useBlock(single, player);
        var be = entity(helper, pair);
        var lone = entity(helper, single);
        double[] range = {99, -99};
        helper.onEachTick(() -> {
            var k0 = be.kite(0);
            var k1 = be.kite(1);
            helper.assertTrue(k0 != null && k1 != null, "the kites are missing");
            var abs = helper.absolutePos(pair);
            for (var k : new KiteEntity[] {k0, k1}) {
                helper.assertTrue(Math.abs(k.getX() - (abs.getX() + 0.5)) <= 4.001 && Math.abs(k.getZ() - (abs.getZ() + 0.5)) <= 4.001,
                        "a kite is outside the play of 4 blocks");
            }
            helper.assertTrue(Math.hypot(k0.getX() - k1.getX(), k0.getZ() - k1.getZ()) >= KiteMath.MIN_KITE_GAP - 0.001, "the kites are closer than 3 blocks");
            range[0] = Math.min(range[0], k0.getX());
            range[1] = Math.max(range[1], k0.getX());
            helper.assertTrue(offset(helper, single, lone.kite()) < 1e-6, "the single kite is not on the axis");
        });
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(range[1] - range[0] > 0.05, "the kite does not move: " + (range[1] - range[0]));
            helper.succeed();
        });
    }

    /**
     * In a thunderstorm the kites of tier 5 stay inside a horizontal radius of 6 blocks around the axis and still fly.
     *
     * @param helper the game test helper
     */
    private static void stormRadius(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        place(helper, pos, 5);
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        level.getServer().setWeatherParameters(0, 100_000, true, true);
        helper.onEachTick(() -> {
            for (int slot = 0; slot < 2; slot++) {
                var k = be.kite(slot);
                helper.assertTrue(k != null, "a kite is missing");
                helper.assertTrue(offset(helper, pos, k) <= KiteMath.THUNDER_RADIUS + 1e-6, "a kite is further than 6 blocks from the axis: " + offset(helper, pos, k));
            }
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(level.isThundering() && be.output() > 0, "no storm or no output");
            level.getServer().setWeatherParameters(6000, 0, false, false);
            helper.succeed();
        });
    }

    /**
     * A bolt that is not ours and lands within 6 blocks of the kites or their tether is credited once (to one kite, with its own
     * cooldown); a bolt further away or the visual bolt a generator spawns itself is not credited a second time.
     *
     * @param helper the game test helper
     */
    private static void bolts(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 1, 1);
        place(helper, pos, 5);
        helper.useBlock(pos, player(helper, true));
        var be = entity(helper, pos);
        be.kite(0).setAltitude(30);
        be.kite(1).setAltitude(30);
        level.getServer().setWeatherParameters(0, 100_000, true, true);
        int bolt = EnergyCommonConfig.kiteLightningEnergy();
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(level.isThundering(), "no thunderstorm");
            var abs = helper.absolutePos(pos);
            long before = be.energy().getAmountAsLong();
            spawnBolt(level, abs.getX() + 0.5 + 40, abs.getY() + 1, abs.getZ() + 0.5);
            helper.assertTrue(be.energy().getAmountAsLong() == before, "a far bolt was credited");
            // our own bolt is credited by the strike, not a second time by the event
            before = be.energy().getAmountAsLong();
            be.strike(level, 1);
            helper.assertTrue(be.energy().getAmountAsLong() - before == bolt, "the own bolt was not credited exactly once: " + (be.energy().getAmountAsLong() - before));
            // a natural bolt next to the foot of both tethers: one credit (slot 0, as slot 1 is cooling down)
            before = be.energy().getAmountAsLong();
            spawnBolt(level, abs.getX() + 0.5 + 3, abs.getY() + 1, abs.getZ() + 0.5);
            helper.assertTrue(be.energy().getAmountAsLong() - before == bolt, "a near bolt was not credited exactly once: " + (be.energy().getAmountAsLong() - before));
            // both kites are cooling down now
            before = be.energy().getAmountAsLong();
            spawnBolt(level, abs.getX() + 0.5 + 3, abs.getY() + 1, abs.getZ() + 0.5);
            helper.assertTrue(be.energy().getAmountAsLong() == before, "a bolt was credited during the cooldown");
            level.getServer().setWeatherParameters(6000, 0, false, false);
            helper.succeed();
        });
    }

    /** Spawns a bolt that does not burn or hurt (it is visual only) but is not one of ours, as natural lightning is. */
    private static void spawnBolt(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        bolt.setPos(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /**
     * The recipe of the tier 1 generator takes iron wire where it took string: with the wire in the top middle it crafts the
     * generator, with string it does not.
     *
     * @param helper the game test helper
     */
    private static void recipeWire(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        var aluminum = net.samtrion.dotcraft.resources.common.Materials.all().stream().filter(m -> m.id().equals("aluminum")).findFirst().orElseThrow();
        var redstone = net.samtrion.dotcraft.resources.common.Materials.all().stream().filter(m -> m.id().equals("redstone")).findFirst().orElseThrow();
        var gear = new ItemStack(net.samtrion.dotcraft.resources.item.ResourceItems.item(aluminum, net.samtrion.dotcraft.resources.common.Forms.get("gear")).get());
        var plate = new ItemStack(net.samtrion.dotcraft.resources.item.ResourceItems.item(aluminum, net.samtrion.dotcraft.resources.common.Forms.get("plate")).get());
        var grains = new ItemStack(net.samtrion.dotcraft.resources.item.ResourceItems.item(redstone, net.samtrion.dotcraft.resources.common.Forms.get("grains")).get());
        var battery = new ItemStack(net.samtrion.dotcraft.energy.item.EnergyItems.BATTERY_TIER_1.get());
        var with = crafting(wireStack(1), gear, plate, battery, grains);
        var found = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, with, helper.getLevel());
        helper.assertTrue(found.isPresent() && found.get().value().assemble(with).is(KiteBlocks.block(1).get().asItem()), "the generator is not crafted with iron wire");
        var without = crafting(new ItemStack(net.minecraft.world.item.Items.STRING), gear, plate, battery, grains);
        helper.assertTrue(recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, without, helper.getLevel()).isEmpty(),
                "the generator is still crafted with string");
        helper.succeed();
    }

    /** The 3x3 grid of the generator recipe: gear, tether, gear / plate, battery, plate / plate, grains, plate. */
    private static net.minecraft.world.item.crafting.CraftingInput crafting(ItemStack tether, ItemStack gear, ItemStack plate, ItemStack battery, ItemStack x) {
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(gear.copy(), tether, gear.copy(), plate.copy(), battery.copy(), plate.copy(),
                plate.copy(), x.copy(), plate.copy()));
    }
}
