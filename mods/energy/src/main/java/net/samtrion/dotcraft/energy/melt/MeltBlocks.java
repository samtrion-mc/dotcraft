package net.samtrion.dotcraft.energy.melt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;

/**
 * Registers the five melt generators, their items and their block entity type. A tier switched off in the
 * {@link EnergyStartupConfig} is not registered. The items go into the creative tab of the energy mod through an event, so this
 * family owns its registration.
 */
public final class MeltBlocks {
    /** The registry path prefix of the blocks, which are named {@code melt_generator_tier_<n>}. */
    public static final String PREFIX = "melt_generator";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);
    private static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(EnergyMod.MODID, "energy"));

    private static final List<DeferredBlock<MeltBlock>> LIST = new ArrayList<>();
    private static final List<DeferredItem<MeltGeneratorItem>> ITEM_LIST = new ArrayList<>();

    /** The block entity type of all melt generators. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeltBlockEntity>> TYPE = TYPES.register(PREFIX,
            () -> new BlockEntityType<>((pos, state) -> new MeltBlockEntity(MeltBlocks.TYPE.get(), pos, state),
                    // the blocks are listed when the registry event runs, after the static initializer has filled the list
                    Set.<Block>copyOf(LIST.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= MeltMath.TIER_COUNT; tier++) {
            int t = tier;
            var name = PREFIX + "_tier_" + t;
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<MeltBlock>registerBlock(name, p -> new MeltBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                            .lightLevel(s -> s.getValue(MeltBlock.LIT) ? 7 : 0));
            LIST.add(block);
            ITEM_LIST.add(ITEMS.registerItem(name, p -> new MeltGeneratorItem(block.get(), p, t), () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    /** Not instantiable. */
    private MeltBlocks() {}

    /**
     * Streams the registered melt generator blocks.
     *
     * @return the blocks, tier 1 first; a switched off tier is missing
     */
    public static Stream<DeferredBlock<MeltBlock>> blocks() {
        return LIST.stream();
    }

    /**
     * Streams the items of the registered melt generators.
     *
     * @return the items, in the order of the blocks
     */
    public static Stream<DeferredItem<MeltGeneratorItem>> items() {
        return ITEM_LIST.stream();
    }

    /**
     * Looks up a block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<MeltBlock> block(int tier) {
        var path = PREFIX + "_tier_" + tier;
        return LIST.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
    }

    /**
     * Hooks the registers, the capability and the creative tab entries onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        bus.addListener(MeltBlocks::registerCapabilities);
        bus.addListener(MeltBlocks::tab);
    }

    /** Exposes the buffer and the water tank; both can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TYPE.get(), (be, side) -> be.tank());
    }

    /** Adds the items to the creative tab of the energy mod. */
    private static void tab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(TAB)) ITEM_LIST.forEach(i -> event.accept(i.get()));
    }
}
