package net.samtrion.dotcraft.resources.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.samtrion.dotcraft.resources.CraftResources;

/** Ids in this mod's namespace, shared by the data providers. */
public final class DataGenIds {
    private DataGenIds() {}

    /**
     * An id in the namespace of this mod.
     *
     * @param path the path
     * @return the id
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CraftResources.MODID, path);
    }

    /**
     * A recipe key in this mod's namespace; the default of {@code save(output)} would use the result's name in
     * {@code minecraft} and override vanilla recipes.
     *
     * @param path the recipe path
     * @return the key
     */
    public static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(Registries.RECIPE, id(path));
    }
}
