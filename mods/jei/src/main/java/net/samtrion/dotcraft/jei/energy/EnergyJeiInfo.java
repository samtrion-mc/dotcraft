package net.samtrion.dotcraft.jei.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.flow.FlowBlocks;
import net.samtrion.dotcraft.energy.flow.FlowMath;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.generator.GeneratorBlockEntity;
import net.samtrion.dotcraft.energy.generator.GeneratorMath;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;
import net.samtrion.dotcraft.energy.kite.KiteMath;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.energy.melt.MeltMath;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.energy.thermo.ThermoMath;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.vibration.VibrationMath;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.jei.energy.GeneratorInfo.Family;

/**
 * Registers the info pages of the energy mod's generators: for every generator block item the text of its family and the
 * perk of its tier. The numbers come from the constants and the config of the energy mod, so a pack that tunes the
 * config sees its own values. This class links the energy mod and is only loaded when that mod is present.
 */
final class EnergyJeiInfo {
    private static final int TIERS = GeneratorInfo.MAX_TIER;

    /** Not instantiable. */
    private EnergyJeiInfo() {}

    /**
     * Adds the info page of every generator item that is registered (a tier switched off in the startup config has none).
     *
     * @param registration the JEI recipe registration
     */
    static void register(IRecipeRegistration registration) {
        for (var block : EnergyBlocks.SOLAR.blocks()) page(registration, Family.SOLAR, block.get().tier(), block.get());
        for (var block : EnergyBlocks.LUNAR.blocks()) page(registration, Family.LUNAR, block.get().tier(), block.get());
        FlowBlocks.blocks().forEach(b -> page(registration, Family.FLOW, b.get().tier(), b.get()));
        VibrationBlocks.blocks().forEach(b -> page(registration, Family.VIBRATION, b.get().tier(), b.get()));
        ThermoBlocks.blocks().forEach(b -> page(registration, Family.THERMO, b.get().tier(), b.get()));
        MeltBlocks.blocks().forEach(b -> page(registration, Family.MELT, b.get().tier(), b.get()));
        WheelBlocks.blocks().forEach(b -> page(registration, Family.WHEEL, b.get().tier(), b.get()));
        KiteBlocks.blocks().forEach(b -> page(registration, Family.KITE, b.get().tier(), b.get()));
        page(registration, Family.DRAIN, 0, EnergyBlocks.POWER_DRAIN_ITEM.get());
    }

    /**
     * Registers the mob wheel yield category.
     *
     * @param registration the JEI category registration
     */
    static void registerWheelCategory(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WheelYieldCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /**
     * Makes every registered wheel a catalyst of the yield category.
     *
     * @param registration the JEI catalyst registration
     */
    static void registerWheelCatalysts(IRecipeCatalystRegistration registration) {
        WheelYieldCategory.wheels().forEach(w -> registration.addCraftingStation(WheelYieldCategory.TYPE, w));
    }

    /**
     * Adds the yield entries, one per eligible mob type.
     *
     * @param registration the JEI recipe registration
     */
    static void registerWheelYield(IRecipeRegistration registration) {
        registration.addRecipes(WheelYieldCategory.TYPE, WheelYieldCategory.entries());
    }

    private static void page(IRecipeRegistration registration, Family family, int tier, ItemLike item) {
        var lines = new ArrayList<Component>();
        Object[][] arguments = arguments(family);
        for (int line = 1; line <= family.lines(); line++) lines.add(Component.translatable(family.lineKey(line), arguments[line - 1]));
        if (family.hasPerks()) lines.add(Component.translatable(GeneratorInfo.PERK_HEADER, tier, Component.translatable(family.perkKey(tier))));
        registration.addItemStackInfo(new ItemStack(item), lines.toArray(Component[]::new));
    }

    /** The format arguments of every line of a family, the line {@code n} at index {@code n - 1}. */
    private static Object[][] arguments(Family family) {
        return switch (family) {
            case SOLAR -> new Object[][] {{}, {GeneratorMath.BIOME_BOOST, GeneratorMath.BIOME_MALUS},
                    {ladder(EnergyCommonConfig.SOLAR_PANELS), buffers(EnergyCommonConfig.SOLAR_PANELS)},
                    {rain()}, {GeneratorBlockEntity.MAX_GROUP}, {percent(GeneratorMath.WATERLOGGED_FACTOR)}};
            case LUNAR -> new Object[][] {{}, {GeneratorMath.BIOME_BOOST, GeneratorMath.BIOME_MALUS},
                    {ladder(EnergyCommonConfig.LUNAR_PANELS), buffers(EnergyCommonConfig.LUNAR_PANELS)},
                    {perTier(t -> percent(GeneratorMath.lunar(t, 180F, 4, true, GeneratorMath.Weather.CLEAR, 1F, 0)))},
                    {rain()}, {GeneratorBlockEntity.MAX_GROUP}, {percent(GeneratorMath.WATERLOGGED_FACTOR)}};
            case FLOW -> new Object[][] {{}, {}, {ladder(EnergyCommonConfig.FLOW_GENERATORS), buffers(EnergyCommonConfig.FLOW_GENERATORS)},
                    {perTier(FlowMath::fullWeight), FlowMath.SOURCE_WEIGHT, FlowMath.FLOWING_WEIGHT}, {},
                    {FlowMath.FAUNA_TYPES, percent(FlowMath.FAUNA_STEP), percent(FlowMath.FAUNA_MAX), FlowMath.FAUNA_RADIUS_FAR, FlowMath.FAUNA_RADIUS_NEAR},
                    {percent(FlowMath.OFF_BIOME), percent(FlowMath.ADAPTED_BIOME)}};
            case VIBRATION -> new Object[][] {{}, {}, {ladder(EnergyCommonConfig.VIBRATION_GENERATORS), perTier(t -> percent(VibrationMath.duty(t)))},
                    {perTier(VibrationMath::radius), perTier(VibrationMath::minFrequency)}, {VibrationMath.BOSS_FACTOR}, {VibrationMath.BURST_TICKS}};
            case THERMO -> new Object[][] {{}, {}, {ThermoMath.MAX_LEVEL, ThermoMath.MIN_DELTA, 2 * ThermoMath.MAX_LEVEL},
                    {ladder(EnergyCommonConfig.THERMO_GENERATORS), perTier(ThermoMath::ceiling)}, {}};
            case MELT -> new Object[][] {{}, {}, {EnergyCommonConfig.MELT_YIELD.ice(), EnergyCommonConfig.MELT_YIELD.packedIce(),
                    EnergyCommonConfig.MELT_YIELD.blueIce(), ladder(EnergyCommonConfig.MELT_GENERATORS)},
                    {MeltMath.TANK_CAPACITY_MB / MeltMath.BUCKET_MB, percent(MeltMath.FULL_TANK_EFFICIENCY)}};
            case WHEEL -> new Object[][] {{}, {}, {ladder(EnergyCommonConfig.MOB_WHEELS)}};
            case KITE -> new Object[][] {{}, {KiteMath.MIN_DISTANCE}, {perTier(KiteMath::tether)},
                    {ladder(EnergyCommonConfig.KITE_GENERATORS), KiteMath.REFERENCE_LENGTH}, {(int) KiteMath.SPREAD}, {KiteMath.MIN_DISTANCE},
                    {(int) KiteMath.THUNDER_RADIUS, number(EnergyCommonConfig.kiteLightningEnergy())}};
            case DRAIN -> new Object[][] {{}};
        };
    }

    /** The peaks of the tiers, joined. */
    private static String ladder(List<EnergyCommonConfig.Generator> generators) {
        return perTier(t -> number(generators.get(t - 1).peak()));
    }

    /** The buffers of the tiers, joined. */
    private static String buffers(List<EnergyCommonConfig.Generator> generators) {
        return perTier(t -> number(generators.get(t - 1).capacity()));
    }

    /** The share of the output that stays in rain, per tier. */
    private static String rain() {
        return perTier(t -> percent(GeneratorMath.weather(t, GeneratorMath.Weather.RAIN)));
    }

    /** One value per tier 1 to 5, joined with a slash. */
    private static String perTier(IntFunction<Object> value) {
        return IntStream.rangeClosed(1, TIERS).mapToObj(t -> String.valueOf(value.apply(t))).collect(Collectors.joining(" / "));
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String percent(double share) {
        return Math.round(share * 100) + "%";
    }
}
