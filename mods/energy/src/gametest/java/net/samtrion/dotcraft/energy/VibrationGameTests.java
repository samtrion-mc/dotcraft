package net.samtrion.dotcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;
import net.samtrion.dotcraft.energy.vibration.VibrationBlockEntity;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.vibration.VibrationMath;

/**
 * GameTests of the vibration generators, run with {@code ./gradlew :mods:energy:runGameTestServer}. The events are emitted with
 * {@code level.gameEvent}; every assertion on the FE of a generator compares the buffer before and after one synchronous emit, so
 * events of other tests cannot disturb it.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class VibrationGameTests {
    private static final Identifier LISTEN = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "vibration_listen");
    private static final ResourceKey<Consumer<GameTestHelper>> LISTEN_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, LISTEN);

    private static final Identifier BLOCKS = Identifier.fromNamespaceAndPath(EnergyMod.MODID, "vibration_blocks");
    private static final ResourceKey<Consumer<GameTestHelper>> BLOCKS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, BLOCKS);

    private VibrationGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(LISTEN, (Consumer<GameTestHelper>) VibrationGameTests::listen);
            registry.register(BLOCKS, (Consumer<GameTestHelper>) VibrationGameTests::blocks);
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var listenEnvironment = event.registerEnvironment(LISTEN, new TestEnvironmentDefinition.AllOf());
        event.registerTest(LISTEN, new FunctionGameTestInstance(LISTEN_FUNCTION,
                new TestData<>(listenEnvironment, Identifier.withDefaultNamespace("empty"), 140, 0, true)));
        var blocksEnvironment = event.registerEnvironment(BLOCKS, new TestEnvironmentDefinition.AllOf());
        event.registerTest(BLOCKS, new FunctionGameTestInstance(BLOCKS_FUNCTION,
                new TestData<>(blocksEnvironment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static long stored(GameTestHelper helper, BlockPos generator) {
        return ((VibrationBlockEntity) helper.getLevel().getBlockEntity(generator)).energy().getAmountAsLong();
    }

    /** Emits a game event and returns the FE the generator gained from it. */
    private static long emit(GameTestHelper helper, BlockPos generator, Holder<GameEvent> event, BlockPos at, GameEvent.Context context) {
        long before = stored(helper, generator);
        helper.getLevel().gameEvent(event, Vec3.atCenterOf(at), context);
        return stored(helper, generator) - before;
    }

    /**
     * One generator after the other (tier 1, 4, 5) at the same spot, each with an empty bucket that fills for 25 ticks first. Tier 1: no
     * FE for events that are not in the tag, too faint, outside the radius or on wool (also stopped by wool in between); then five
     * loud events in one tick give exactly the burst of the bucket, and the buffer can be drained but not filled. Tier 4 hears
     * through wool and has the larger radius; tier 5 ignores a sneaking entity for the events the sneaking tag lists.
     *
     * @param helper the game test helper
     */
    private static void listen(GameTestHelper helper) {
        var rel = new BlockPos(1, 2, 1);
        var gen = helper.absolutePos(rel);
        var destroy = GameEvent.BLOCK_DESTROY;
        var none = GameEvent.Context.of((net.minecraft.world.entity.Entity) null);
        helper.setBlock(rel, VibrationBlocks.block(1).get());
        helper.assertTrue(helper.getLevel().getBlockEntity(gen) instanceof VibrationBlockEntity, "no vibration block entity");
        // an event before the bucket has run for a while gives nothing, it only starts the clock
        helper.assertTrue(emit(helper, gen, destroy, gen.east(2), none) == 0, "a fresh generator made FE");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(emit(helper, gen, GameEvent.CONTAINER_OPEN, gen.east(2), none) == 0, "container_open is no source");
            helper.assertTrue(emit(helper, gen, GameEvent.HIT_GROUND, gen.east(2), none) == 0, "hit_ground is too faint for tier 1");
            helper.assertTrue(emit(helper, gen, destroy, gen.east(8), none) == 0, "an event outside the radius counted");
            helper.assertTrue(emit(helper, gen, destroy, gen.east(2), GameEvent.Context.of(Blocks.WHITE_WOOL.defaultBlockState())) == 0,
                    "an event on a block that dampens vibrations counted");
            helper.setBlock(rel.east(1), Blocks.WHITE_WOOL);
            helper.assertTrue(emit(helper, gen, destroy, gen.east(3), none) == 0, "wool did not stop the vibration of tier 1");
            helper.setBlock(rel.east(1), Blocks.AIR);
            // 5 x 120 FE wanted, the bucket holds 300 (20 ticks of 15 FE/t)
            long made = 0;
            for (int i = 0; i < 5; i++) made += emit(helper, gen, destroy, gen.east(2), none);
            helper.assertTrue(made == VibrationMath.burst(EnergyCommonConfig.VIBRATION_GENERATORS.get(0).peak()), "the burst was " + made);
            var bucket = ((VibrationBlockEntity) helper.getLevel().getBlockEntity(gen)).bucket();
            helper.assertTrue(bucket.tokens() == 0, "the bucket is not empty: " + bucket.tokens());
            for (Direction side : Direction.values()) {
                EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, gen, side);
                helper.assertTrue(handler != null, "no capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, "the generator accepted FE on " + side);
                    helper.assertTrue(handler.extract(1000, tx) == Math.min(made, EnergyCommonConfig.VIBRATION_GENERATORS.get(0).maxOutput()), "extraction not limited on " + side);
                }
            }
            tier4(helper, rel, gen);
        });
    }

    private static void tier4(GameTestHelper helper, BlockPos rel, BlockPos gen) {
        var destroy = GameEvent.BLOCK_DESTROY;
        var none = GameEvent.Context.of((net.minecraft.world.entity.Entity) null);
        helper.setBlock(rel, VibrationBlocks.block(4).get());
        emit(helper, gen, destroy, gen.east(2), none);
        helper.runAfterDelay(25, () -> {
            helper.setBlock(rel.east(1), Blocks.WHITE_WOOL);
            helper.assertTrue(emit(helper, gen, destroy, gen.east(3), none) > 0, "tier 4 did not hear through wool");
            helper.setBlock(rel.east(1), Blocks.AIR);
            helper.assertTrue(emit(helper, gen, destroy, gen.east(VibrationMath.radius(4) - 1), none) > 0, "tier 4 did not hear its radius");
            helper.assertTrue(emit(helper, gen, destroy, gen.east(VibrationMath.radius(4) + 2), none) == 0, "tier 4 heard beyond its radius");
            helper.assertTrue(emit(helper, gen, GameEvent.HIT_GROUND, gen.east(2), none) > 0, "tier 4 does not hear a faint landing");
            helper.assertTrue(emit(helper, gen, GameEvent.STEP, gen.east(2), none) == 0, "tier 4 heard a step");
            tier5(helper, rel, gen);
        });
    }

    private static void tier5(GameTestHelper helper, BlockPos rel, BlockPos gen) {
        helper.setBlock(rel, VibrationBlocks.block(5).get());
        var quiet = helper.makeMockPlayer(GameType.SURVIVAL);
        quiet.setShiftKeyDown(true);
        var loud = helper.makeMockPlayer(GameType.SURVIVAL);
        emit(helper, gen, GameEvent.BLOCK_DESTROY, gen.east(2), GameEvent.Context.of((net.minecraft.world.entity.Entity) null));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(emit(helper, gen, GameEvent.STEP, gen.east(2), GameEvent.Context.of(quiet)) == 0, "a sneaking entity was heard");
            helper.assertTrue(emit(helper, gen, GameEvent.STEP, gen.east(2), GameEvent.Context.of(loud)) > 0, "tier 5 did not hear a step");
            helper.assertTrue(emit(helper, gen, GameEvent.BLOCK_DESTROY, gen.east(2), GameEvent.Context.of(quiet)) > 0, "a sneaking entity that breaks a block was not heard");
            helper.succeed();
        });
    }

    /**
     * Every enabled tier has a block entity of the vibration type with the radius of the tier and an energy capability on every side
     * that cannot be filled; it is no panel, so it neither joins the pool of the panels nor shows as a neighbour of one.
     *
     * @param helper the game test helper
     */
    private static void blocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var blocks = VibrationBlocks.blocks().toList();
        helper.assertTrue(blocks.size() == 5, "expected 5 vibration generators, found " + blocks.size());
        int x = 0;
        for (var block : blocks) helper.setBlock(new BlockPos(2 * x++, 12, 0), block.get());
        var panel = EnergyBlocks.SOLAR.block(1).get();
        helper.setBlock(new BlockPos(-1, 12, 0), panel);
        var panelPos = helper.absolutePos(new BlockPos(-1, 12, 0));
        helper.assertTrue(!level.getBlockState(panelPos).getValue(GeneratorBlock.EAST), "the panel shows a vibration generator as a panel neighbour");
        x = 0;
        for (var block : blocks) {
            var pos = helper.absolutePos(new BlockPos(2 * x++, 12, 0));
            var be = level.getBlockEntity(pos);
            helper.assertTrue(be instanceof VibrationBlockEntity, block.getId() + " has the wrong block entity");
            var listener = ((VibrationBlockEntity) be).getListener();
            helper.assertTrue(listener.getListenerRadius() == VibrationMath.radius(block.get().tier()), block.getId() + " has the wrong radius");
            for (Direction side : Direction.values()) {
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
                helper.assertTrue(handler != null, block.getId() + " has no capability on " + side);
                try (var tx = Transaction.openRoot()) {
                    helper.assertTrue(handler.insert(1000, tx) == 0, block.getId() + " accepted energy");
                }
                helper.assertTrue(handler.getCapacityAsInt() == EnergyCommonConfig.VIBRATION_GENERATORS.get(block.get().tier() - 1).capacity(), block.getId() + " has the wrong buffer");
            }
        }
        helper.succeed();
    }
}
