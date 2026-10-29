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
        lang.add("message." + KiteBlocks.MODID + ".kite.status", "Kite at %1$s of %2$s m, %3$s FE/t (%4$s)");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.none", "flying freely");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.no_sky", "no sky here");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.storm", "reeled in by the storm");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.blocked", "blocked by something above");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.weather", "weather costs output");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.crowded", "crowded sky costs output");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.tether", "Tether: %s m (the longer, the more FE: output = base x tether / 16)");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.1", "Plain kite: needs a free 3x3 way up, rain halves the output, a storm reels it in.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.2", "Waxed line: rain costs only a quarter.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.3", "Rain-proof sail; ignores obstructions: blocks above and a crowded sky cost nothing.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.4", "Storm line: flies in a thunderstorm at half output; a crowded sky costs half as much. Two kites share the base. Lightning on the kite: 1,000,000 FE, rarely.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.5", "Insulated tether: ignores thunder and a crowded sky. Two kites share the base. Lightning on the kite: 1,000,000 FE, rarely.");
    }

    /**
     * Adds the German texts.
     *
     * @param lang the German provider
     */
    public static void german(LanguageProvider lang) {
        lang.add(KiteBlocks.KITE.get(), "Lenkdrachen");
        lang.add(KiteBlocks.KITE_ENTITY.get(), "Lenkdrachen");
        KiteBlocks.blocks().forEach(b -> lang.add(b.get(), "Lenkdrachen-Windgenerator (Stufe " + b.get().tier() + ")"));
        lang.add("message." + KiteBlocks.MODID + ".kite.none", "Kein Lenkdrachen. Benutze einen Lenkdrachen am Generator; schleichend mit leerer Hand nimmst du ihn zurück.");
        lang.add("message." + KiteBlocks.MODID + ".kite.status", "Lenkdrachen auf %1$s von %2$s m, %3$s FE/t (%4$s)");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.none", "fliegt frei");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.no_sky", "hier gibt es keinen Himmel");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.storm", "vom Sturm eingeholt");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.blocked", "oben blockiert");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.weather", "Wetter kostet Leistung");
        lang.add("message." + KiteBlocks.MODID + ".kite.limit.crowded", "zugebauter Himmel kostet Leistung");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.tether", "Leine: %s m (je länger, desto mehr FE: Ertrag = Basis x Leine / 16)");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.1", "Einfacher Lenkdrachen: braucht einen freien 3x3-Weg nach oben, Regen halbiert die Leistung, ein Sturm holt ihn ein.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.2", "Gewachste Leine: Regen kostet nur ein Viertel.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.3", "Regenfestes Segel; ignoriert Hindernisse: Blöcke darüber und ein zugebauter Himmel kosten nichts.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.4", "Sturmleine: fliegt im Gewitter mit halber Leistung; ein zugebauter Himmel kostet nur halb so viel. Zwei Lenkdrachen teilen sich die Basis. Blitz auf den Lenkdrachen: 1.000.000 FE, selten.");
        lang.add("tooltip." + KiteBlocks.MODID + ".kite.perk.5", "Isolierte Leine: ignoriert Gewitter und einen zugebauten Himmel. Zwei Lenkdrachen teilen sich die Basis. Blitz auf den Lenkdrachen: 1.000.000 FE, selten.");
    }
}
