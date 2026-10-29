package net.samtrion.dotcraft.energy.kite.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

/** What the {@link KiteRenderer} needs of a kite: its tier, where the top of its block is relative to it, and a phase for the sway. */
public class KiteRenderState extends EntityRenderState {
    /** The tier, 1..5; picks the texture and the size. */
    public int tier = 1;
    /** From the kite to the centre of the top of its generator block, in blocks. */
    public Vec3 anchorOffset = Vec3.ZERO;
    /** A phase per block position, 0..255, so neighbouring kites do not move in step. */
    public int phase;
}
