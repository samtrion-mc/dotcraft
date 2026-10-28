package net.samtrion.dotcraft.resources.compat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import net.minecraft.world.item.Item;
import net.samtrion.dotcraft.resources.CommonConfig;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.common.CompressedTier;
import net.samtrion.dotcraft.resources.common.Form;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.item.ResourceItems;

/**
 * The items the recipe viewers hide because the config disables them: the items and blocks of every material form
 * that is disabled (all of them when the materials or a material are disabled, see {@link CommonConfig}), and
 * the compressed blocks above the maximum compress level.
 */
public final class HiddenItems {
    /** Not instantiable. */
    private HiddenItems() {}

    /**
     * Collects the items to hide for the current config.
     *
     * @return the hidden items, empty when everything is enabled
     */
    public static Set<Item> items() {
        Set<Item> hidden = new HashSet<>();
        for (Material material : Materials.all()) {
            for (Form form : material.forms()) {
                if (!CommonConfig.formEnabled(material.id(), form.id())) {
                    ResourceItems.items(material, form).forEach(i -> hidden.add(i.get()));
                }
            }
        }
        int max = CommonConfig.maxCompressLevel();
        if (max < CompressedTier.MAX) {
            CompressedBases.enabled().forEach(base -> IntStream.rangeClosed(max + 1, CompressedTier.MAX)
                    .forEach(tier -> hidden.add(ResourceBlocks.item(base, tier).get())));
        }
        return hidden;
    }
}
