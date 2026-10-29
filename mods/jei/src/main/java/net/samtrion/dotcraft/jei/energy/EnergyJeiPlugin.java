package net.samtrion.dotcraft.jei.energy;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.jei.JeiMod;

/**
 * JEI plugin of the energy mod; discovered by JEI via {@link JeiPlugin}. Adds an info page to every generator item (see
 * {@link EnergyJeiInfo}). Does nothing when the energy mod is not loaded.
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
     * Adds the info pages of the generators.
     *
     * @param registration the JEI recipe registration
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (ModList.get().isLoaded(ENERGY)) EnergyJeiInfo.register(registration);
    }
}
