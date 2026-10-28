package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
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
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
