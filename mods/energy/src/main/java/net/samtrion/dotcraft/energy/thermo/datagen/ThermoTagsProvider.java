package net.samtrion.dotcraft.energy.thermo.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.thermo.ThermoTags;

/**
 * The heat and cold tags of the thermoelectric generators (the data files a data pack extends, see {@link ThermoTags}):
 * <ul>
 * <li>heat 1: furnaces and campfires (only while lit);</li>
 * <li>heat 2: fire and magma;</li>
 * <li>heat 3: lava, also in a cauldron;</li>
 * <li>cold 1: water, also in a cauldron, and a layer of snow;</li>
 * <li>cold 2: snow, powder snow, ice and the cauldron of powder snow;</li>
 * <li>cold 3: packed ice and blue ice (they do not melt).</li>
 * </ul>
 */
public final class ThermoTagsProvider extends BlockTagsProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     * @param lookup the registries
     */
    public ThermoTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, EnergyMod.MODID);
    }

    @Override
    public String getName() {
        return "Thermoelectric Generator Block Tags";
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ThermoTags.HEAT.get(0)).add(Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
        tag(ThermoTags.HEAT.get(1)).add(Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK);
        tag(ThermoTags.HEAT.get(2)).add(Blocks.LAVA, Blocks.LAVA_CAULDRON);
        tag(ThermoTags.COLD.get(0)).add(Blocks.WATER, Blocks.WATER_CAULDRON, Blocks.SNOW);
        tag(ThermoTags.COLD.get(1)).add(Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.POWDER_SNOW_CAULDRON, Blocks.ICE, Blocks.FROSTED_ICE);
        tag(ThermoTags.COLD.get(2)).add(Blocks.PACKED_ICE, Blocks.BLUE_ICE);
    }
}
