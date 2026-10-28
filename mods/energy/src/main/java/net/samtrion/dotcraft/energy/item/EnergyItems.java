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

    /** The single-use battery: gives its FE once and then becomes a baked potato; {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> POTATO_BATTERY = register("potato_battery", EnergyCommonConfig.POTATO_BATTERY);

    /** The battery that can be charged again and again; {@code null} when switched off in the config. */
    public static final @Nullable DeferredItem<? extends Item> RECHARGEABLE_POTATO_BATTERY = register("rechargeable_potato_battery", EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY);

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
        if (RECHARGEABLE_POTATO_BATTERY != null) {
            event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> {
                var config = EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY;
                return new BatteryEnergyHandler(access, ENERGY.get(), config.capacity(), config.maxInput(), config.maxOutput(), null);
            }, RECHARGEABLE_POTATO_BATTERY.get());
        }
    }

    /**
     * Lists the fully charged batteries in tools and utilities.
     *
     * @param event the creative tab contents event
     */
    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        if (POTATO_BATTERY != null) event.accept(POTATO_BATTERY.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        if (RECHARGEABLE_POTATO_BATTERY != null) event.accept(RECHARGEABLE_POTATO_BATTERY.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
