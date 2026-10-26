package net.samtrion.dotcraft.resources.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.samtrion.dotcraft.resources.CraftResources;

/** Registers the world generation features of the mod. */
public final class ResourceFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, CraftResources.MODID);

    /** {@code dotcraftresources:exposed_ore}, an ore vein that only places the blocks that touch air, lava or water. */
    public static final DeferredHolder<Feature<?>, ExposedOreFeature> EXPOSED_ORE = FEATURES.register("exposed_ore", ExposedOreFeature::new);

    /** Not instantiable. */
    private ResourceFeatures() {}

    /**
     * Hooks the register onto the mod event bus.
     *
     * @param bus the mod event bus
     */
    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
