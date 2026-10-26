package net.samtrion.dotcraft.resources.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreHost;
import net.samtrion.dotcraft.resources.common.OreRichness;

/**
 * Registers the plain items of all {@link Materials} (every form that is not a block), and the creative tab
 * {@code materials} (which lists only the forms the config enables when it is opened), and is the one place to look up the item of any form, block forms included.
 */
public final class ResourceItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CraftResources.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftResources.MODID);

    private static final Map<String, DeferredItem<? extends Item>> BY_PATH = new LinkedHashMap<>();
    private static final Map<Material, List<DeferredItem<? extends Item>>> BY_MATERIAL = new LinkedHashMap<>();

    static {
        for (Material material : Materials.all()) {
            List<DeferredItem<? extends Item>> items = new ArrayList<>();
            for (Form form : material.forms()) {
                if (form.hosted()) {
                    for (Material.OreVariant variant : material.oreVariants()) {
                        items.add(MaterialBlocks.oreItem(material, variant.host(), variant.richness()));
                    }
                } else if (form.isBlock()) {
                    items.add(MaterialBlocks.item(material, form));
                } else {
                    items.add(ITEMS.registerSimpleItem(material.path(form)));
                }
            }
            items.forEach(i -> BY_PATH.put(i.getId().getPath(), i));
            BY_MATERIAL.put(material, items);
        }
        TABS.register("materials", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + CraftResources.MODID + ".materials"))
                .icon(() -> new ItemStack(Materials.all().stream().findFirst()
                        .map(m -> (Item) item(m, iconForm(m)).get())
                        .orElse(Items.IRON_INGOT)))
                .displayItems((params, output) -> Materials.all().forEach(m -> m.forms().stream()
                        .filter(f -> CraftResourcesConfig.formEnabled(m.id(), f.id()))
                        .forEach(f -> items(m, f).forEach(i -> output.accept(i.get())))))
                .build());
    }

    /** Not instantiable. */
    private ResourceItems() {}

    /** The form shown as tab icon: the first {@link Form#icon() icon} form of the material, else its first form. */
    private static Form iconForm(Material material) {
        return material.forms().stream().filter(f -> f.icon() && !f.hosted()).findFirst()
                .orElseGet(() -> material.forms().stream().filter(f -> !f.hosted()).findFirst().orElseThrow());
    }

    /**
     * Hooks the registers onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    /**
     * Looks up the item of a form that is not hosted; for block forms the block item.
     *
     * @param material the material
     * @param form     the form, which the material must have
     * @return the item
     */
    public static DeferredItem<? extends Item> item(Material material, Form form) {
        return BY_PATH.get(material.path(form));
    }

    /**
     * Looks up the item of an ore block.
     *
     * @param material the material
     * @param host     the block the ore sits in
     * @param richness how much the ore yields
     * @return the block item of the ore
     */
    public static DeferredItem<? extends Item> ore(Material material, OreHost host, OreRichness richness) {
        return BY_PATH.get(material.orePath(host, richness));
    }

    /**
     * Streams all items of a material, in creative tab order.
     *
     * @param material the material
     * @return its items, block items included
     */
    public static Stream<DeferredItem<? extends Item>> items(Material material) {
        return BY_MATERIAL.get(material).stream();
    }

    /**
     * Streams the items of one form of a material: the item or block item, or all ore variants of a hosted form.
     *
     * @param material the material
     * @param form     a form the material has
     * @return the items of the form
     */
    public static Stream<DeferredItem<? extends Item>> items(Material material, Form form) {
        if (form.hosted()) {
            return material.oreVariants().stream()
                    .<DeferredItem<? extends Item>>map(v -> ore(material, v.host(), v.richness()));
        }
        return Stream.of(item(material, form));
    }

    /**
     * Streams all material items.
     *
     * @return the items, ordered by material, form and ore variant
     */
    public static Stream<DeferredItem<? extends Item>> items() {
        return BY_MATERIAL.values().stream().flatMap(List::stream);
    }
}
