package net.samtrion.dotcraft.resources.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.CompressedBlock;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.client.CompressedTooltips;
import net.samtrion.dotcraft.resources.common.Locales;

/** The German translations. Every key added here needs the same key in {@link EnglishUSLanguageProvider}. */
public final class GermanLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public GermanLanguageProvider(PackOutput output) {
        super(output, CraftResources.MODID, Locales.DE_DE);
    }

    @Override
    protected void addTranslations() {
        add(CompressedBlock.NAME_KEY, "Komprimiert: %1$s (Stufe %2$s)");
        add(CompressedTooltips.COUNT_KEY, "Enthält %s Blöcke");
        add("itemGroup." + CraftResources.MODID + ".compressed", ".Craft: Komprimierte Blöcke");
        add(ResourceBlocks.CHARCOAL_BLOCK.get(), "Holzkohleblock");
        add("jade.dotcraftresources.ore_richness", "Ergiebigkeit: %s");
        add("jade.dotcraftresources.ore_richness.poor", "Arm");
        add("jade.dotcraftresources.ore_richness.normal", "Normal");
        add("jade.dotcraftresources.ore_richness.rich", "Reich");
        MaterialLanguage.add(this, Locales.DE_DE, ".Craft: Materialien");
    }
}
