package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
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
        WheelBlocks.blocks().forEach(b -> add(b.get(), "Mob-Laufrad (Stufe " + b.get().tier() + ")"));
        add(EnergyBlocks.POWER_DRAIN.get(), "Energie-Senke");
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energie");
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
