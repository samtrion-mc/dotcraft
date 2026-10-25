package net.samtrion.dotcraft.energy.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.Identifier;
import net.samtrion.dotcraft.energy.EnergyMod;

@JeiPlugin
public class EnergyJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(EnergyMod.MODID, "jei");
    }
}
