package net.samtrion.dotcraft.resources.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ToolType}. */
class ToolTypeTest {
    /** Each type has its own tag, only {@link ToolType#NONE} has none. */
    @Test
    void tags() {
        assertNull(ToolType.NONE.tag());
        assertEquals("minecraft:mineable/pickaxe", ToolType.PICKAXE.tag().location().toString());
        assertEquals("minecraft:mineable/hoe", ToolType.HOE.tag().location().toString());
        assertEquals("minecraft:sword_efficient", ToolType.SWORD.tag().location().toString());
        assertEquals("c:mineable/shear", ToolType.SHEAR.tag().location().toString());
        Set<?> tags = java.util.stream.Stream.of(ToolType.values()).map(ToolType::tag).filter(t -> t != null).collect(Collectors.toSet());
        assertEquals(ToolType.values().length - 1, tags.size());
    }

    /** The names used in the compressed block sources are the lower case enum names. */
    @Test
    void serializedNames() {
        for (ToolType type : ToolType.values()) {
            assertEquals(type.name().toLowerCase(java.util.Locale.ROOT), type.getSerializedName());
        }
        assertEquals(ToolType.SHEAR, ToolType.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("shear")).getOrThrow());
    }
}
