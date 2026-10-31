package net.samtrion.dotcraft.jei.energy;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.jei.JeiMod;

/**
 * JEI plugin of the energy mod; discovered by JEI via {@link JeiPlugin}. Adds an info page to every generator item (see
 * {@link EnergyJeiInfo}) and the yield per mob of the mob wheels (see {@link WheelYieldCategory}). Does nothing when the energy mod is not loaded.
 */
@JeiPlugin
public class EnergyJeiPlugin implements IModPlugin {
    /** Mod id of the optional energy mod. */
    private static final String ENERGY = "dotcraftenergy";

    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraftjei:energy}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(JeiMod.MODID, "energy");
    }

    /**
     * Adds the mob wheel yield category.
     *
     * @param registration the JEI category registration
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (ModList.get().isLoaded(ENERGY)) EnergyJeiInfo.registerWheelCategory(registration);
    }

    /**
     * Makes the wheels the catalysts of the mob wheel yield category.
     *
     * @param registration the JEI catalyst registration
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ModList.get().isLoaded(ENERGY)) EnergyJeiInfo.registerWheelCatalysts(registration);
    }

    /**
     * Adds the info pages of the generators.
     *
     * @param registration the JEI recipe registration
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded(ENERGY)) return;
        EnergyJeiInfo.register(registration);
        EnergyJeiInfo.registerWheelYield(registration);
    }
}
