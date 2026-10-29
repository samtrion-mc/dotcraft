package net.samtrion.dotcraft.energy.vibration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;

/**
 * Registers the vibration generators, tiers 1-5, their items and their block entity type. A tier switched off in the
 * {@link EnergyStartupConfig} is not registered. The items are shown in the energy creative tab, which is built in
 * {@code EnergyBlocks}.
 */
public final class VibrationBlocks {
    /** The registry path prefix of the blocks, which are named {@code vibration_generator_tier_<n>}. */
    public static final String PREFIX = "vibration_generator";

    /** The register of the blocks. */
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyMod.MODID);
    /** The register of the items. */
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    /** The register of the block entity type. */
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyMod.MODID);

    /** The generator blocks of the tiers that are switched on, ordered by tier. */
    private static final List<DeferredBlock<VibrationBlock>> GENERATORS = new ArrayList<>();
    /** The items of those blocks, in the same order. */
    private static final List<DeferredItem<BlockItem>> GENERATOR_ITEMS = new ArrayList<>();

    /** The block entity type of all tiers; the blocks are listed when the registry event runs, after the loop below has filled the list. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VibrationBlockEntity>> TYPE = TYPES.register(PREFIX,
            () -> new BlockEntityType<>((pos, state) -> new VibrationBlockEntity(VibrationBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(GENERATORS.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= VibrationMath.MAX_TIER; tier++) {
            int t = tier;
            var name = PREFIX + "_tier_" + t;
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<VibrationBlock>registerBlock(name, p -> new VibrationBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
            GENERATORS.add(block);
            GENERATOR_ITEMS.add(ITEMS.<BlockItem>registerItem(name, props -> new VibrationBlockItem(block.get(), props), props -> props.useBlockDescriptionPrefix()));
        }
    }

    /** Not instantiable. */
    private VibrationBlocks() {}

    /**
     * Streams the blocks of the tiers that are switched on.
     *
     * @return the blocks, ordered by tier
     */
    public static Stream<DeferredBlock<VibrationBlock>> blocks() {
        return GENERATORS.stream();
    }

    /**
     * Streams the items of the tiers that are switched on.
     *
     * @return the items, ordered by tier
     */
    public static Stream<DeferredItem<BlockItem>> items() {
        return GENERATOR_ITEMS.stream();
    }

    /**
     * Looks up a block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<VibrationBlock> block(int tier) {
        var path = PREFIX + "_tier_" + tier;
        return GENERATORS.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
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
        bus.addListener(VibrationBlocks::registerCapabilities);
    }

    /** Exposes the buffer; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
    }
}
