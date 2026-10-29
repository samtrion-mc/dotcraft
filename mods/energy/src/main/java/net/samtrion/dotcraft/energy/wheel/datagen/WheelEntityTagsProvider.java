package net.samtrion.dotcraft.energy.wheel.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.wheel.WheelTags;

/**
 * The entity type tags of the mob wheels: who runs a wheel, who weighs twice (horses and the like) and who half (small animals).
 * Hostile mobs and everything else are simply not in the tags and so count nothing.
 */
public final class WheelEntityTagsProvider extends EntityTypeTagsProvider {
    /**
     * Creates the provider.
     *
     * @param output the pack output
     * @param lookup the registries
     */
    public WheelEntityTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, EnergyMod.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(WheelTags.STRONG).add(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.CAMEL, EntityType.LLAMA, EntityType.TRADER_LLAMA);
        tag(WheelTags.WEAK).add(EntityType.RABBIT, EntityType.CHICKEN, EntityType.TURTLE, EntityType.ARMADILLO);
        tag(WheelTags.RUNNERS).addTag(WheelTags.STRONG).addTag(WheelTags.WEAK)
                .add(EntityType.PIG, EntityType.COW, EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.GOAT, EntityType.WOLF, EntityType.FOX,
                        EntityType.CAT, EntityType.OCELOT);
    }
}
