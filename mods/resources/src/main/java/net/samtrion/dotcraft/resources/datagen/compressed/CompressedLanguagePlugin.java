package net.samtrion.dotcraft.resources.datagen.compressed;

import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.api.LanguagePlugin;
import net.samtrion.dotcraft.resources.block.CompressedBlock;

/** One translation for the name pattern instead of one per compressed block, plus the creative tab title. */
public final class CompressedLanguagePlugin implements LanguagePlugin {
    private final String name;
    private final String tab;

    /**
     * Creates the plugin of one language.
     *
     * @param name the name pattern, arguments: base name, tier
     * @param tab  the creative tab title
     */
    public CompressedLanguagePlugin(String name, String tab) {
        this.name = name;
        this.tab = tab;
    }

    @Override
    public void addTranslations(LanguageProvider language) {
        language.add(CompressedBlock.NAME_KEY, name);
        language.add("itemGroup." + CraftResources.MODID + ".compressed", tab);
    }
}
