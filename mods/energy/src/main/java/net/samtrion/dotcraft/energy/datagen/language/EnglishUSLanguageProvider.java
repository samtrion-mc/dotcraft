package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.current.CurrentBlocks;
import net.samtrion.dotcraft.energy.current.CurrentGeneratorItem;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
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
        add(EnergyBlocks.POWER_DRAIN.get(), "Power Drain");
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energy");
        add(CurrentGeneratorItem.FORMULA_KEY, "Needs water: peak x water share x biome x (1 + animals)");
        add(CurrentGeneratorItem.PERK_KEY + 1, "Baseline: needs a nearly full cube of water");
        add(CurrentGeneratorItem.PERK_KEY + 2, "Wide net: animals count within 6 blocks");
        add(CurrentGeneratorItem.PERK_KEY + 3, "Adaptation: keeps 75% outside ocean and river");
        add(CurrentGeneratorItem.PERK_KEY + 4, "Lean water: a small pond is already enough");
        add(CurrentGeneratorItem.PERK_KEY + 5, "Abyssal: independent of the biome");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
