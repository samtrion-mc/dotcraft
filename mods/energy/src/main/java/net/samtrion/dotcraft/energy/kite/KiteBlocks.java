package net.samtrion.dotcraft.energy.kite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
 * Registers the kite wind generators (tiers 1-5, a tier switched off in the {@link EnergyStartupConfig} is not registered), their
 * items, the kite item, the block entity type and the kite entity type. The items go into the existing energy creative tab through
 * the tab contents event, so the tab itself is not touched.
 */
public final class KiteBlocks {
    /** The mod id, the namespace of everything registered here. */
    public static final String MODID = EnergyMod.MODID;

    /** The blocks. */
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    /** The items. */
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    /** The block entity types. */
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    /** The entity types. */
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MODID);

    /** The registered generator blocks, by tier. */
    private static final List<DeferredBlock<KiteWindBlock>> BLOCK_LIST = new ArrayList<>();
    /** The items of the registered generator blocks, by tier. */
    private static final List<DeferredItem<KiteGeneratorItem>> ITEM_LIST = new ArrayList<>();

    /** The kite item: put into a generator it flies on a tether. */
    public static final DeferredItem<Item> KITE = ITEMS.registerSimpleItem("kite");

    /** The kite entity; not saved and not summonable, the block entity owns it. */
    public static final DeferredHolder<EntityType<?>, EntityType<KiteEntity>> KITE_ENTITY = ENTITIES.registerEntityType("kite", KiteEntity::new,
            MobCategory.MISC, b -> b.sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(1).noSave().noSummon().fireImmune().noLootTable());

    /** The block entity type of all tiers. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KiteBlockEntity>> TYPE = TYPES.register("kite_generator",
            () -> new BlockEntityType<>((pos, state) -> new KiteBlockEntity(KiteBlocks.TYPE.get(), pos, state),
                    Set.<Block>copyOf(BLOCK_LIST.stream().map(b -> (Block) b.get()).toList())));

    static {
        for (int tier = 1; tier <= KiteMath.MAX_TIER; tier++) {
            int t = tier;
            var name = EnergyCommonConfig.KITE_NAMES.get(t - 1);
            if (!EnergyStartupConfig.enabled(name)) continue;
            var block = BLOCKS.<KiteWindBlock>registerBlock(name, p -> new KiteWindBlock(p, t, TYPE),
                    () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion());
            BLOCK_LIST.add(block);
            ITEM_LIST.add(ITEMS.registerItem(name, p -> new KiteGeneratorItem(block.get(), p, t), () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    /** Not instantiable. */
    private KiteBlocks() {}

    /**
     * Streams the generator blocks that are switched on.
     *
     * @return the blocks, ordered by tier
     */
    public static Stream<DeferredBlock<KiteWindBlock>> blocks() {
        return BLOCK_LIST.stream();
    }

    /**
     * Looks up a generator block.
     *
     * @param tier the tier, 1..5
     * @return the block, or {@code null} if the tier is switched off
     */
    public static @Nullable DeferredBlock<KiteWindBlock> block(int tier) {
        var path = EnergyCommonConfig.KITE_NAMES.get(tier - 1);
        return BLOCK_LIST.stream().filter(b -> b.getId().getPath().equals(path)).findFirst().orElse(null);
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
        ENTITIES.register(bus);
        bus.addListener(KiteBlocks::registerCapabilities);
        bus.addListener(KiteBlocks::fillTab);
    }

    /** Exposes the buffer; it can only be drained. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TYPE.get(), (be, side) -> be.energy());
    }

    /** Adds the kite and the generators to the energy tab (registered in the generator package). */
    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().identifier().equals(Identifier.fromNamespaceAndPath(MODID, "energy"))) return;
        event.accept(KITE.get());
        ITEM_LIST.forEach(i -> event.accept(i.get()));
    }
}
