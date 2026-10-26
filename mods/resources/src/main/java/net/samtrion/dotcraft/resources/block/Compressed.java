package net.samtrion.dotcraft.resources.block;

/** Implemented by every compressed block class, whatever vanilla block type it extends. */
public interface Compressed {
    /**
     * The compression tier of this block.
     *
     * @return the tier, {@code 1..MAX}
     */
    int tier();
}
