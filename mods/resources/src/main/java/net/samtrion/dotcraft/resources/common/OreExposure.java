package net.samtrion.dotcraft.resources.common;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** What a block of an ore vein must touch to be placed, see {@link Material.OreSpec#exposure()}. */
public enum OreExposure implements StringRepresentable {
    /** Air, also cave air. */
    AIR,
    /** Lava, source or flowing. */
    LAVA,
    /** Water, source or flowing. */
    WATER;

    /** Codec that reads and writes the lower case name, e.g. in the material sources. */
    public static final Codec<OreExposure> CODEC = StringRepresentable.fromEnum(OreExposure::values);

    /**
     * Tells whether a block is of this kind.
     *
     * @param state the block state of a neighbour
     * @return {@code true} for air (also cave air), or the water or lava block (source and flowing are one block)
     */
    public boolean matches(BlockState state) {
        return switch (this) {
            case AIR -> state.isAir();
            case LAVA -> state.is(Blocks.LAVA);
            case WATER -> state.is(Blocks.WATER);
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
