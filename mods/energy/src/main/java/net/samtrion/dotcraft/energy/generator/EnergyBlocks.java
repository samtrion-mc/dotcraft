package net.samtrion.dotcraft.energy.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.transfer.energy.VoidingEnergyHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;
import net.samtrion.dotcraft.energy.item.EnergyItems;

/**
 * Registers the power drain and the generator blocks of every family, tiers 1-5, their items, one block entity type per family and the creative tab.
 * A block switched off in the {@link EnergyStartupConfig} is not registered.
 */
public final class EnergyBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnergyMod.MODID);

    private static final List<Family> FAMILIES = new ArrayList<>();

    /** The solar panels. */
    public static final Family SOLAR = family(SolarKind.INSTANCE);

    /**
     * Creative-only test block: takes any FE from any side, up to {@link Integer#MAX_VALUE} per operation, pulls FE out of its neighbours on every side, and destroys it all. Not
     * craftable, unbreakable in survival (like a command block) and without a drop, so it only comes from the creative tab.
     */
    public static final DeferredBlock<PowerDrainBlock> POWER_DRAIN = BLOCKS.registerBlock("power_drain",
            p -> new PowerDrainBlock(p, () -> EnergyBlocks.POWER_DRAIN_TYPE.get()),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3_600_000.0F).noLootTable());

    /** The block entity type of {@link #POWER_DRAIN}, which pulls FE out of its neighbours. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerDrainBlockEntity>> POWER_DRAIN_TYPE = TYPES.register("power_drain",
            () -> new BlockEntityType<>((pos, state) -> new PowerDrainBlockEntity(EnergyBlocks.POWER_DRAIN_TYPE.get(), pos, state), Set.<Block>of(POWER_DRAIN.get())));

    /** Item of {@link #POWER_DRAIN}. */
    public static final DeferredItem<BlockItem> POWER_DRAIN_ITEM = ITEMS.registerSimpleBlockItem(POWER_DRAIN);

    static {
        TABS.register("energy", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + EnergyMod.MODID + ".energy"))
                .icon(() -> new ItemStack(SOLAR.itemStream().findFirst().orElseThrow().get()))
                .displayItems((params, output) -> { output.accept(EnergyItems.PHOTOVOLTAIC_CELL.get()); output.accept(POWER_DRAIN_ITEM.get()); FAMILIES.forEach(f -> f.itemStream().forEach(i -> output.accept(i.get()))); })
                .build());
    }

    /** Not instantiable. */
    private EnergyBlocks() {}

    /**
     * One family: its block entity type, and the blocks and items of the tiers that are switched on.
     *
     * @param kind  the family
     * @param type  the block entity type
     * @param blocks the blocks, ordered by tier; a switched off tier is missing
     * @param items  the items of the blocks, in the same order
     */
    public record Family(GeneratorKind kind, DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> type,
            List<DeferredBlock<GeneratorBlock>> blocks, List<DeferredItem<BlockItem>> items) {
        /**
         * Streams the items of the tiers that are switched on.
         *
         * @return the items
         */
        public Stream<DeferredItem<BlockItem>> itemStream() {
            return items.stream();
        }

        /**
         * Looks up a block.
         *
         * @param tier the tier, 1..5
         * @return the block, or {@code null} if the tier is switched off
         */
        public @org.jspecify.annotations.Nullable DeferredBlock<GeneratorBlock> block(int tier) {
            var path = kind.prefix() + "_tier_" + tier;
            return blocks.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
        }
    }

    private static Family family(GeneratorKind kind) {
        var blocks = new ArrayList<DeferredBlock<GeneratorBlock>>();
        var items = new ArrayList<DeferredItem<BlockItem>>();
        // the blocks are listed when the registry event runs, after this method has filled the list
        DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> type = TYPES.register(kind.prefix(),
                () -> new BlockEntityType<>((pos, state) -> new GeneratorBlockEntity(((GeneratorBlock) state.getBlock()).type(), pos, state),
                        Set.<Block>copyOf(blocks.stream().map(b -> (Block) b.get()).toList())));
        for (int tier = 1; tier <= GeneratorMath.MAX_TIER; tier++) {
            int t = tier;
            var name = kind.prefix() + "_tier_" + t;
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<GeneratorBlock>registerBlock(name, p -> new GeneratorBlock(p, kind, t, type),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion()
                            .isValidSpawn((state, level, pos, entityType) -> false));
            blocks.add(block);
            items.add(ITEMS.registerSimpleBlockItem(block));
        }
        var family = new Family(kind, type, List.copyOf(blocks), List.copyOf(items));
        FAMILIES.add(family);
        return family;
    }

    /**
     * Streams every registered generator block.
     *
     * @return the blocks of all families
     */
    public static Stream<DeferredBlock<GeneratorBlock>> blocks() {
        return FAMILIES.stream().flatMap(f -> f.blocks().stream());
    }

    /**
     * Streams every registered generator item.
     *
     * @return the items of all families
     */
    public static Stream<DeferredItem<BlockItem>> items() {
        return FAMILIES.stream().flatMap(Family::itemStream);
    }

    /**
     * Hooks the registers and the capability onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        TABS.register(bus);
        bus.addListener(EnergyBlocks::registerCapabilities);
    }

    /** Exposes the buffer of every family; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var family : FAMILIES) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, family.type().get(), (be, side) -> be.energy());
        }
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> VoidingEnergyHandler.INSTANCE, POWER_DRAIN.get());
    }
}
