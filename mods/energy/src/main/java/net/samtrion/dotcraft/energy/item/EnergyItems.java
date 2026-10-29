package net.samtrion.dotcraft.energy.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;

/**
 * Registers the potato batteries, the data component that holds their FE, and their FE capability. A stack without the component is full.
 */
public final class EnergyItems {
    /** Lang key of the {@code x / y FE} tooltip line; the arguments are the stored FE and the capacity. */
    public static final String TOOLTIP_KEY = "tooltip." + EnergyMod.MODID + ".energy";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyMod.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnergyMod.MODID);

    /** The stored FE of a battery stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The photovoltaic cell the solar panels are made of (named Photovoltaic Cell). */
    public static final DeferredItem<Item> PHOTOVOLTAIC_CELL = ITEMS.registerSimpleItem("photovoltaic_cell");

    /** The single-use battery: gives its FE once and then becomes a baked potato; {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> POTATO_BATTERY = register("potato_battery", EnergyCommonConfig.POTATO_BATTERY);

    /** The battery that can be charged again and again; {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> RECHARGEABLE_POTATO_BATTERY = register("rechargeable_potato_battery", EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY);

    /** Tier 2 (lead), {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> BATTERY_TIER_2 = register("battery_tier_2", EnergyCommonConfig.BATTERY_TIER_2);
    /** Tier 3 (nickel), {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> BATTERY_TIER_3 = register("battery_tier_3", EnergyCommonConfig.BATTERY_TIER_3);
    /** Tier 4 (silver), {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> BATTERY_TIER_4 = register("battery_tier_4", EnergyCommonConfig.BATTERY_TIER_4);
    /** Tier 5 (platinum), {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> BATTERY_TIER_5 = register("battery_tier_5", EnergyCommonConfig.BATTERY_TIER_5);

    /**
     * Registers the item of a battery unless the config switches it off; a stack without the component is full.
     *
     * @param name    the registry path
     * @param battery its config
     * @return the item, {@code null} if the battery is switched off
     */
    private static @Nullable DeferredItem<? extends Item> register(String name, EnergyCommonConfig.Battery battery) {
        if (!EnergyStartupConfig.enabled(name)) return null;
        return ITEMS.registerItem(name, p -> new BatteryItem(p, battery), p -> p.stacksTo(1));
    }

    /** Not instantiable. */
    private EnergyItems() {}

    /**
     * Hooks the registers and listeners onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        bus.addListener(EnergyItems::registerCapabilities);
        bus.addListener(EnergyItems::addToVanillaTabs);
    }

    /**
     * Gives both batteries the FE capability; the single-use one cannot be charged and turns into a baked potato
     * when it is drained.
     *
     * @param event the capability registration event
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (POTATO_BATTERY != null) {
            event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> {
                var config = EnergyCommonConfig.POTATO_BATTERY;
                return new BatteryEnergyHandler(access, ENERGY.get(), config.capacity(), config.maxInput(), config.maxOutput(), Items.BAKED_POTATO);
            }, POTATO_BATTERY.get());
        }
        rechargeable(event, RECHARGEABLE_POTATO_BATTERY, EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY);
        rechargeable(event, BATTERY_TIER_2, EnergyCommonConfig.BATTERY_TIER_2);
        rechargeable(event, BATTERY_TIER_3, EnergyCommonConfig.BATTERY_TIER_3);
        rechargeable(event, BATTERY_TIER_4, EnergyCommonConfig.BATTERY_TIER_4);
        rechargeable(event, BATTERY_TIER_5, EnergyCommonConfig.BATTERY_TIER_5);
    }

    /** Gives a rechargeable battery its FE capability; it stays itself when drained. Does nothing if it is switched off. */
    private static void rechargeable(RegisterCapabilitiesEvent event, @Nullable DeferredItem<? extends Item> item, EnergyCommonConfig.Battery config) {
        if (item == null) return;
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new BatteryEnergyHandler(access, ENERGY.get(), config.capacity(), config.maxInput(), config.maxOutput(), null), item.get());
    }

    /**
     * Lists the fully charged batteries in tools and utilities.
     *
     * @param event the creative tab contents event
     */
    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        if (POTATO_BATTERY != null) event.accept(POTATO_BATTERY.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        for (var battery : new DeferredItem<?>[] {RECHARGEABLE_POTATO_BATTERY, BATTERY_TIER_2, BATTERY_TIER_3, BATTERY_TIER_4, BATTERY_TIER_5}) {
            if (battery != null) event.accept(battery.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
