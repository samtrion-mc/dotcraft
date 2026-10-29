package net.samtrion.dotcraft.energy.datagen.language;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.flow.FlowBlocks;
import net.samtrion.dotcraft.energy.flow.FlowGeneratorItem;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.item.EnergyItems;
import net.samtrion.dotcraft.energy.vibration.VibrationBlockItem;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.thermo.ThermoBlockItem;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
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
        FlowBlocks.blocks().forEach(b -> add(b.get(), "Flow Generator (Tier " + b.get().tier() + ")"));
        VibrationBlocks.blocks().forEach(b -> add(b.get(), "Vibration Generator (Tier " + b.get().tier() + ")"));
        add(VibrationBlockItem.NEEDS_KEY, "Powered by shocks nearby: blocks broken, fights, explosions");
        add(VibrationBlockItem.PEAK_KEY, "Peak %1$s FE/t, on average at most %2$s FE/t");
        add(VibrationBlockItem.HEARING_KEY, "Hears shocks within %1$s blocks, from vibration frequency %2$s");
        add(VibrationBlockItem.BOSS_KEY, "Boss fights count x%s");
        add(VibrationBlockItem.PIERCE_KEY, "Hears through wool");
        WheelBlocks.blocks().forEach(b -> add(b.get(), "Mob Wheel (Tier " + b.get().tier() + ")"));
        add(EnergyBlocks.POWER_DRAIN.get(), "Power Drain");
        MeltBlocks.blocks().forEach(b -> add(b.get(), "Melt Generator (Tier " + b.get().tier() + ")"));
        add(net.samtrion.dotcraft.energy.melt.MeltGeneratorItem.TANK_KEY, "Water goes into a tank of %s buckets that can only be drained");
        add(net.samtrion.dotcraft.energy.melt.MeltGeneratorItem.FULL_KEY, "A full tank leaves %s%% of the FE per ice until water is drained");
        net.samtrion.dotcraft.energy.kite.datagen.KiteLanguage.english(this);
        add("itemGroup." + EnergyMod.MODID + ".energy", ".Craft: Energy");
        add(FlowGeneratorItem.FORMULA_KEY, "Needs water: peak x water share x biome x (1 + animals)");
        add(FlowGeneratorItem.AXIS_KEY, "Front and back must stay free for the water (a solid block there: 0 FE); gives FE to the other sides");
        add(FlowGeneratorItem.PERK_KEY + 1, "Baseline: needs a nearly full cube of water");
        add(FlowGeneratorItem.PERK_KEY + 2, "Wide net: animals count within 6 blocks");
        add(FlowGeneratorItem.PERK_KEY + 3, "Adaptation: keeps 75% outside ocean and river");
        add(FlowGeneratorItem.PERK_KEY + 4, "Lean water: a small pond is already enough");
        add(FlowGeneratorItem.PERK_KEY + 5, "Abyssal: independent of the biome");
        ThermoBlocks.blocks().forEach(b -> add(b.get(), "Thermoelectric Generator (Tier " + b.get().tier() + ")"));
        add(ThermoBlockItem.TOOLTIP_KEY, "Peak %1$s FE/t at a temperature difference of %2$s");
        add(ThermoBlockItem.HINT_KEY, "Needs a hot and a cold neighbour on opposite sides (levels 1-3 each, they add up to the difference)");
        add(WheelItem.STATS_KEY, "Up to %1$s FE/t, up to %2$s mobs on the wheel");
        add(WheelItem.PERK_KEY + 1, "Base wheel.");
        add(WheelItem.PERK_KEY + 2, "Bigger cage: more mobs.");
        add(WheelItem.PERK_KEY + 3, "Small animals pull their full weight.");
        add(WheelItem.PERK_KEY + 4, "Gearbox: more mobs keep their weight.");
        add(WheelItem.PERK_KEY + 5, "Flywheel: keeps turning for a few seconds after the mobs stop.");
        add(WheelItem.RULES_KEY, "Animals that stand and run on top of it drive it (animals nearby do not), standing counts half: no babies, no hurt animals, no animals in water, one wheel per animal. A comparator reads the load.");
        add(EnergyItems.TOOLTIP_KEY, "%1$s / %2$s FE");
    }
}
