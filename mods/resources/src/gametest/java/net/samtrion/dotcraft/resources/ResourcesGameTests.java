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
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.item.ResourceItems;
import net.samtrion.dotcraft.resources.common.Forms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.samtrion.dotcraft.resources.common.OreRichness;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.OreHost;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedLook;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.common.ResourceBlockTags;
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

    private static final Identifier CHARCOAL = Identifier.fromNamespaceAndPath(CraftResources.MODID, "charcoal_block");
    private static final ResourceKey<Consumer<GameTestHelper>> CHARCOAL_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, CHARCOAL);

    private static final Identifier MATERIALS = Identifier.fromNamespaceAndPath(CraftResources.MODID, "materials");
    private static final ResourceKey<Consumer<GameTestHelper>> MATERIALS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, MATERIALS);

    private static final Identifier SWITCHES = Identifier.fromNamespaceAndPath(CraftResources.MODID, "material_switches");
    private static final ResourceKey<Consumer<GameTestHelper>> SWITCHES_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, SWITCHES);

    private static final Identifier EXPOSED = Identifier.fromNamespaceAndPath(CraftResources.MODID, "exposed_ore");
    private static final ResourceKey<Consumer<GameTestHelper>> EXPOSED_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, EXPOSED);

    private static final Identifier HOSTS = Identifier.fromNamespaceAndPath(CraftResources.MODID, "ore_hosts");
    private static final ResourceKey<Consumer<GameTestHelper>> HOSTS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, HOSTS);

    private static final Identifier TOOLS = Identifier.fromNamespaceAndPath(CraftResources.MODID, "crafting_tools");
    private static final ResourceKey<Consumer<GameTestHelper>> TOOLS_FUNCTION = ResourceKey.create(Registries.TEST_FUNCTION, TOOLS);

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
            registry.register(CHARCOAL, (Consumer<GameTestHelper>) ResourcesGameTests::charcoalBlock);
            registry.register(MATERIALS, (Consumer<GameTestHelper>) ResourcesGameTests::materials);
            registry.register(SWITCHES, (Consumer<GameTestHelper>) ResourcesGameTests::materialSwitches);
            registry.register(EXPOSED, (Consumer<GameTestHelper>) ResourcesGameTests::exposedOre);
            registry.register(HOSTS, (Consumer<GameTestHelper>) ResourcesGameTests::oreHosts);
            registry.register(TOOLS, (Consumer<GameTestHelper>) ResourcesGameTests::craftingTools);
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
        event.registerTest(CHARCOAL, new FunctionGameTestInstance(CHARCOAL_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(MATERIALS, new FunctionGameTestInstance(MATERIALS_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(SWITCHES, new FunctionGameTestInstance(SWITCHES_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(EXPOSED, new FunctionGameTestInstance(EXPOSED_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        event.registerTest(TOOLS, new FunctionGameTestInstance(TOOLS_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
        event.registerTest(HOSTS, new FunctionGameTestInstance(HOSTS_FUNCTION,
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
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
                new Case("charcoal_block", 1, 16_000 * 9),
                new Case("coal_block", 1, 16_000 * 9),
                new Case("coal_block", 5, 16_000 * 59_049),
                new Case("coal_block", 6, 0),
                new Case("oak_planks", 2, 300 * 81),
                new Case("white_wool", 1, 100 * 9),
                new Case("stone", 1, 0),
                new Case("crimson_planks", 3, 0)}) {
            var stack = new ItemStack(ResourceBlocks.item(base(c.base()), c.tier()).get());
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
        var pos = new BlockPos(1, 1, 1);
        for (Case c : new Case[] {
                new Case("stone", 1, Items.WOODEN_PICKAXE, true),
                new Case("stone", 1, Items.AIR, false),
                new Case("ice", 1, Items.AIR, false),
                new Case("ice", 1, Items.WOODEN_PICKAXE, true),
                new Case("obsidian", 1, Items.IRON_PICKAXE, false),
                new Case("obsidian", 1, Items.DIAMOND_PICKAXE, true),
                new Case("obsidian", 3, Items.DIAMOND_PICKAXE, true),
                new Case("obsidian", 4, Items.DIAMOND_PICKAXE, false),
                new Case("obsidian", 4, Items.NETHERITE_PICKAXE, true)}) {
            Block block = ResourceBlocks.block(base(c.base()), c.tier()).get();
            helper.setBlock(pos, block);
            var state = helper.getBlockState(pos);
            helper.assertTrue(state.is(block), c.base() + "_" + c.tier() + " was not placed");
            var stack = new ItemStack(c.tool());
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
        var pos = new BlockPos(0, 2, 0);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hand = ItemStack.EMPTY;
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(pos, ResourceBlocks.block(ice, 3).get());
        helper.getBlockState(pos).randomTick(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getRandom());
        helper.assertBlockPresent(ResourceBlocks.block(ice, 3).get(), pos);
        for (int tier = 3; tier >= 1; tier--) {
            var state = helper.getBlockState(pos);
            state.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), state, null, hand);
            helper.assertBlockPresent(tier == 1 ? ice.block() : ResourceBlocks.block(ice, tier - 1).get(), pos);
        }
        // also without ground below
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(pos, ResourceBlocks.block(ice, 2).get());
        var floating = helper.getBlockState(pos);
        floating.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), floating, null, hand);
        helper.assertBlockPresent(ResourceBlocks.block(ice, 1).get(), pos);
        helper.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(pos, ice.block());
        var vanilla = helper.getBlockState(pos);
        vanilla.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(pos), vanilla, null, hand);
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.WATER, pos);
        helper.succeed();
    }

    /**
     * The charcoal block mines like the coal block, burns as long, is tagged as a storage block and can be crafted
     * from and back into charcoal.
     *
     * @param helper the game test helper
     */
    private static void charcoalBlock(GameTestHelper helper) {
        var block = ResourceBlocks.CHARCOAL_BLOCK.get();
        var coal = net.minecraft.world.level.block.Blocks.COAL_BLOCK;
        var state = block.defaultBlockState();
        var item = new ItemStack(ResourceBlocks.CHARCOAL_BLOCK_ITEM.get());
        helper.assertTrue(block.defaultDestroyTime() == coal.defaultDestroyTime(), "charcoal block has not the hardness of coal");
        helper.assertTrue(state.requiresCorrectToolForDrops() && state.is(BlockTags.MINEABLE_WITH_PICKAXE), "charcoal block lacks its pickaxe gate");
        helper.assertTrue(state.is(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS)
                && state.is(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL) && item.is(ResourceBlockTags.STORAGE_BLOCKS_CHARCOAL_ITEM)
                && item.is(net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS), "charcoal block lacks its storage block tags");
        int burn = item.getBurnTime(null, helper.getLevel().fuelValues());
        helper.assertTrue(burn == 16_000, "charcoal block burns " + burn);
        helper.assertTrue(block.getLootTable().isPresent()
                && helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable().get()) != LootTable.EMPTY,
                "charcoal block has no loot table");
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String recipe : new String[] {"charcoal_block", "charcoal_from_charcoal_block"}) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(CraftResources.MODID, recipe))).isPresent(),
                    "missing recipe " + recipe);
        }
        helper.succeed();
    }

    /**
     * A crafting tool stays in the grid of a craft with one more damage, breaks with the last point, and has the
     * durability of its material; the recipe of the tool exists.
     *
     * @param helper the game test helper
     */
    private static void craftingTools(GameTestHelper helper) {
        var material = Materials.all().stream().filter(m -> m.id().equals("iron")).findFirst().orElseThrow();
        var form = Forms.get("hammer");
        var hammer = new ItemStack(ResourceItems.item(material, form).get());
        helper.assertTrue(hammer.getMaxDamage() == 250, "iron hammer has the durability " + hammer.getMaxDamage());
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, java.util.List.of(hammer));
        var left = net.minecraft.world.item.crafting.CraftingRecipe.defaultCraftingReminder(input).get(0);
        helper.assertTrue(left.is(hammer.getItem()) && left.getDamageValue() == 1 && left.getCount() == 1, "the hammer is not left damaged: " + left);
        hammer.setDamageValue(hammer.getMaxDamage() - 1);
        var last = net.minecraft.world.item.crafting.CraftingRecipe.defaultCraftingReminder(
                net.minecraft.world.item.crafting.CraftingInput.of(1, 1, java.util.List.of(hammer))).get(0);
        helper.assertTrue(last.isEmpty(), "the worn hammer does not break");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(CraftResources.MODID, "material/iron/hammer"))).isPresent(), "missing the hammer recipe");
        helper.succeed();
    }

    /**
     * Every base has all tiers, with the right tool tags and drop gate, hardness, loot table, item name and
     * compress/decompress recipes.
     *
     * @param helper the game test helper
     */
    private static void compressedBlocks(GameTestHelper helper) {
        helper.assertTrue(ResourceBlocks.blocks().count() == CompressedBases.enabled().count() * CompressedTier.MAX,
                "block count is not bases x tiers");
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                Block block = ResourceBlocks.block(base, tier).get();
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
                helper.assertTrue(ResourceBlocks.item(base, tier).get().components().has(DataComponents.ITEM_NAME),
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

    /** The id of something of this mod. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CraftResources.MODID, path);
    }

    /**
     * Ore drops follow the richness (poor at most one, normal one, rich two raw items), and the alloy mix, dust
     * smelting and the ore features of tin are registered, named after the material, the ore entry (the default entry
     * of a source without ids is {@code ore_0}), the dimension and the richness.
     *
     * @param helper the game test helper
     */
    private static void materials(GameTestHelper helper) {
        var tin = Materials.all().stream().filter(m -> m.id().equals("tin")).findFirst().orElseThrow();
        var raw = ResourceItems.item(tin, Forms.get("raw")).get();
        var pos = new BlockPos(1, 1, 1);
        var pick = new ItemStack(Items.NETHERITE_PICKAXE);
        for (var richness : OreRichness.values()) {
            Block block = MaterialBlocks.ore(tin, OreHost.STONE, richness).get();
            helper.setBlock(pos, block);
            for (int i = 0; i < 20; i++) {
                var drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), null, null, pick);
                int count = drops.stream().filter(s -> s.is(raw)).mapToInt(ItemStack::getCount).sum();
                boolean ok = switch (richness) {
                    case POOR -> count <= 1 && drops.size() <= 1;
                    case NORMAL -> count == 1;
                    case RICH -> count == 2;
                };
                helper.assertTrue(ok, richness + " tin ore dropped " + drops);
            }
        }
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String recipe : new String[] {"material/bronze/dust_from_mix", "material/bronze/ingot_from_mix", "material/steel/block_from_mix_coal", "material/steel/block_from_mix_charcoal", "material/steel/ingot_from_mix_coal", "material/steel/ingot_from_mix_charcoal", "material/tin/ingot_from_dust_smelting"}) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id(recipe))).isPresent(), "missing recipe " + recipe);
        }
        var features = helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var placed = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (String dimension : new String[] {"overworld", "nether", "end"}) {
            for (String richness : new String[] {"poor", "normal", "rich"}) {
                String name = "tin_ore_0_" + dimension + "_" + richness;
                helper.assertTrue(features.get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id(name))).isPresent(),
                        "missing configured feature " + name);
                helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, id(name))).isPresent(),
                        "missing placed feature " + name);
            }
        }
        helper.assertTrue(features.get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id("tin_overworld_normal"))).isEmpty(),
                "the feature names without an ore entry are gone");
        helper.succeed();
    }

    /** Edge length of the cube of the vein tests is {@code 2 * RANGE + 1}. */
    private static final int RANGE = 5;

    /** Sets the cube around a position to a block, without neighbour updates so that no fluid starts to flow. */
    private static void fill(GameTestHelper helper, BlockPos center, int fromY, int toY, net.minecraft.world.level.block.state.BlockState state) {
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = -RANGE; z <= RANGE; z++) helper.getLevel().setBlock(center.offset(x, y, z), state, 2);
            }
        }
    }

    /** Counts the blocks of the cube around a position that are ore, and checks that each one is at the surface layer. */
    private static int countOre(GameTestHelper helper, BlockPos center, Block ore, java.util.function.Predicate<BlockPos> allowed) {
        int count = 0;
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!helper.getLevel().getBlockState(pos).is(ore)) continue;
                    helper.assertTrue(allowed.test(pos), "ore placed at " + pos.subtract(center) + " where the exposure does not allow it");
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * The exposed ore feature places a vein block only next to the listed kind of block: nothing in sealed stone, ore in the
     * top layer of stone under air (also cave air), water or lava, but only for the exposure that is listed; the config
     * survives a round trip through its codec.
     *
     * @param helper the game test helper
     */
    private static void exposedOre(GameTestHelper helper) {
        var level = helper.getLevel();
        // far above the other tests, which work in the first blocks above the floor
        BlockPos center = helper.absolutePos(new BlockPos(0, 40, 0));
        Block ore = MaterialBlocks.ore(tin(), OreHost.STONE, OreRichness.NORMAL).get();
        var feature = net.samtrion.dotcraft.resources.worldgen.ResourceFeatures.EXPOSED_ORE.get();
        record Case(String name, net.minecraft.world.level.block.state.BlockState above,
                    java.util.List<net.samtrion.dotcraft.resources.common.OreExposure> exposure, boolean places) {}
        var air = net.samtrion.dotcraft.resources.common.OreExposure.AIR;
        var lava = net.samtrion.dotcraft.resources.common.OreExposure.LAVA;
        var water = net.samtrion.dotcraft.resources.common.OreExposure.WATER;
        try {
            for (Case c : new Case[] {
                    new Case("sealed stone, air", net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), java.util.List.of(air, lava, water), false),
                    new Case("open air, air", net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), java.util.List.of(air), true),
                    new Case("open cave air, air", net.minecraft.world.level.block.Blocks.CAVE_AIR.defaultBlockState(), java.util.List.of(air), true),
                    new Case("open air, water or lava", net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), java.util.List.of(water, lava), false),
                    new Case("open water, water", net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), java.util.List.of(water), true),
                    new Case("open water, air or lava", net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), java.util.List.of(air, lava), false),
                    new Case("open lava, lava", net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), java.util.List.of(lava), true),
                    new Case("open lava, air or water", net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), java.util.List.of(air, water), false)}) {
                fill(helper, center, -RANGE, RANGE, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                fill(helper, center, 1, RANGE, c.above());
                // the vein is built where the height map says there is ground
                net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(level.getChunkAt(center),
                        java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
                var config = new net.samtrion.dotcraft.resources.worldgen.ExposedOreConfiguration(
                        new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(
                                OreHost.STONE.rule(), ore.defaultBlockState(), 8), c.exposure());
                for (int attempt = 0; attempt < 150; attempt++) {
                    feature.place(config, level, level.getChunkSource().getGenerator(), level.getRandom(), center);
                }
                int count = countOre(helper, center, ore, pos -> pos.getY() == center.getY());
                helper.assertTrue((count > 0) == c.places(), c.name() + ": " + count + " ore blocks, expected " + (c.places() ? "some" : "none"));
            }
            var config = new net.samtrion.dotcraft.resources.worldgen.ExposedOreConfiguration(
                    new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(
                            OreHost.BASALT.rule(), ore.defaultBlockState(), 6), java.util.List.of(air, water));
            var json = net.samtrion.dotcraft.resources.worldgen.ExposedOreConfiguration.CODEC
                    .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, config).getOrThrow();
            var back = net.samtrion.dotcraft.resources.worldgen.ExposedOreConfiguration.CODEC
                    .parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
            helper.assertTrue(back.exposure().equals(config.exposure()) && back.ore().size == 6
                    && json.getAsJsonObject().getAsJsonArray("exposure").size() == 2, "the exposed ore config does not round trip: " + json);
        } finally {
            fill(helper, center, -RANGE, RANGE, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /**
     * Ore is placed in basalt and blackstone (which the overworld has too) and in nothing else of the world: the rule of the
     * host replaces its own block only.
     *
     * @param helper the game test helper
     */
    private static void oreHosts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(0, 40, 0));
        Block ore = MaterialBlocks.ore(tin(), OreHost.STONE, OreRichness.NORMAL).get();
        var other = new Block[] {net.minecraft.world.level.block.Blocks.STONE, net.minecraft.world.level.block.Blocks.DEEPSLATE,
                net.minecraft.world.level.block.Blocks.NETHERRACK, net.minecraft.world.level.block.Blocks.END_STONE,
                net.minecraft.world.level.block.Blocks.BLACKSTONE, net.minecraft.world.level.block.Blocks.BASALT};
        try {
            for (OreHost host : new OreHost[] {OreHost.BASALT, OreHost.BLACKSTONE, OreHost.STONE, OreHost.END}) {
                for (Block ground : other) {
                    fill(helper, center, -RANGE, RANGE, ground.defaultBlockState());
                    net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(level.getChunkAt(center),
                            java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
                    var config = new net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration(host.rule(), ore.defaultBlockState(), 16);
                    for (int attempt = 0; attempt < 10; attempt++) {
                        net.minecraft.world.level.levelgen.feature.Feature.ORE.place(config, level, level.getChunkSource().getGenerator(),
                                level.getRandom(), center);
                    }
                    int count = countOre(helper, center, ore, pos -> true);
                    boolean matches = host.rule().test(ground.defaultBlockState(), level.getRandom());
                    helper.assertTrue((count > 0) == matches, host + " rule on " + ground + ": " + count + " ore blocks, rule matches " + matches);
                }
            }
            helper.assertTrue(OreHost.BASALT.rule().test(net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState(), level.getRandom())
                    && OreHost.BLACKSTONE.rule().test(net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState(), level.getRandom()),
                    "the basalt and blackstone rules do not match their blocks");
            // the host blocks and their tags
            for (OreHost host : new OreHost[] {OreHost.BASALT, OreHost.BLACKSTONE}) {
                helper.assertTrue(host.dimensions().contains(net.samtrion.dotcraft.resources.common.OreDimension.OVERWORLD)
                        && host.dimensions().contains(net.samtrion.dotcraft.resources.common.OreDimension.NETHER), host + " must occur in the overworld too");
            }
        } finally {
            fill(helper, center, -RANGE, RANGE, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    /**
     * With the default config every recipe of the material chain and the alloys is loaded (the conditions hold), coal and
     * charcoal have no recipe from their dust to the item, and the conditions follow the switches: a disabled form or
     * material fails its condition, which is what drops the recipes at load.
     *
     * @param helper the game test helper
     */
    private static void materialSwitches(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (var m : Materials.all()) {
            for (var to : Forms.all()) {
                for (var c : to.conversions()) {
                    if (c.kind() != net.samtrion.dotcraft.resources.common.Form.Conversion.Kind.PACK || !m.has(to) || !m.has(c.from())) continue;
                    String recipe = "material/" + m.id() + "/" + to.id() + "_from_" + c.from();
                    helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id(recipe))).isPresent(), "missing recipe " + recipe);
                }
            }
        }
        for (String coal : new String[] {"coal", "charcoal"}) {
            for (String kind : new String[] {"smelting", "blasting"}) {
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id("material/" + coal + "/ingot_from_dust_" + kind))).isEmpty(),
                        coal + " dust must not be cooked");
            }
        }
        var gear = net.samtrion.dotcraft.resources.common.conditions.ConfigCondition.ofForm("tin", "gear");
        var tin = net.samtrion.dotcraft.resources.common.conditions.ConfigCondition.ofMaterial("tin");
        var context = net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY;
        helper.assertTrue(gear.test(context) && tin.test(context), "tin must be enabled by default");
        var gearSwitch = CraftResourcesConfig.formSwitch("tin", "gear");
        gearSwitch.set(false);
        try {
            helper.assertTrue(!gear.test(context) && tin.test(context), "a disabled tin gear form must fail only its own condition");
            helper.assertTrue(!net.samtrion.dotcraft.resources.compat.HiddenItems.items().isEmpty()
                    && net.samtrion.dotcraft.resources.compat.HiddenItems.items().contains(ResourceItems.item(tin(), Forms.get("gear")).get()),
                    "the disabled tin gear is not hidden");
        } finally {
            gearSwitch.set(true);
        }
        helper.assertTrue(gear.test(context), "tin gears must be enabled again");
        helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, id("material/steel/ingot_from_mix"))).isEmpty(),
                "steel has one recipe per alternative, not a plain one");
        var charcoal = net.samtrion.dotcraft.resources.common.conditions.ConfigCondition.ofRecipe("steel", "charcoal");
        var charcoalSwitch = CraftResourcesConfig.recipeSwitch("steel", "charcoal");
        helper.assertTrue(charcoal.test(context), "steel with charcoal must be enabled by default");
        charcoalSwitch.set(false);
        try {
            helper.assertTrue(!charcoal.test(context)
                    && net.samtrion.dotcraft.resources.common.conditions.ConfigCondition.ofRecipe("steel", "coal").test(context),
                    "a disabled charcoal alternative must fail only its own condition");
        } finally {
            charcoalSwitch.set(true);
        }
        helper.succeed();
    }

    /** The tin material. */
    private static net.samtrion.dotcraft.resources.common.Material tin() {
        return Materials.all().stream().filter(m -> m.id().equals("tin")).findFirst().orElseThrow();
    }
}
