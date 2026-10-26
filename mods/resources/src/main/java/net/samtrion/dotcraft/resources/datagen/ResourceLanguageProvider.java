package net.samtrion.dotcraft.resources.datagen;

import java.util.List;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.api.LanguagePlugin;

/** The translations of one language; the plugins are called in the given order. */
public final class ResourceLanguageProvider extends LanguageProvider {
    private final List<LanguagePlugin> plugins;

    /**
     * Creates the provider of one language.
     *
     * @param output  the pack output
     * @param locale  the language code, e.g. {@code en_us}
     * @param plugins the contributors of the translations
     */
    public ResourceLanguageProvider(PackOutput output, String locale, LanguagePlugin... plugins) {
        super(output, CraftResources.MODID, locale);
        this.plugins = List.of(plugins);
    }

    @Override
    protected void addTranslations() {
        plugins.forEach(plugin -> plugin.addTranslations(this));
    }
}
