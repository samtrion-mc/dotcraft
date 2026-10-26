package net.samtrion.dotcraft.resources.block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;

/**
 * A compressed block. The name is composed from the base block, so a single translation key
 * covers every compressed block in every language.
 */
public class CompressedBlock extends Block implements Compressed {
    /** Translation key of the shared name pattern, arguments: base name, tier. */
    public static final String NAME_KEY = "block.dotcraftresources.compressed";

    private final Block base;
    private final int tier;

    /**
     * Creates a compressed block.
     *
     * @param properties the block properties
     * @param base       the compressed vanilla block
     * @param tier       the tier, {@code 1..MAX}
     */
    public CompressedBlock(Properties properties, Block base, int tier) {
        super(properties);
        this.base = base;
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public MutableComponent getName() {
        return name(base, tier);
    }

    /**
     * Display name of a compressed block, also used for its item.
     *
     * @param base the compressed vanilla block
     * @param tier the tier, {@code 1..MAX}
     * @return the translatable name
     */
    public static MutableComponent name(Block base, int tier) {
        return Component.translatable(NAME_KEY, Component.translatable(base.getDescriptionId()), tier);
    }
}
