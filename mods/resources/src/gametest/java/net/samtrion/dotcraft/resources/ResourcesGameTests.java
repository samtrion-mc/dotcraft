package net.samtrion.dotcraft.resources;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedLook;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * GameTests of the resources mod, run with {@code ./gradlew :mods:resources:runGameTestServer}.
 */
@EventBusSubscriber(modid = CraftResources.MODID)
public final class ResourcesGameTests {
    private static final Identifier COMPRESSED = Identifier.fromNamespaceAndPath(CraftResources.MODID, "compressed_blocks");
    private static final ResourceKey<Consumer<GameTestHelper>> COMPRESSED_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, COMPRESSED);

    private static final Identifier FUEL = Identifier.fromNamespaceAndPath(CraftResources.MODID, "compressed_fuel");
    private static final ResourceKey<Consumer<GameTestHelper>> FUEL_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, FUEL);
    private static final Identifier MINING = Identifier.fromNamespaceAndPath(CraftResources.MODID, "compressed_mining");
    private static final ResourceKey<Consumer<GameTestHelper>> MINING_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MINING);

    private static final Identifier ICE = Identifier.fromNamespaceAndPath(CraftResources.MODID, "compressed_ice");
    private static final ResourceKey<Consumer<GameTestHelper>> ICE_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, ICE);

    private ResourcesGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(COMPRESSED, (Consumer<GameTestHelper>) ResourcesGameTests::compressedBlocks);
            registry.register(FUEL, (Consumer<GameTestHelper>) ResourcesGameTests::compressedFuel);
            registry.register(MINING, (Consumer<GameTestHelper>) ResourcesGameTests::compressedMining);
            registry.register(ICE, (Consumer<GameTestHelper>) ResourcesGameTests::compressedIce);
        });
    }

    /**
     * Registers the test instances.
     *
     * @param event the game test registration event
     */
    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(COMPRESSED, new TestEnvironmentDefinition.AllOf());
        event.registerTest(COMPRESSED, new FunctionGameTestInstance(COMPRESSED_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(FUEL, new FunctionGameTestInstance(FUEL_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(MINING, new FunctionGameTestInstance(MINING_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(ICE, new FunctionGameTestInstance(ICE_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 40, 0, true)));
    }

    private static CompressedBase base(String path) {
        return CompressedBases.enabled().filter(b -> b.id.getPath().equals(path)).findFirst().orElseThrow();
    }

    /**
     * Compressed blocks burn as long as their base times 9^tier, and not at all once that exceeds an int; blocks whose
     * base is no fuel do not burn.
     *
     * @param helper the game test helper
     */
    private static void compressedFuel(GameTestHelper helper) {
        var fuels = helper.getLevel().fuelValues();
        record Case(String base, int tier, int expected) {}
        for (Case c : new Case[] {
                new Case("coal_block", 1, 16_000 * 9),
                new Case("coal_block", 5, 16_000 * 59_049),
                new Case("coal_block", 6, 0),
                new Case("oak_planks", 2, 300 * 81),
                new Case("white_wool", 1, 100 * 9),
                new Case("stone", 1, 0),
                new Case("crimson_planks", 3, 0)}) {
            var stack = new net.minecraft.world.item.ItemStack(CompressedBlocks.item(base(c.base()), c.tier()).get());
            int burn = stack.getBurnTime(null, fuels);
            helper.assertTrue(burn == c.expected(), c.base() + "_" + c.tier() + " burns " + burn + ", expected " + c.expected());
        }
        helper.succeed();
    }

    /**
     * A placed compressed block drops itself only with a tool of its tier: obsidian tier 1 needs a diamond
     * pickaxe, tier 4 a netherite one; stone tier 1 needs a wooden pickaxe.
     *
     * @param helper the game test helper
     */
    private static void compressedMining(GameTestHelper helper) {
        record Case(String base, int tier, net.minecraft.world.item.Item tool, boolean drops) {}
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        for (Case c : new Case[] {
                new Case("stone", 1, net.minecraft.world.item.Items.WOODEN_PICKAXE, true),
                new Case("stone", 1, net.minecraft.world.item.Items.AIR, false),
                new Case("ice", 1, net.minecraft.world.item.Items.AIR, false),
                new Case("ice", 1, net.minecraft.world.item.Items.WOODEN_PICKAXE, true),
                new Case("obsidian", 1, net.minecraft.world.item.Items.IRON_PICKAXE, false),
                new Case("obsidian", 1, net.minecraft.world.item.Items.DIAMOND_PICKAXE, true),
                new Case("obsidian", 3, net.minecraft.world.item.Items.DIAMOND_PICKAXE, true),
                new Case("obsidian", 4, net.minecraft.world.item.Items.DIAMOND_PICKAXE, false),
                new Case("obsidian", 4, net.minecraft.world.item.Items.NETHERITE_PICKAXE, true)}) {
            Block block = CompressedBlocks.block(base(c.base()), c.tier()).get();
            helper.setBlock(pos, block);
            var state = helper.getBlockState(pos);
            helper.assertTrue(state.is(block), c.base() + "_" + c.tier() + " was not placed");
            var stack = new net.minecraft.world.item.ItemStack(c.tool());
            // the tool tier decides whether the player gets the drop, the loot table itself only holds the block
            boolean harvests = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
            helper.assertTrue(harvests == c.drops(), c.base() + "_" + c.tier() + " with " + c.tool() + " harvests " + harvests);
            var drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(pos), null, null, stack);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()), c.base() + "_" + c.tier() + " does not drop itself: " + drops);
        }
        helper.succeed();
    }

    /**
     * Compressed ice broken without Silk Touch over a solid block melts one tier down, tier 1 into vanilla ice, which melts into water; the random
     * tick without any light leaves it alone.
     *
     * @param helper the game test helper
     */
    private static void compressedIce(GameTestHelper helper) {
        var ice = base("ice");
        var pos = new net.minecraft.core.BlockPos(0, 2, 0);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hand = net.minecraft.world.item.ItemStack.EMPTY;
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(pos, CompressedBlocks.block(ice, 3).get());
        helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getRandom());
        helper.assertBlockPresent(CompressedBlocks.block(ice, 3).get(), pos);
        for (int tier = 3; tier >= 1; tier--) {
            var state = helper.getBlockState(pos);
            state.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), state, null, hand);
            helper.assertBlockPresent(tier == 1 ? ice.block() : CompressedBlocks.block(ice, tier - 1).get(), pos);
        }
        // also without ground below
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(pos, CompressedBlocks.block(ice, 2).get());
        var floating = helper.getBlockState(pos);
        floating.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), floating, null, hand);
        helper.assertBlockPresent(CompressedBlocks.block(ice, 1).get(), pos);
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(pos, ice.block());
        var vanilla = helper.getBlockState(pos);
        vanilla.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), vanilla, null, hand);
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.WATER, pos);
        helper.succeed();
    }

    /**
     * Every base has all tiers, with the right tool tags and drop gate, hardness, loot table, item name and
     * compress/decompress recipes.
     *
     * @param helper the game test helper
     */
    private static void compressedBlocks(GameTestHelper helper) {
        helper.assertTrue(CompressedBlocks.blocks().count() == CompressedBases.enabled().count() * CompressedTier.MAX,
                "block count is not bases x tiers");
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = CompressedBlocks.block(base, tier).get();
                String name = base.path(tier);
                helper.assertTrue(block.defaultBlockState().requiresCorrectToolForDrops() == (base.need != ToolTier.NONE),
                        name + ": the tool tier does not match whether the block needs a tool");
                for (ToolTier t : ToolTier.materials().toList()) {
                    helper.assertTrue(block.defaultBlockState().is(t.incorrectTag()) == !t.canMine(base.required(tier)),
                            name + " has the wrong drop gate for " + t);
                }
                helper.assertTrue(block.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS) == (base.look == CompressedLook.LOG),
                        name + " has the wrong axis state");
                helper.assertTrue((base.tool.tag() == null || block.defaultBlockState().is(base.tool.tag()))
                        && (base.required(tier).needsTag() == null || block.defaultBlockState().is(base.required(tier).needsTag())), name + " lacks its tool tags");
                helper.assertTrue(block.defaultDestroyTime() == CompressedTier.hardness(base.block().defaultDestroyTime(), tier),
                        name + " has the wrong hardness");
                helper.assertTrue(block.getLootTable().isPresent()
                        && helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable().get()) != LootTable.EMPTY,
                        name + " has no loot table");
                helper.assertTrue(CompressedBlocks.item(base, tier).get().components().has(net.minecraft.core.component.DataComponents.ITEM_NAME),
                        name + " item has no name");
                for (String kind : new String[] {"compress", "decompress"}) {
                    ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                            Identifier.fromNamespaceAndPath(CraftResources.MODID, base.recipePath(kind, tier)));
                    helper.assertTrue(recipes.byKey(key).isPresent(), "missing recipe " + key.identifier());
                }
            }
        }
        helper.succeed();
    }
}
