package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.item.EnergyItems;
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
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
