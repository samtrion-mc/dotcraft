package net.samtrion.dotcraft.energy.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.generator.EnergyBlocks;

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
        net.samtrion.dotcraft.energy.kite.KiteBlocks.blocks().forEach(b -> pickaxe.add(b.get()));
    }
}
