package net.samtrion.dotcraft.resources.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedBlocks;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/**
 * JEI plugin of the resources mod; hides compressed blocks above the configured maximum level, whose recipes are
 * disabled. Discovered by JEI via {@link JeiPlugin}.
 */
@JeiPlugin
public class ResourcesJeiPlugin implements IModPlugin {
    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraft_resources:jei}
     */
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(CraftResources.MODID, "jei");
    }

    /**
     * Removes the compressed blocks of every level above {@link CraftResourcesConfig#maxCompressLevel()} from the
     * ingredient list.
     *
     * @param runtime the JEI runtime
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        int max = CraftResourcesConfig.maxCompressLevel();
        if (max >= CompressedTier.MAX) return;
        List<ItemStack> hidden = CompressedBases.enabled()
                .flatMap(base -> hiddenStacks(base, max))
                .toList();
        CraftResources.LOGGER.info("JEI: hiding {} compressed blocks above level {}", hidden.size(), max);
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }

    private static java.util.stream.Stream<ItemStack> hiddenStacks(CompressedBase base, int max) {
        return java.util.stream.IntStream.rangeClosed(max + 1, CompressedTier.MAX)
                .mapToObj(tier -> new ItemStack(CompressedBlocks.item(base, tier).get()));
    }
}
