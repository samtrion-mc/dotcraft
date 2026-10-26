package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedBlockLootSubProvider;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedBlockTagsProvider;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedLanguageProvider;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedModelProvider;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedRecipeProvider;

/**
 * Wires the data providers of the resources mod. Run {@code ./gradlew :mods:resources:generateData}; the output
 * in {@code src/generated/resources} is committed. The providers themselves live next to this class, one per kind
 * of data, and read the compressed bases from {@code CompressedBases}.
 */
@EventBusSubscriber(modid = CraftResources.MODID)
public final class ResourcesDataGen {
    private ResourcesDataGen() {}

    /**
     * Registers the client-side providers: models and translations.
     *
     * @param event the data gathering event of the client run
     */
    @SubscribeEvent
    static void client(GatherDataEvent.Client event) {
        event.createProvider(CompressedModelProvider::new);
        event.createProvider(o -> new CompressedLanguageProvider(o, "en_us",
                "Compressed %1$s (Tier %2$s)", ".Craft: Compressed Blocks"));
        event.createProvider(o -> new CompressedLanguageProvider(o, "de_de",
                "Komprimiert: %1$s (Stufe %2$s)", ".Craft: Komprimierte Blöcke"));
    }

    /**
     * Registers the server-side providers: recipes, loot tables and tags.
     *
     * @param event the data gathering event of the server run
     */
    @SubscribeEvent
    static void server(GatherDataEvent.Server event) {
        event.createProvider(CompressedRecipeProvider::new);
        event.createProvider((o, l) -> new LootTableProvider(o, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(CompressedBlockLootSubProvider::new, LootContextParamSets.BLOCK)), l));
        event.createProvider(CompressedBlockTagsProvider::new);
    }
}
