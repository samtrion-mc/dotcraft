package net.samtrion.dotcraft.resources.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.common.CompressedBlock;

/** One translation for the name pattern instead of one per block, plus the creative tab title. */
final class CompressedLanguageProvider extends LanguageProvider {
    private final String name;
    private final String tab;

    /**
     * Creates the provider of one language.
     *
     * @param output the pack output
     * @param locale the language code, e.g. {@code en_us}
     * @param name   the name pattern, arguments: base name, tier
     * @param tab    the creative tab title
     */
    CompressedLanguageProvider(PackOutput output, String locale, String name, String tab) {
        super(output, CraftResources.MODID, locale);
        this.name = name;
        this.tab = tab;
    }

    @Override
    protected void addTranslations() {
        add(CompressedBlock.NAME_KEY, name);
        add("itemGroup." + CraftResources.MODID + ".compressed", tab);
    }
}
