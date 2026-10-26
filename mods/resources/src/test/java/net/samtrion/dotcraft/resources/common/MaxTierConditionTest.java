package net.samtrion.dotcraft.resources.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.electronwill.nightconfig.core.CommentedConfig;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.CraftResourcesConfig;

/** Unit tests for {@link MaxTierCondition} and its config. */
class MaxTierConditionTest {
    @AfterEach
    void unload() throws ReflectiveOperationException {
        var field = ModConfigSpec.class.getDeclaredField("loadedConfig");
        field.setAccessible(true);
        field.set(CraftResourcesConfig.SPEC, null);
        CraftResourcesConfig.MAX_COMPRESS_LEVEL.clearCache();
    }

    /** The loaded-config type is sealed and package-private and {@code acceptConfig} would save it, so the field is set reflectively. */
    private static void load() throws ReflectiveOperationException {
        Class<?> type = Class.forName("net.neoforged.fml.config.LoadedConfig");
        var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var field = ModConfigSpec.class.getDeclaredField("loadedConfig");
        field.setAccessible(true);
        field.set(CraftResourcesConfig.SPEC, constructor.newInstance(CommentedConfig.inMemory(), null, null));
    }

    /** Without a loaded config (datagen) the default applies: every level enabled. */
    @Test
    void defaultsWhenNotLoaded() {
        for (int level = 1; level <= CompressedTier.MAX; level++) assertTrue(new MaxTierCondition(level).test(ICondition.IContext.EMPTY));
    }

    /** A recipe is enabled exactly up to the configured level. */
    @Test
    void enabledUpToConfiguredLevel() throws ReflectiveOperationException {
        load();
        CraftResourcesConfig.MAX_COMPRESS_LEVEL.set(3);
        for (int level = 1; level <= CompressedTier.MAX; level++) {
            assertTrue(new MaxTierCondition(level).test(ICondition.IContext.EMPTY) == (level <= 3), "level " + level);
        }
    }
}
