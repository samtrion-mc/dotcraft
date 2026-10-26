package net.samtrion.dotcraft.resources.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.CompressedBlock;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** The English translations. Every key added here needs the same key in {@link GermanLanguageProvider}. */
public final class EnglishUSLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public EnglishUSLanguageProvider(PackOutput output) {
        super(output, CraftResources.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(CompressedBlock.NAME_KEY, "Compressed %1$s (Tier %2$s)");
        add("itemGroup." + CraftResources.MODID + ".compressed", ".Craft: Compressed Blocks");
        add(ResourceBlocks.CHARCOAL_BLOCK.get(), "Block of Charcoal");
    }
}
