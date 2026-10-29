package net.samtrion.dotcraft.energy.kite.datagen;

import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;

/** The translations of the kite wind generator; called by the language providers, so both languages are written side by side. */
public final class KiteLanguage {
    /** Not instantiable. */
    private KiteLanguage() {}

    /**
     * Adds the English texts.
     *
     * @param lang the English provider
     */
    public static void english(LanguageProvider lang) {
        lang.add(KiteBlocks.KITE.get(), "Kite");
        lang.add(KiteBlocks.KITE_ENTITY.get(), "Kite");
        KiteBlocks.blocks().forEach(b -> lang.add(b.get(), "Kite Wind Generator (Tier " + b.get().tier() + ")"));
        lang.add("message." + KiteBlocks.MODID + ".kite.none", "No kite. Use a kite on the generator; sneak with an empty hand to take it back.");
        lang.add("message." + KiteBlocks.MODID + ".kite.status", "Kite at %1$s of %2$s m, %3$s FE/t");
    }

    /**
     * Adds the German texts.
     *
     * @param lang the German provider
     */
    public static void german(LanguageProvider lang) {
        lang.add(KiteBlocks.KITE.get(), "Lenkdrachen");
        lang.add(KiteBlocks.KITE_ENTITY.get(), "Lenkdrachen");
        KiteBlocks.blocks().forEach(b -> lang.add(b.get(), "Drachen-Windgenerator (Stufe " + b.get().tier() + ")"));
        lang.add("message." + KiteBlocks.MODID + ".kite.none", "Kein Drachen. Benutze einen Lenkdrachen am Generator; schleichend mit leerer Hand nimmst du ihn zurück.");
        lang.add("message." + KiteBlocks.MODID + ".kite.status", "Drachen auf %1$s von %2$s m, %3$s FE/t");
    }
}
