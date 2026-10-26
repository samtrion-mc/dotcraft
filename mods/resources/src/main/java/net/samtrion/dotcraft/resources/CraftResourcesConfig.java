package net.samtrion.dotcraft.resources;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/**
 * Common config of the resources mod. Recipes are checked when the data packs load, so a changed value
 * applies after a restart or {@code /reload}.
 */
public final class CraftResourcesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Highest compression tier that can be crafted; recipes that output a higher tier are disabled. */
    public static final ModConfigSpec.IntValue MAX_COMPRESS_LEVEL = BUILDER
            .push("compressed")
            .comment("Highest compression level that can be crafted (1-" + CompressedTier.MAX + "). Recipes that output a higher level are disabled.")
            .defineInRange("maxCompressLevel", CompressedTier.MAX, 1, CompressedTier.MAX);

    static {
        BUILDER.pop();
    }

    /** The built spec, registered as COMMON config. */
    public static final ModConfigSpec SPEC = BUILDER.build();

    private CraftResourcesConfig() {}

    /**
     * The configured maximum level; the default while the config is not loaded (e.g. in datagen).
     *
     * @return the highest craftable level, {@code 1..MAX}
     */
    public static int maxCompressLevel() {
        return SPEC.isLoaded() ? MAX_COMPRESS_LEVEL.get() : MAX_COMPRESS_LEVEL.getDefault();
    }
}
