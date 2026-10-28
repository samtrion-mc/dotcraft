package net.samtrion.dotcraft.resources.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.compat.HiddenItems;

/**
 * JEI plugin of the resources mod; hides the items the config disables (see {@link HiddenItems}): compressed blocks
 * above the configured maximum level and the disabled materials and forms, whose recipes are disabled. Discovered by JEI via {@link JeiPlugin}.
 */
@JeiPlugin
public class ResourcesJeiPlugin implements IModPlugin {
    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraftresources:jei}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(CraftResources.MODID, "jei");
    }

    /**
     * Removes the items that the config disables from the ingredient list.
     *
     * @param runtime the JEI runtime
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = HiddenItems.items().stream().map(ItemStack::new).toList();
        if (hidden.isEmpty()) return;
        CraftResources.LOGGER.info("JEI: hiding {} items disabled by the config", hidden.size());
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }
}
