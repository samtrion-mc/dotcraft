package net.samtrion.dotcraft.rei.resources;

import java.util.Set;
import java.util.function.Predicate;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.rei.ReiMod;
import net.samtrion.dotcraft.resources.api.HiddenItems;

/**
 * REI plugin for the resources mod; hides the items the config disables (see {@link HiddenItems}): compressed blocks
 * above the configured maximum level and the disabled materials and forms, whose recipes are disabled. Discovered by
 * REI via {@link REIPluginClient}; does nothing when the resources mod is not loaded.
 */
@REIPluginClient
public class ResourcesReiPlugin implements REIClientPlugin {
    /** Mod id of the optional resources mod. */
    private static final String RESOURCES = "dotcraftresources";

    /**
     * Removes the items that the config disables from the entry list.
     *
     * @param registry the REI entry registry
     */
    @Override
    public void registerEntries(EntryRegistry registry) {
        if (ModList.get().isLoaded(RESOURCES)) hideDisabledItems(registry);
    }

    /** Only called with the resources mod loaded, so {@link HiddenItems} is only linked then. */
    private static void hideDisabledItems(EntryRegistry registry) {
        Set<Item> hidden = HiddenItems.items();
        if (hidden.isEmpty()) return;
        ReiMod.LOGGER.info("REI: hiding {} items disabled by the config", hidden.size());
        Predicate<EntryStack<?>> isHidden = s -> s.getValue() instanceof ItemStack stack && hidden.contains(stack.getItem());
        registry.removeEntryIf(isHidden);
    }
}
