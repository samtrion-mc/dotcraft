package net.samtrion.dotcraft.resources.compat.rei;

import java.util.Set;
import java.util.function.Predicate;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.compat.HiddenItems;

/**
 * REI plugin of the resources mod; like the JEI plugin it hides all material items when the materials are disabled and
 * the compressed blocks above the configured maximum level. Discovered by REI via {@link REIPluginClient}.
 */
@REIPluginClient
public class ResourcesReiPlugin implements REIClientPlugin {
    /**
     * Removes the disabled items from the entry list.
     *
     * @param registry the REI entry registry
     */
    @Override
    public void registerEntries(EntryRegistry registry) {
        Set<Item> hidden = HiddenItems.items();
        if (hidden.isEmpty()) return;
        CraftResources.LOGGER.info("REI: hiding {} items disabled by the config", hidden.size());
        Predicate<EntryStack<?>> isHidden = s -> s.getValue() instanceof ItemStack stack && hidden.contains(stack.getItem());
        registry.removeEntryIf(isHidden);
    }
}
