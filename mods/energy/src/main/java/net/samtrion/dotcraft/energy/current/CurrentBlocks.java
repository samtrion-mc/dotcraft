package net.samtrion.dotcraft.energy.current;

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
 * Registers the current generators, tiers 1-5, their items and their block entity type. A tier that is switched off in the
 * {@link EnergyStartupConfig} is not registered. The items are added to the creative tab of the energy mod, which the
 * {@code EnergyBlocks} class registers.
 */
public final class CurrentBlocks {
    /** The config of the five tiers, tier 1 first. */
    public static final List<EnergyCommonConfig.Generator> CONFIGS = EnergyCommonConfig.CURRENT_GENERATORS;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);

    private static final List<DeferredBlock<CurrentGeneratorBlock>> GENERATORS = new ArrayList<>();
    private static final List<DeferredItem<CurrentGeneratorItem>> GENERATOR_ITEMS = new ArrayList<>();

    /** The block entity type of every current generator. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CurrentGeneratorBlockEntity>> TYPE = TYPES.register("current_generator",
            () -> new BlockEntityType<>((pos, state) -> new CurrentGeneratorBlockEntity(CurrentBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(GENERATORS.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= CurrentMath.MAX_TIER; tier++) {
            int t = tier;
            var name = name(t);
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<CurrentGeneratorBlock>registerBlock(name, p -> new CurrentGeneratorBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion());
            GENERATORS.add(block);
            GENERATOR_ITEMS.add(ITEMS.registerItem(name, p -> new CurrentGeneratorItem(block.get(), p.useBlockDescriptionPrefix())));
        }
    }

    /** Not instantiable. */
    private CurrentBlocks() {}

    /**
     * The registry path of a tier.
     *
     * @param tier the tier, 1..5
     * @return {@code current_generator_tier_<tier>}
     */
    public static String name(int tier) {
        return "current_generator_tier_" + tier;
    }

    /**
     * Streams the registered blocks, tier 1 first.
     *
     * @return the blocks of the tiers that are switched on
     */
    public static Stream<DeferredBlock<CurrentGeneratorBlock>> blocks() {
        return GENERATORS.stream();
    }

    /**
     * Streams the registered items, tier 1 first.
     *
     * @return the items of the tiers that are switched on
     */
    public static Stream<DeferredItem<CurrentGeneratorItem>> items() {
        return GENERATOR_ITEMS.stream();
    }

    /**
     * Looks up a block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<CurrentGeneratorBlock> block(int tier) {
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
        bus.addListener(CurrentBlocks::registerCapabilities);
        bus.addListener(CurrentBlocks::fillTab);
    }

    /** Exposes the buffer; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
    }

    /** Appends the generators to the creative tab of the energy mod. */
    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> energy = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "energy"));
        if (event.getTabKey().equals(energy)) items().forEach(i -> event.accept(i.get()));
    }
}
