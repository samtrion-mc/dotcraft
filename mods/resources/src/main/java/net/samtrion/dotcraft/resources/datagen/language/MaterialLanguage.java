package net.samtrion.dotcraft.resources.datagen.language;

import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The names of all material items and blocks, composed from the name of the material (see
 * {@link Material#displayName(String)}) and the name pattern of the form (see {@link Form#names()}), so a new
 * material or form only needs its source file. Every language provider calls this with its locale, which keeps
 * the keys identical; a material or form that lacks a name for the locale fails the data generation.
 */
final class MaterialLanguage {
    /** Not instantiable. */
    private MaterialLanguage() {}

    /**
     * Adds the translations of the creative tab and of every material item and block.
     *
     * @param provider the language provider
     * @param locale   the locale of the provider, one of {@link net.samtrion.dotcraft.resources.common.Locales#SUPPORTED}
     * @param tabTitle the title of the creative tab of the materials
     */
    static void add(LanguageProvider provider, String locale, String tabTitle) {
        provider.add("itemGroup." + CraftResources.MODID + ".materials", tabTitle);
        for (Material material : Materials.all()) {
            String name = material.displayName(locale);
            for (Form form : material.forms()) {
                if (form.hosted()) {
                    for (var variant : material.oreVariants()) {
                        provider.add(MaterialBlocks.ore(material, variant.host(), variant.richness()).get(),
                                form.displayName(locale, name, variant.host(), variant.richness()));
                    }
                } else if (form.isBlock()) {
                    provider.add(MaterialBlocks.block(material, form).get(), form.displayName(locale, name));
                } else {
                    provider.add(ResourceItems.item(material, form).get(), form.displayName(locale, name));
                }
            }
        }
    }
}
