package net.samtrion.dotcraft.energy.wheel;

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
 * Registers the mob wheels, tiers 1-5 (a tier switched off in the {@link EnergyStartupConfig} is not registered), their items and
 * the block entity type, and puts the items into the creative tab {@code .Craft: Energy} of {@code EnergyBlocks}.
 */
public final class WheelBlocks {
    /** The registry path prefix of the wheels, which are named {@code mob_wheel_tier_<n>}. */
    public static final String PREFIX = "mob_wheel";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);

    private static final List<DeferredBlock<WheelBlock>> WHEELS = new ArrayList<>();
    private static final List<DeferredItem<BlockItem>> WHEEL_ITEMS = new ArrayList<>();

    /** The block entity type of every wheel. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WheelBlockEntity>> TYPE = TYPES.register(PREFIX,
            () -> new BlockEntityType<>((pos, state) -> new WheelBlockEntity(WheelBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(WHEELS.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= WheelMath.MAX_TIER; tier++) {
            int t = tier;
            var name = PREFIX + "_tier_" + t;
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<WheelBlock>registerBlock(name, p -> new WheelBlock(p, t, () -> WheelBlocks.TYPE.get()),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion()
                            .isValidSpawn((state, level, pos, entityType) -> false));
            WHEELS.add(block);
            WHEEL_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        }
    }

    /** Not instantiable. */
    private WheelBlocks() {}

    /**
     * Streams every registered wheel.
     *
     * @return the blocks, ordered by tier
     */
    public static Stream<DeferredBlock<WheelBlock>> blocks() {
        return WHEELS.stream();
    }

    /**
     * Streams every registered wheel item.
     *
     * @return the items, ordered by tier
     */
    public static Stream<DeferredItem<BlockItem>> items() {
        return WHEEL_ITEMS.stream();
    }

    /**
     * Looks up a wheel.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<WheelBlock> block(int tier) {
        var path = PREFIX + "_tier_" + tier;
        return WHEELS.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
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
        bus.addListener(WheelBlocks::registerCapabilities);
        bus.addListener(WheelBlocks::fillTab);
    }

    /** Exposes the buffer of every wheel; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
    }

    /** Appends the wheels to the energy tab, which {@code EnergyBlocks} registers. */
    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> energy = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "energy"));
        if (event.getTabKey().equals(energy)) WHEEL_ITEMS.forEach(i -> event.accept(i.get()));
    }
}
