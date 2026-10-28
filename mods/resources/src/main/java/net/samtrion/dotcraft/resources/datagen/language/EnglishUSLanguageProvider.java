package net.samtrion.dotcraft.resources.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.CompressedBlock;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.client.CompressedTooltips;
import net.samtrion.dotcraft.resources.common.Locales;

/** The English translations. Every key added here needs the same key in {@link GermanLanguageProvider}. */
public final class EnglishUSLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public EnglishUSLanguageProvider(PackOutput output) {
        super(output, CraftResources.MODID, Locales.EN_US);
    }

    @Override
    protected void addTranslations() {
        add(CompressedBlock.NAME_KEY, "Compressed %1$s (Tier %2$s)");
        add(CompressedTooltips.COUNT_KEY, "Contains %s blocks");
        add("itemGroup." + CraftResources.MODID + ".compressed", ".Craft: Compressed Blocks");
        add(ResourceBlocks.CHARCOAL_BLOCK.get(), "Block of Charcoal");
        MaterialLanguage.add(this, Locales.EN_US, ".Craft: Materials");
    }
}
