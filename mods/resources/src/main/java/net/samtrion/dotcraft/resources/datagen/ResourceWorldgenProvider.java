package net.samtrion.dotcraft.resources.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreDimension;
import net.samtrion.dotcraft.resources.common.OreHost;
import net.samtrion.dotcraft.resources.common.OreRichness;
import net.samtrion.dotcraft.resources.common.conditions.ConfigCondition;
import net.samtrion.dotcraft.resources.worldgen.ExposedOreConfiguration;
import net.samtrion.dotcraft.resources.worldgen.ExposedOreFeature;
import net.samtrion.dotcraft.resources.worldgen.ResourceFeatures;

/**
 * World generation of the ores of all {@link Materials}. Every ore entry of a material (see {@link Material.OreSpec};
 * a material without an {@code ore} array has the one default entry) generates in each of its dimensions, and for every
 * entry, dimension and {@link OreRichness} there is one configured and one placed feature, which target the hosts of
 * the entry that occur in the dimension. Per entry and dimension one {@code neoforge:add_features} biome modifier adds
 * the placed features of the richnesses in the {@code underground_ores} step to the biomes of the dimension
 * ({@code c:is_overworld}, {@code c:is_nether}, {@code c:is_end}, or the {@code biomeTag} of the entry).
 *
 * <p>Names, all in this mod's namespace, with {@code <entry>} the id of the entry (for a source without ids
 * {@code ore_0}, {@code ore_1}, ...), {@code <dimension>} one of {@code overworld}, {@code nether}, {@code end} and
 * {@code <richness>} one of {@code poor}, {@code normal}, {@code rich}:
 * <ul>
 * <li>configured and placed feature {@code <material>_<entry>_<dimension>_<richness>}, e.g. {@code tin_ore_0_overworld_normal};</li>
 * <li>biome modifier {@code <material>_<entry>_<dimension>}, e.g. {@code tin_ore_0_overworld}.</li>
 * </ul>
 * The modifier is per entry, not per material, as the config switch {@code materials.<material>.ores.<entry>.enabled} and
 * the biome tag belong to one entry. An entry with {@code exposure} uses the {@code dotcraftresources:exposed_ore}
 * feature (see {@link ExposedOreFeature}), all others the vanilla {@code minecraft:ore}.
 *
 * <p>Each modifier carries the {@code worldgen} and {@code materials} config conditions, the switch of the ore form of
 * its material and the switch of its entry (see {@link ConfigCondition}), so a disabled ore, entry or material does not
 * generate. The modifier, the configured and the placed features of a material or ore form with {@code neoforge:conditions}
 * (see {@link net.samtrion.dotcraft.resources.common.conditions.Conditions}) carry these too, as the features name the ore
 * blocks, which are not registered where a condition fails.
 *
 * <p>The settings of {@link Material.OreSpec} describe the normal ore; poor ore is placed 1.5 times as often over
 * a range 16 blocks wider at each end with an even height distribution, rich ore a quarter as often. The height
 * range is cut to what the dimension has ({@link OreDimension}). The blocks the ore replaces are given by
 * {@link OreHost#rule()}.
 */
public final class ResourceWorldgenProvider extends DatapackBuiltinEntriesProvider {

    /**
     * Creates the provider.
     *
     * @param output   the target directory
     * @param registry the registries to look the vanilla biomes and tags up in
     */
    public ResourceWorldgenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registry) {
        super(output, registry, builder(), conditions(), Set.of(CraftResources.MODID));
    }

    /** The registry entries of the ore features and biome modifiers. */
    private static net.minecraft.core.RegistrySetBuilder builder() {
        return new net.minecraft.core.RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ResourceWorldgenProvider::configuredFeatures)
                .add(Registries.PLACED_FEATURE, ResourceWorldgenProvider::placedFeatures)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceWorldgenProvider::biomeModifiers);
    }

    /** The config conditions of the entries. */
    private static Consumer<BiConsumer<ResourceKey<?>, ICondition>> conditions() {
        return add -> forEachGeneration((material, spec, dimension, hosts) -> {
            ResourceKey<BiomeModifier> modifier = modifierKey(material, spec, dimension);
            add.accept(modifier, new ConfigCondition(ConfigCondition.Switch.WORLDGEN));
            add.accept(modifier, new ConfigCondition(ConfigCondition.Switch.MATERIALS));
            add.accept(modifier, ConfigCondition.ofForm(material.id(), material.hostedForm().orElseThrow().id()));
            add.accept(modifier, ConfigCondition.ofOre(material.id(), spec.id()));
            // the features name the ore blocks, so they are loaded only where the material and the ore form are
            for (ICondition condition : material.conditions(material.hostedForm().orElseThrow())) {
                add.accept(modifier, condition);
                for (OreRichness richness : richnesses(spec)) {
                    add.accept(configuredKey(material, spec, dimension, richness), condition);
                    add.accept(placedKey(material, spec, dimension, richness), condition);
                }
            }
        });
    }

    /** Receives one dimension of one ore entry of one material with the hosts of the entry that occur in it, in source order. */
    private interface GenerationVisitor {
        void visit(Material material, Material.OreSpec spec, OreDimension dimension, List<OreHost> hosts);
    }

    /** Calls the visitor for each material, ore entry and dimension of the entry, in source order. */
    private static void forEachGeneration(GenerationVisitor visitor) {
        for (Material material : Materials.all()) {
            if (material.hostedForm().isEmpty()) continue;
            for (Material.OreSpec spec : material.oreSpecs()) {
                for (OreDimension dimension : OreDimension.values()) {
                    List<OreHost> hosts = spec.hosts(dimension);
                    if (!hosts.isEmpty()) visitor.visit(material, spec, dimension, hosts);
                }
            }
        }
    }

    /** The richnesses of an entry in their declaration order. */
    private static List<OreRichness> richnesses(Material.OreSpec spec) {
        return Stream.of(OreRichness.values()).filter(spec.richness()::contains).toList();
    }

    /** An id of this mod. */
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CraftResources.MODID, path);
    }

    /** Name of the modifier of an entry and dimension. */
    private static String name(Material material, Material.OreSpec spec, OreDimension dimension) {
        return material.id() + "_" + spec.id() + "_" + dimension.id();
    }

    /** Name of the features of an entry, dimension and richness. */
    private static String name(Material material, Material.OreSpec spec, OreDimension dimension, OreRichness richness) {
        return name(material, spec, dimension) + "_" + richness.id();
    }

    /** Key of a configured feature. */
    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(Material material, Material.OreSpec spec, OreDimension dimension,
                                                                       OreRichness richness) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(name(material, spec, dimension, richness)));
    }

    /** Key of a placed feature. */
    private static ResourceKey<PlacedFeature> placedKey(Material material, Material.OreSpec spec, OreDimension dimension,
                                                        OreRichness richness) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(name(material, spec, dimension, richness)));
    }

    /** Key of a biome modifier. */
    private static ResourceKey<BiomeModifier> modifierKey(Material material, Material.OreSpec spec, OreDimension dimension) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, id(name(material, spec, dimension)));
    }

    /** Registers the configured ore features. */
    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        forEachGeneration((material, spec, dimension, hosts) -> {
            for (OreRichness richness : richnesses(spec)) {
                List<OreConfiguration.TargetBlockState> targets = new ArrayList<>();
                for (OreHost host : hosts) {
                    targets.add(OreConfiguration.target(host.rule(),
                            MaterialBlocks.ore(material, host, richness).get().defaultBlockState()));
                }
                OreConfiguration ore = new OreConfiguration(targets, spec.veinSize());
                context.register(configuredKey(material, spec, dimension, richness), spec.exposure().isEmpty()
                        ? new ConfiguredFeature<>(Feature.ORE, ore)
                        : new ConfiguredFeature<>(ResourceFeatures.EXPOSED_ORE.get(), new ExposedOreConfiguration(ore, spec.exposure())));
            }
        });
    }

    /** Registers the placed ore features. */
    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE);
        forEachGeneration((material, spec, dimension, hosts) -> {
            for (OreRichness richness : richnesses(spec)) {
                int count = switch (richness) {
                    case POOR -> (spec.count() * 3 + 1) / 2;
                    case NORMAL -> spec.count();
                    case RICH -> Math.max(1, spec.count() / 4);
                };
                int widen = richness == OreRichness.POOR ? 16 : 0;
                int low = Math.max(dimension.lowest(), spec.minY() - widen);
                int high = Math.min(dimension.highest(), spec.maxY() + widen);
                if (low > high) { // the settings aim at another dimension: use the whole height of this one
                    low = dimension.lowest();
                    high = dimension.highest();
                }
                VerticalAnchor from = VerticalAnchor.absolute(low);
                VerticalAnchor to = VerticalAnchor.absolute(high);
                List<PlacementModifier> placement = List.of(CountPlacement.of(count), InSquarePlacement.spread(),
                        richness == OreRichness.POOR ? HeightRangePlacement.uniform(from, to) : HeightRangePlacement.triangle(from, to),
                        BiomeFilter.biome());
                context.register(placedKey(material, spec, dimension, richness),
                        new PlacedFeature(configured.getOrThrow(configuredKey(material, spec, dimension, richness)), placement));
            }
        });
    }

    /** Registers the biome modifiers that add the ore features. */
    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);
        forEachGeneration((material, spec, dimension, hosts) -> {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, Identifier.parse(spec.biomeTag().orElse(dimension.biomeTag())));
            List<net.minecraft.core.Holder<PlacedFeature>> features = new ArrayList<>();
            for (OreRichness richness : richnesses(spec)) features.add(placed.getOrThrow(placedKey(material, spec, dimension, richness)));
            context.register(modifierKey(material, spec, dimension), new BiomeModifiers.AddFeaturesBiomeModifier(
                    biomes.getOrThrow(tag), HolderSet.direct(features), GenerationStep.Decoration.UNDERGROUND_ORES));
        });
    }
}
