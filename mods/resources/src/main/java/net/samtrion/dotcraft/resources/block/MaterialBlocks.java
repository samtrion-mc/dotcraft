package net.samtrion.dotcraft.resources.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreHost;
import net.samtrion.dotcraft.resources.common.OreRichness;

/**
 * Registers the blocks and block items of all {@link Materials}: for every material the block {@link Form}s it
 * has, with the properties the form source names, and the ore blocks of {@link Material#oreVariants()}, with the
 * properties of their host. All of them are plain {@link Block}s; the behavior comes from data (loot tables,
 * tags). The plain item forms are registered by {@code ResourceItems}.
 */
public final class MaterialBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CraftResources.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CraftResources.MODID);

    /** The map colors a form source can name. */
    private static final Map<String, MapColor> MAP_COLORS = Map.of(
            "metal", MapColor.METAL, "raw_iron", MapColor.RAW_IRON, "sand", MapColor.SAND);
    /** The sound types a form source can name. */
    private static final Map<String, SoundType> SOUNDS = Map.of("metal", SoundType.METAL, "sand", SoundType.SAND);

    private static final Map<String, DeferredBlock<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> ITEMS_BY_PATH = new LinkedHashMap<>();
    private static final Map<Material, List<DeferredBlock<Block>>> BLOCKS_BY_MATERIAL = new LinkedHashMap<>();
    private static final Map<Material, List<DeferredBlock<Block>>> TINTED_BY_MATERIAL = new LinkedHashMap<>();

    static {
        for (Material material : Materials.all()) {
            List<DeferredBlock<Block>> blocks = new ArrayList<>();
            List<DeferredBlock<Block>> tinted = new ArrayList<>();
            for (Form form : material.forms()) {
                List<DeferredBlock<Block>> added = new ArrayList<>();
                if (form.hosted()) {
                    for (Material.OreVariant variant : material.oreVariants()) {
                        added.add(add(material.orePath(variant.host(), variant.richness()), () -> ore(variant.host())));
                    }
                } else if (form.isBlock()) {
                    added.add(add(material.path(form), () -> properties(form)));
                }
                blocks.addAll(added);
                if (form.tinted()) tinted.addAll(added);
            }
            BLOCKS_BY_MATERIAL.put(material, blocks);
            TINTED_BY_MATERIAL.put(material, tinted);
        }
    }

    /** Not instantiable. */
    private MaterialBlocks() {}

    /** Registers a block with its item and remembers it. */
    private static DeferredBlock<Block> add(String path, Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(path, properties);
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    /** The block properties of a form that is not hosted, as its source names them. */
    private static BlockBehaviour.Properties properties(Form form) {
        Form.BlockSpec spec = form.block().orElseThrow();
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(named(MAP_COLORS, "map color", form, spec.mapColor().orElseThrow()));
        spec.sound().ifPresent(sound -> properties.sound(named(SOUNDS, "sound", form, sound)));
        if (spec.requiresTool()) properties.requiresCorrectToolForDrops();
        float hardness = spec.hardness().orElseThrow();
        return properties.strength(hardness, spec.resistance().orElse(hardness));
    }

    /** Looks up a name of a form source, which fails with a message that names the form. */
    private static <T> T named(Map<String, T> table, String what, Form form, String name) {
        T value = table.get(name);
        if (value == null) throw new IllegalArgumentException(form.id() + ": unknown " + what + " \"" + name + "\", known: " + table.keySet());
        return value;
    }

    /** The block properties of an ore in a host. */
    private static BlockBehaviour.Properties ore(OreHost host) {
        return switch (host) {
            case STONE -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F);
            case DEEPSLATE -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops().strength(4.5F, 3.0F);
            case NETHER -> BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).sound(SoundType.NETHER_ORE)
                    .requiresCorrectToolForDrops().strength(3.0F, 3.0F);
            case END -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops()
                    .strength(3.0F, 9.0F);
            case BLACKSTONE -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F);
            case BASALT -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops().strength(3.0F, 3.0F);
        };
    }

    /**
     * Hooks the registers onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    /**
     * Looks up a block of a form that is not hosted.
     *
     * @param material the material
     * @param form     a block form that the material has
     * @return the block
     */
    public static DeferredBlock<Block> block(Material material, Form form) {
        return BLOCKS_BY_PATH.get(material.path(form));
    }

    /**
     * Looks up an ore block.
     *
     * @param material the material
     * @param host     the block the ore sits in
     * @param richness how much the ore yields
     * @return the ore block
     */
    public static DeferredBlock<Block> ore(Material material, OreHost host, OreRichness richness) {
        return BLOCKS_BY_PATH.get(material.orePath(host, richness));
    }

    /**
     * Looks up the item of a block of a form that is not hosted.
     *
     * @param material the material
     * @param form     the block form
     * @return the block item
     */
    public static DeferredItem<BlockItem> item(Material material, Form form) {
        return ITEMS_BY_PATH.get(material.path(form));
    }

    /**
     * Looks up the item of an ore block.
     *
     * @param material the material
     * @param host     the block the ore sits in
     * @param richness how much the ore yields
     * @return the block item
     */
    public static DeferredItem<BlockItem> oreItem(Material material, OreHost host, OreRichness richness) {
        return ITEMS_BY_PATH.get(material.orePath(host, richness));
    }

    /**
     * Streams all blocks of a material, in creative tab order.
     *
     * @param material the material
     * @return its blocks
     */
    public static Stream<DeferredBlock<Block>> blocks(Material material) {
        return BLOCKS_BY_MATERIAL.get(material).stream();
    }

    /**
     * Streams the blocks of a material whose form is {@link Form#tinted() tinted}, which get the tint of the
     * material in the world.
     *
     * @param material the material
     * @return its tinted blocks
     */
    public static Stream<DeferredBlock<Block>> tintedBlocks(Material material) {
        return TINTED_BY_MATERIAL.get(material).stream();
    }

    /**
     * Streams all material blocks.
     *
     * @return the blocks, ordered by material, form and ore variant
     */
    public static Stream<DeferredBlock<Block>> blocks() {
        return BLOCKS_BY_MATERIAL.values().stream().flatMap(List::stream);
    }
}
