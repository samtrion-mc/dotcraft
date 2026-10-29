package net.samtrion.dotcraft.energy.thermo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
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
 * Registers the thermoelectric generators, tiers 1-5, their items and their block entity type; the items go into the creative tab
 * of the energy mod. A tier switched off in the {@link EnergyStartupConfig} is not registered.
 */
public final class ThermoBlocks {
    /** The registry path prefix of the blocks, which are named {@code thermo_generator_tier_<n>}. */
    public static final String PREFIX = "thermo_generator";

    /** The block register. */
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    /** The item register. */
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    /** The block entity type register. */
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);

    /** The blocks of the tiers that are switched on, filled before the registry event runs. */
    private static final List<DeferredBlock<ThermoBlock>> BLOCK_LIST = new ArrayList<>();
    /** The items of {@link #BLOCK_LIST}, in the same order. */
    private static final List<DeferredItem<BlockItem>> ITEM_LIST = new ArrayList<>();

    /** The block entity type of all tiers. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermoBlockEntity>> TYPE = TYPES.register(PREFIX,
            () -> new BlockEntityType<>((pos, state) -> new ThermoBlockEntity(ThermoBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(BLOCK_LIST.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= ThermoMath.MAX_TIER; tier++) {
            int t = tier;
            var name = PREFIX + "_tier_" + t;
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<ThermoBlock>registerBlock(name, p -> new ThermoBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops());
            BLOCK_LIST.add(block);
            ITEM_LIST.add(ITEMS.<BlockItem>registerItem(name, p -> new ThermoBlockItem(block.get(), t, p.useBlockDescriptionPrefix())));
        }
    }

    /** Not instantiable. */
    private ThermoBlocks() {}

    /**
     * Streams the blocks of the tiers that are switched on.
     *
     * @return the blocks, ordered by tier
     */
    public static Stream<DeferredBlock<ThermoBlock>> blocks() {
        return BLOCK_LIST.stream();
    }

    /**
     * Streams the items of the tiers that are switched on.
     *
     * @return the items, ordered by tier
     */
    public static Stream<DeferredItem<BlockItem>> items() {
        return ITEM_LIST.stream();
    }

    /**
     * Looks up a block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<ThermoBlock> block(int tier) {
        var path = PREFIX + "_tier_" + tier;
        return BLOCK_LIST.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
    }

    /**
     * Hooks the registers, the capability and the creative tab onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        bus.addListener(ThermoBlocks::registerCapabilities);
        bus.addListener(ThermoBlocks::addToEnergyTab);
    }

    /** Exposes the buffer on every side; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
    }

    /** Adds the generators to the creative tab of the energy mod, after the content that tab already shows. */
    private static void addToEnergyTab(BuildCreativeModeTabContentsEvent event) {
        var energyTab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "energy"));
        if (event.getTabKey() != energyTab) return;
        ITEM_LIST.forEach(i -> event.accept(i.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
