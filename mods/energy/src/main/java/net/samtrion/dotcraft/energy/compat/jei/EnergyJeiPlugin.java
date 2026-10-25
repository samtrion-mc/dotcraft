package net.samtrion.dotcraft.energy.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;

/** JEI plugin of the energy mod; discovered by JEI via {@link JeiPlugin}. */
@JeiPlugin
public class EnergyJeiPlugin implements IModPlugin {
    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraft_energy:jei}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "jei");
    }
}
