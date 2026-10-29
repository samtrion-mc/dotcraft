package net.samtrion.dotcraft.energy.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.datagen.language.EnglishUSLanguageProvider;
import net.samtrion.dotcraft.energy.datagen.language.GermanLanguageProvider;
import net.samtrion.dotcraft.energy.vibration.VibrationLootPlugin;
import net.samtrion.dotcraft.resources.datagen.ResourceBlockLootSubProvider;

/**
 * Wires the data providers of the energy mod. Run {@code ./gradlew :mods:energy:generateData}; the output in
 * {@code src/generated/resources} is committed. There is one provider per kind of data, as a data file can only
 * be written by one provider; the translations are one provider per language.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class EnergyDataGen {
    /** Not instantiable. */
    private EnergyDataGen() {}

    /**
     * Registers the client-side providers: models and translations.
     *
     * @param event the data gathering event of the client run
     */
    @SubscribeEvent
    static void client(GatherDataEvent.Client event) {
        event.createProvider(EnergyModelProvider::new);
        event.createProvider(EnglishUSLanguageProvider::new);
        event.createProvider(GermanLanguageProvider::new);
    }

    /**
     * Registers the server-side providers: recipes, loot tables and block tags.
     *
     * @param event the data gathering event of the server run
     */
    @SubscribeEvent
    static void server(GatherDataEvent.Server event) {
        event.createProvider(EnergyRecipeProvider::new);
        event.createProvider((o, l) -> new LootTableProvider(o, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(r -> new ResourceBlockLootSubProvider(r, new GeneratorLootPlugin(), new VibrationLootPlugin()),
                        LootContextParamSets.BLOCK)), l));
        event.createProvider(EnergyBlockTagsProvider::new);
    }
}
