package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.energy.wheel.WheelItem;
import net.samtrion.dotcraft.resources.common.Locales;

/** The English translations. Every key added here needs the same key in {@link GermanLanguageProvider}. */
public final class EnglishUSLanguageProvider extends LanguageProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     */
    public EnglishUSLanguageProvider(PackOutput output) {
        super(output, EnergyMod.MODID, Locales.EN_US);
    }

    @Override
    protected void addTranslations() {
        add(EnergyItems.POTATO_BATTERY.get(), "Potato Battery");
        add(EnergyItems.RECHARGEABLE_POTATO_BATTERY.get(), "Rechargeable Potato Battery");
        add(EnergyItems.BATTERY_TIER_2.get(), "Battery (Tier 2)");
        add(EnergyItems.BATTERY_TIER_3.get(), "Battery (Tier 3)");
        add(EnergyItems.BATTERY_TIER_4.get(), "Battery (Tier 4)");
        add(EnergyItems.BATTERY_TIER_5.get(), "Battery (Tier 5)");
        add(EnergyItems.PHOTOVOLTAIC_CELL.get(), "Photovoltaic Cell");
        EnergyBlocks.SOLAR.blocks().forEach(b -> add(b.get(), "Solar Panel (Tier " + b.get().tier() + ")"));
        EnergyBlocks.LUNAR.blocks().forEach(b -> add(b.get(), "Lunar Panel (Tier " + b.get().tier() + ")"));
        WheelBlocks.blocks().forEach(b -> add(b.get(), "Mob Wheel (Tier " + b.get().tier() + ")"));
        add(EnergyBlocks.POWER_DRAIN.get(), "Power Drain");
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energy");
        add(WheelItem.STATS_KEY, "Up to %1$s FE/t, zone radius %2$s, up to %3$s mobs");
        add(WheelItem.PERK_KEY + 1, "Base wheel.");
        add(WheelItem.PERK_KEY + 2, "Bigger cage: more mobs and a wider zone.");
        add(WheelItem.PERK_KEY + 3, "Small animals pull their full weight.");
        add(WheelItem.PERK_KEY + 4, "Gearbox: more mobs keep their weight.");
        add(WheelItem.PERK_KEY + 5, "Flywheel: keeps turning for a few seconds after the mobs stop.");
        add(WheelItem.RULES_KEY, "Animals that run in the zone drive it: no babies, no hurt animals, no animals in water, one wheel per animal. A comparator reads the load.");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
