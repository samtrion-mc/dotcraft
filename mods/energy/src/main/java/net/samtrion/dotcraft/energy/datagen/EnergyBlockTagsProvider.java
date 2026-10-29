package net.samtrion.dotcraft.energy.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.current.CurrentBlocks;
import net.samtrion.dotcraft.energy.thermo.ThermoBlocks;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;
import net.samtrion.dotcraft.energy.vibration.VibrationBlocks;
import net.samtrion.dotcraft.energy.melt.MeltBlocks;
import net.samtrion.dotcraft.energy.melt.MeltTags;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;

/** The block tags of the energy mod: the generators are mined with a pickaxe. */
public final class EnergyBlockTagsProvider extends BlockTagsProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     * @param lookup the registries
     */
    public EnergyBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, EnergyMod.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        EnergyBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
        CurrentBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
        VibrationBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
        ThermoBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
        // melt generators: pickaxe, the heat sources and the three kinds of ice they burn
        MeltBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
        tag(MeltTags.HEAT).add(Blocks.LAVA).add(Blocks.FIRE).add(Blocks.SOUL_FIRE).add(Blocks.MAGMA_BLOCK).add(Blocks.CAMPFIRE).add(Blocks.SOUL_CAMPFIRE)
                .add(Blocks.FURNACE).add(Blocks.BLAST_FURNACE).add(Blocks.SMOKER);
        tag(MeltTags.ICE).add(Blocks.ICE);
        tag(MeltTags.PACKED_ICE).add(Blocks.PACKED_ICE);
        tag(MeltTags.BLUE_ICE).add(Blocks.BLUE_ICE);
        WheelBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
    }
}
