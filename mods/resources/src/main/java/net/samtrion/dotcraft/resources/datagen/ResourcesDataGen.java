package net.samtrion.dotcraft.resources.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedBlockLootPlugin;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedBlockTagsPlugin;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedModelPlugin;
import net.samtrion.dotcraft.resources.datagen.compressed.CompressedRecipePlugin;
import net.samtrion.dotcraft.resources.datagen.language.EnglishUSLanguageProvider;
import net.samtrion.dotcraft.resources.datagen.language.GermanLanguageProvider;
import net.samtrion.dotcraft.resources.datagen.material.MaterialBlockLootPlugin;
import net.samtrion.dotcraft.resources.datagen.material.MaterialBlockTagsPlugin;
import net.samtrion.dotcraft.resources.datagen.material.MaterialItemTagsPlugin;
import net.samtrion.dotcraft.resources.datagen.material.MaterialAlloyRecipePlugin;
import net.samtrion.dotcraft.resources.datagen.material.MaterialModelPlugin;
import net.samtrion.dotcraft.resources.datagen.material.MaterialRecipePlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModBlockLootPlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModBlockTagsPlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModFuelPlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModItemTagsPlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModModelPlugin;
import net.samtrion.dotcraft.resources.datagen.mod.ModRecipePlugin;

/**
 * Wires the data providers of the resources mod. Run {@code ./gradlew :mods:resources:generateData}; the output
 * in {@code src/generated/resources} is committed. There is one provider per kind of data, as a data file can only
 * be written by one provider; the translations are one provider per language. The other providers call plugins
 * in the given order: first the ones of the own blocks of the mod, in {@code mod}, then the ones of the compressed
 * blocks, in {@code compressed}, then the ones of the materials, in {@code material}; the plugin interfaces are in
 * {@code api}.
 */
@EventBusSubscriber(modid = CraftResources.MODID)
public final class ResourcesDataGen {
    /** Not instantiable. */
    private ResourcesDataGen() {}

    /**
     * Registers the client-side providers: models and translations.
     *
     * @param event the data gathering event of the client run
     */
    @SubscribeEvent
    static void client(GatherDataEvent.Client event) {
        event.createProvider(o -> new ResourceModelProvider(o, new ModModelPlugin(), new CompressedModelPlugin(), new MaterialModelPlugin()));
        event.createProvider(EnglishUSLanguageProvider::new);
        event.createProvider(GermanLanguageProvider::new);
    }

    /**
     * Registers the server-side providers: recipes, loot tables, tags and fuels.
     *
     * @param event the data gathering event of the server run
     */
    @SubscribeEvent
    static void server(GatherDataEvent.Server event) {
        event.createProvider((o, l) -> new ResourceRecipeProvider(o, l, new ModRecipePlugin(), new CompressedRecipePlugin(), new MaterialRecipePlugin(), new MaterialAlloyRecipePlugin()));
        event.createProvider((o, l) -> new LootTableProvider(o, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(
                        r -> new ResourceBlockLootSubProvider(r, new ModBlockLootPlugin(), new CompressedBlockLootPlugin(), new MaterialBlockLootPlugin()),
                        LootContextParamSets.BLOCK)), l));
        event.createProvider((o, l) -> new ResourceBlockTagsProvider(o, l, new ModBlockTagsPlugin(), new CompressedBlockTagsPlugin(), new MaterialBlockTagsPlugin()));
        event.createProvider((o, l) -> new ResourceItemTagsProvider(o, l, new ModItemTagsPlugin(), new MaterialItemTagsPlugin()));
        event.createProvider((o, l) -> new ResourceFuelProvider(o, l, new ModFuelPlugin()));
        event.createProvider(ResourceWorldgenProvider::new);
    }
}
