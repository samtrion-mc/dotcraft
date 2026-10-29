package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.current.CurrentBlocks;
import net.samtrion.dotcraft.energy.current.CurrentGeneratorItem;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.vibration.VibrationBlockItem;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.thermo.ThermoBlockItem;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.resources.common.Locales;

/** The English translations. Every key added here needs the same key in {@link GermanLanguageProvider}. */
public final class EnglishUSLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public EnglishUSLanguageProvider(PackOutput output) {
        super(output, EnergyMod.MODID, Locales.EN_US);
    }

    @Override
    protected void addTranslations() {
        add(EnergyItems.POTATO_BATTERY.get(), "Potato Battery");
        add(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), "Rechargeable Potato Battery");
        add(EnergyItems.BATTERY_TIER_2.get(), "Battery (Tier 2)");
        add(EnergyItems.BATTERY_TIER_3.get(), "Battery (Tier 3)");
        add(EnergyItems.BATTERY_TIER_4.get(), "Battery (Tier 4)");
        add(EnergyItems.BATTERY_TIER_5.get(), "Battery (Tier 5)");
        add(EnergyItems.PHOTOVOLTAIC_CELL.get(), "Photovoltaic Cell");
        EnergyBlocks.SOLAR.blocks().forEach(b -> add(b.get(), "Solar Panel (Tier " + b.get().tier() + ")"));
        EnergyBlocks.LUNAR.blocks().forEach(b -> add(b.get(), "Lunar Panel (Tier " + b.get().tier() + ")"));
        CurrentBlocks.blocks().forEach(b -> add(b.get(), "Current Generator (Tier " + b.get().tier() + ")"));
        VibrationBlocks.blocks().forEach(b -> add(b.get(), "Vibration Generator (Tier " + b.get().tier() + ")"));
        add(VibrationBlockItem.NEEDS_KEY, "Powered by shocks nearby: blocks broken, fights, explosions");
        add(VibrationBlockItem.PEAK_KEY, "Peak %1$s FE/t, on average at most %2$s FE/t");
        add(VibrationBlockItem.HEARING_KEY, "Hears shocks within %1$s blocks, from vibration frequency %2$s");
        add(VibrationBlockItem.PIERCE_KEY, "Hears through wool");
        add(EnergyBlocks.POWER_DRAIN.get(), "Power Drain");
        MeltBlocks.blocks().forEach(b -> add(b.get(), "Melt Generator (Tier " + b.get().tier() + ")"));
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energy");
        add(CurrentGeneratorItem.FORMULA_KEY, "Needs water: peak x water share x biome x (1 + animals)");
        add(CurrentGeneratorItem.PERK_KEY + 1, "Baseline: needs a nearly full cube of water");
        add(CurrentGeneratorItem.PERK_KEY + 2, "Wide net: animals count within 6 blocks");
        add(CurrentGeneratorItem.PERK_KEY + 3, "Adaptation: keeps 75% outside ocean and river");
        add(CurrentGeneratorItem.PERK_KEY + 4, "Lean water: a small pond is already enough");
        add(CurrentGeneratorItem.PERK_KEY + 5, "Abyssal: independent of the biome");
        ThermoBlocks.blocks().forEach(b -> add(b.get(), "Thermoelectric Generator (Tier " + b.get().tier() + ")"));
        add(ThermoBlockItem.TOOLTIP_KEY, "Peak %1$s FE/t at a temperature difference of %2$s");
        add(ThermoBlockItem.HINT_KEY, "Needs a hot and a cold neighbour (levels 1-3 each, they add up to the difference)");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
