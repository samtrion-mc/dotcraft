package net.samtrion.dotcraft.resources.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.resources.CraftResources;

/**
 * Registers {@link CompressedTier#MAX} compressed blocks, their items and a creative tab for every
 * {@link CompressedBase}.
 */
public final class CompressedBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CraftResources.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CraftResources.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftResources.MODID);

    private static final Map<CompressedBase, List<DeferredBlock<Block>>> BLOCKS_BY_BASE = new LinkedHashMap<>();
    private static final Map<CompressedBase, List<DeferredItem<BlockItem>>> ITEMS_BY_BASE = new LinkedHashMap<>();

    static {
        for (CompressedBase base : CompressedBases.enabled().toList()) {
            List<DeferredBlock<Block>> blocks = new ArrayList<>();
            List<DeferredItem<BlockItem>> items = new ArrayList<>();
            for (int tier = 1; tier <= CompressedTier.MAX; tier++) {
                int t = tier;
                var block = BLOCKS.<Block>registerBlock(base.path(t), p -> base.look == CompressedLook.LOG
                        ? new CompressedPillarBlock(p, base.block(), t) : new CompressedBlock(p, base.block(), t), () -> properties(base, t));
                blocks.add(block);
                items.add(ITEMS.registerSimpleBlockItem(block,
                        p -> p.component(DataComponents.ITEM_NAME, CompressedBlock.name(base.block(), t))));
            }
            BLOCKS_BY_BASE.put(base, blocks);
            ITEMS_BY_BASE.put(base, items);
        }
        TABS.register("compressed", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + CraftResources.MODID + ".compressed"))
                .icon(() -> new ItemStack(item(CompressedBases.enabled().findFirst().orElseThrow(), CompressedTier.MAX).get()))
                .displayItems((params, output) -> items().forEach(i -> output.accept(i.get())))
                .build());
    }

    private CompressedBlocks() {}

    @SuppressWarnings("deprecation") // ofFullCopy would also copy the base block's loot table and name
    private static BlockBehaviour.Properties properties(CompressedBase base, int tier) {
        return BlockBehaviour.Properties.ofLegacyCopy(base.block())
                .mapColor(base.block().defaultMapColor()) // the copied function of logs reads the axis property
                .destroyTime(CompressedTier.hardness(base.block().defaultDestroyTime(), tier));
    }

    /**
     * Hooks all registers onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    /**
     * Looks up one compressed block.
     *
     * @param base the base block
     * @param tier the tier, {@code 1..MAX}
     * @return the compressed block
     */
    public static DeferredBlock<Block> block(CompressedBase base, int tier) {
        return BLOCKS_BY_BASE.get(base).get(tier - 1);
    }

    /**
     * Looks up the item of one compressed block.
     *
     * @param base the base block
     * @param tier the tier, {@code 1..MAX}
     * @return the item of the compressed block
     */
    public static DeferredItem<BlockItem> item(CompressedBase base, int tier) {
        return ITEMS_BY_BASE.get(base).get(tier - 1);
    }

    /**
     * Streams all compressed blocks.
     *
     * @return the blocks, ordered by base and tier
     */
    public static Stream<DeferredBlock<Block>> blocks() {
        return BLOCKS_BY_BASE.values().stream().flatMap(List::stream);
    }

    /**
     * Streams all compressed block items.
     *
     * @return the items, ordered by base and tier
     */
    public static Stream<DeferredItem<BlockItem>> items() {
        return ITEMS_BY_BASE.values().stream().flatMap(List::stream);
    }
}
