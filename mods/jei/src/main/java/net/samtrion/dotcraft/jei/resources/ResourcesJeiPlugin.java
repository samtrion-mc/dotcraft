package net.samtrion.dotcraft.jei.resources;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.jei.JeiMod;
import net.samtrion.dotcraft.resources.api.HiddenItems;

/**
 * JEI plugin for the resources mod; hides the items the config disables (see {@link HiddenItems}): compressed blocks
 * above the configured maximum level and the disabled materials and forms, whose recipes are disabled. Discovered by
 * JEI via {@link JeiPlugin}; does nothing when the resources mod is not loaded.
 */
@JeiPlugin
public class ResourcesJeiPlugin implements IModPlugin {
    /** Mod id of the optional resources mod. */
    private static final String RESOURCES = "dotcraftresources";

    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraftjei:resources}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(JeiMod.MODID, "resources");
    }

    /**
     * Removes the items that the config disables from the ingredient list.
     *
     * @param runtime the JEI runtime
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (ModList.get().isLoaded(RESOURCES)) hideDisabledItems(runtime);
    }

    /** Only called with the resources mod loaded, so {@link HiddenItems} is only linked then. */
    private static void hideDisabledItems(IJeiRuntime runtime) {
        List<ItemStack> hidden = HiddenItems.items().stream().map(ItemStack::new).toList();
        if (hidden.isEmpty()) return;
        JeiMod.LOGGER.info("JEI: hiding {} items disabled by the config", hidden.size());
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
