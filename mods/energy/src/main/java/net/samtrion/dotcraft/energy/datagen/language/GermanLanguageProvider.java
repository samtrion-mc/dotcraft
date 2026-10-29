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
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.energy.wheel.WheelItem;
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
        CurrentBlocks.blocks().forEach(b -> add(b.get(), "Strömungsgenerator (Stufe " + b.get().tier() + ")"));
        VibrationBlocks.blocks().forEach(b -> add(b.get(), "Vibrationsgenerator (Stufe " + b.get().tier() + ")"));
        add(VibrationBlockItem.NEEDS_KEY, "Wird von Erschütterungen in der Nähe angetrieben: Blöcke abbauen, Kämpfe, Explosionen");
        add(VibrationBlockItem.PEAK_KEY, "Spitze %1$s FE/t, im Schnitt höchstens %2$s FE/t");
        add(VibrationBlockItem.HEARING_KEY, "Hört Erschütterungen im Umkreis von %1$s Blöcken, ab Vibrationsfrequenz %2$s");
        add(VibrationBlockItem.PIERCE_KEY, "Hört durch Wolle");
        WheelBlocks.blocks().forEach(b -> add(b.get(), "Mob-Laufrad (Stufe " + b.get().tier() + ")"));
        add(net.samtrion.dotcraft.energy.melt.MeltGeneratorItem.TANK_KEY, "Das Wasser landet in einem Tank mit %s Eimern, der sich nur entleeren lässt");
        add(net.samtrion.dotcraft.energy.melt.MeltGeneratorItem.FULL_KEY, "Ein voller Tank lässt %s%% der FE pro Eis übrig, bis Wasser entnommen wird");
        net.samtrion.dotcraft.energy.kite.datagen.KiteLanguage.german(this);
        add(EnergyBlocks.POWER_DRAIN.get(), "Energie-Senke");
        MeltBlocks.blocks().forEach(b -> add(b.get(), "Schmelzgenerator (Stufe " + b.get().tier() + ")"));
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energy");
        add(CurrentGeneratorItem.FORMULA_KEY, "Braucht Wasser: Spitze x Wasseranteil x Biom x (1 + Tiere)");
        add(CurrentGeneratorItem.AXIS_KEY, "Vorder- und Rückseite müssen für das Wasser frei bleiben (ein fester Block dort: 0 FE); gibt FE an die anderen Seiten");
        add(CurrentGeneratorItem.PERK_KEY + 1, "Basis: braucht einen fast vollen Wasserwürfel");
        add(CurrentGeneratorItem.PERK_KEY + 2, "Weites Netz: Tiere zählen im Umkreis von 6 Blöcken");
        add(CurrentGeneratorItem.PERK_KEY + 3, "Anpassung: behält 75% außerhalb von Ozean und Fluss");
        add(CurrentGeneratorItem.PERK_KEY + 4, "Schlankes Wasser: ein kleiner Teich genügt schon");
        add(CurrentGeneratorItem.PERK_KEY + 5, "Abyssal: unabhängig vom Biom");
        ThermoBlocks.blocks().forEach(b -> add(b.get(), "Thermoelektrischer Generator (Stufe " + b.get().tier() + ")"));
        add(ThermoBlockItem.TOOLTIP_KEY, "Spitze %1$s FE/t bei einem Temperaturunterschied von %2$s");
        add(ThermoBlockItem.HINT_KEY, "Braucht einen heissen und einen kalten Nachbarn auf gegenüberliegenden Seiten (je Stufe 1-3, sie ergeben zusammen den Unterschied)");
        add(WheelItem.STATS_KEY, "Bis zu %1$s FE/t, Zonenradius %2$s, bis zu %3$s Mobs");
        add(WheelItem.PERK_KEY + 1, "Basisrad.");
        add(WheelItem.PERK_KEY + 2, "Größerer Käfig: mehr Mobs und eine größere Zone.");
        add(WheelItem.PERK_KEY + 3, "Kleine Tiere zählen voll.");
        add(WheelItem.PERK_KEY + 4, "Getriebe: weitere Mobs behalten ihr Gewicht.");
        add(WheelItem.PERK_KEY + 5, "Schwungrad: dreht nach dem Stillstand der Mobs noch einige Sekunden weiter.");
        add(WheelItem.RULES_KEY, "Tiere, die in der Zone laufen, treiben es an: keine Babys, keine verletzten Tiere, keine Tiere im Wasser, ein Rad pro Tier. Ein Komparator liest die Last.");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
