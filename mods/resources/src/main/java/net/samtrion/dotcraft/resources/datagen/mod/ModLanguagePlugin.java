package net.samtrion.dotcraft.resources.datagen.mod;

import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.api.LanguagePlugin;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;

/** The names of the own blocks of the mod in one language. */
public final class ModLanguagePlugin implements LanguagePlugin {
    private final String charcoalBlock;

    /**
     * Creates the plugin of one language.
     *
     * @param charcoalBlock the name of the charcoal block
     */
    public ModLanguagePlugin(String charcoalBlock) {
        this.charcoalBlock = charcoalBlock;
    }

    @Override
    public void addTranslations(LanguageProvider language) {
        language.add(ResourceBlocks.CHARCOAL_BLOCK.get(), charcoalBlock);
    }
}
