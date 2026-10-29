package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.vibration.VibrationBlockItem;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.resources.common.Locales;

/** The German translations. Every key added here needs the same key in {@link EnglishUSLanguageProvider}. */
public final class GermanLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public GermanLanguageProvider(PackOutput output) {
        super(output, EnergyMod.MODID, Locales.DE_DE);
    }

    @Override
    protected void addTranslations() {
        add(EnergyItems.POTATO_BATTERY.get(), "Kartoffelbatterie");
        add(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), "Wiederaufladbare Kartoffelbatterie");
        add(EnergyItems.BATTERY_TIER_2.get(), "Batterie (Stufe 2)");
        add(EnergyItems.BATTERY_TIER_3.get(), "Batterie (Stufe 3)");
        add(EnergyItems.BATTERY_TIER_4.get(), "Batterie (Stufe 4)");
        add(EnergyItems.BATTERY_TIER_5.get(), "Batterie (Stufe 5)");
        add(EnergyItems.PHOTOVOLTAIC_CELL.get(), "Photovoltaikzelle");
        EnergyBlocks.SOLAR.blocks().forEach(b -> add(b.get(), "Solarpanel (Stufe " + b.get().tier() + ")"));
        EnergyBlocks.LUNAR.blocks().forEach(b -> add(b.get(), "Mondpanel (Stufe " + b.get().tier() + ")"));
        VibrationBlocks.blocks().forEach(b -> add(b.get(), "Vibrationsgenerator (Stufe " + b.get().tier() + ")"));
        add(VibrationBlockItem.NEEDS_KEY, "Wird von Erschütterungen in der Nähe angetrieben: Blöcke abbauen, Kämpfe, Explosionen");
        add(VibrationBlockItem.PEAK_KEY, "Spitze %1$s FE/t, im Schnitt höchstens %2$s FE/t");
        add(VibrationBlockItem.HEARING_KEY, "Hört Erschütterungen im Umkreis von %1$s Blöcken, ab Vibrationsfrequenz %2$s");
        add(VibrationBlockItem.PIERCE_KEY, "Hört durch Wolle");
        add(EnergyBlocks.POWER_DRAIN.get(), "Energie-Senke");
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energie");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
