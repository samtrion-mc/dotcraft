package net.samtrion.dotcraft.jei.energy;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.jei.JeiMod;

/** JEI plugin of the energy mod; discovered by JEI via {@link JeiPlugin}. Touches no class of the energy mod; nothing registered yet. */
@JeiPlugin
public class EnergyJeiPlugin implements IModPlugin {
    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraftjei:energy}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(JeiMod.MODID, "energy");
    }
}
