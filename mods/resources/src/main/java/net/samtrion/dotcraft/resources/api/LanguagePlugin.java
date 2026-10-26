package net.samtrion.dotcraft.resources.api;

import net.neoforged.neoforge.common.data.LanguageProvider;

/** Contributes the translations of one language to the language provider. */
public interface LanguagePlugin {
    /**
     * Adds the translations; a key may only be added once over all plugins.
     *
     * @param language the language provider of the language
     */
    void addTranslations(LanguageProvider language);
}
