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

    private ResourcesGameTests() {}

    /**
     * Registers the test functions.
     *
     * @param event the registry event
     */
    @SubscribeEvent
    static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry ->
                registry.register(COMPRESSED, (Consumer<GameTestHelper>) ResourcesGameTests::compressedBlocks));
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
                helper.assertTrue(base.block().defaultBlockState().requiresCorrectToolForDrops() == (base.need != ToolTier.NONE),
                        name + ": the tool tier does not match whether the base block needs a tool");
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
