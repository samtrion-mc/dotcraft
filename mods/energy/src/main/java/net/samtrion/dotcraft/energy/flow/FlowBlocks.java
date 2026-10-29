package net.samtrion.dotcraft.energy.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;

/**
 * Registers the flow generators, tiers 1-5, their items and their block entity type. A tier that is switched off in the
 * {@link EnergyStartupConfig} is not registered. The items are added to the creative tab of the energy mod, which the
 * {@code EnergyBlocks} class registers.
 */
public final class FlowBlocks {
    /** The config of the five tiers, tier 1 first. */
    public static final List<EnergyCommonConfig.Generator> CONFIGS = EnergyCommonConfig.FLOW_GENERATORS;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);

    private static final List<DeferredBlock<FlowGeneratorBlock>> GENERATORS = new ArrayList<>();
    private static final List<DeferredItem<FlowGeneratorItem>> GENERATOR_ITEMS = new ArrayList<>();

    /** The block entity type of every flow generator. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlowGeneratorBlockEntity>> TYPE = TYPES.register("flow_generator",
            () -> new BlockEntityType<>((pos, state) -> new FlowGeneratorBlockEntity(FlowBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(GENERATORS.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= FlowMath.MAX_TIER; tier++) {
            int t = tier;
            var name = name(t);
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<FlowGeneratorBlock>registerBlock(name, p -> new FlowGeneratorBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion());
            GENERATORS.add(block);
            GENERATOR_ITEMS.add(ITEMS.registerItem(name, p -> new FlowGeneratorItem(block.get(), p.useBlockDescriptionPrefix())));
        }
    }

    /** Not instantiable. */
    private FlowBlocks() {}

    /**
     * The registry path of a tier.
     *
     * @param tier the tier, 1..5
     * @return {@code flow_generator_tier_<tier>}
     */
    public static String name(int tier) {
        return "flow_generator_tier_" + tier;
    }

    /**
     * Streams the registered blocks, tier 1 first.
     *
     * @return the blocks of the tiers that are switched on
     */
    public static Stream<DeferredBlock<FlowGeneratorBlock>> blocks() {
        return GENERATORS.stream();
    }

    /**
     * Streams the registered items, tier 1 first.
     *
     * @return the items of the tiers that are switched on
     */
    public static Stream<DeferredItem<FlowGeneratorItem>> items() {
        return GENERATOR_ITEMS.stream();
    }

    /**
     * Looks up a block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<FlowGeneratorBlock> block(int tier) {
        var path = name(tier);
        return GENERATORS.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
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
        bus.addListener(FlowBlocks::registerCapabilities);
        bus.addListener(FlowBlocks::fillTab);
    }

    /** Exposes the buffer on the four sides across the flow axis (not front or back); it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> FlowGeneratorBlock.connects(be.getBlockState(), side) ? be.energy() : null);
    }

    /** Appends the generators to the creative tab of the energy mod. */
    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> energy = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "energy"));
        if (event.getTabKey().equals(energy)) items().forEach(i -> event.accept(i.get()));
    }
}
