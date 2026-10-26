package net.samtrion.dotcraft.resources;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/**
 * Common config of the resources mod. Recipes are checked when the data packs load, so a changed value
 * applies after a restart or {@code /reload}.
 */
public final class CraftResourcesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Master switch of the compression feature; when off, no compress recipe is craftable. */
    public static final ModConfigSpec.BooleanValue COMPRESSED_ENABLED = BUILDER
            .push("compressed")
            .comment("Enables compressing blocks. When disabled, all compress recipes are disabled and the compressed blocks are hidden in JEI; existing blocks can still be decompressed.")
            .define("enabled", true);

    /** Highest compression tier that can be crafted; recipes that output a higher tier are disabled. */
    public static final ModConfigSpec.IntValue MAX_COMPRESS_LEVEL = BUILDER
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
     * @return the highest craftable level, {@code 0} when compressing is disabled, otherwise {@code 1..MAX}
     */
    public static int maxCompressLevel() {
        if (SPEC.isLoaded() && !COMPRESSED_ENABLED.get()) return 0;
        return SPEC.isLoaded() ? MAX_COMPRESS_LEVEL.get() : MAX_COMPRESS_LEVEL.getDefault();
    }
}
