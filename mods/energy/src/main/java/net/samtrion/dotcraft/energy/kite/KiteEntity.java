package net.samtrion.dotcraft.energy.kite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The kite on its tether: it hangs straight above its generator block (same x and z, so it always lives in the chunk of the block
 * and is ticked and unloaded with it) and moves only up and down, towards the target altitude the block entity gives it. Its height
 * is <b>not</b> limited by the build height: an entity can be anywhere above the world (only below the bottom of the world it is
 * removed), it is tracked by the players around and it keeps ticking, which the game tests verify.
 * <p>
 * It is the block entity that owns the kite: the entity type is not saved ({@code noSave}), so there is never a stray kite after a
 * restart or a chunk reload; the block entity remembers that it has a kite and spawns a new one (which climbs again from the block)
 * when its chunk is ticking and none is there. A kite whose block is gone or that is not the block's current kite discards itself.
 * A tier 4 or 5 block has two kites (slot 0 and 1) that hang above the same block and are told apart by their slot; a kite that is not in
 * a slot of its block (a duplicate) discards itself. Only the anchor, the tier and the slot are synchronized, the client draws the tether and the sway from them.
 */
public class KiteEntity extends Entity {
    /** The synchronized position of the generator block. */
    private static final EntityDataAccessor<BlockPos> ANCHOR = SynchedEntityData.defineId(KiteEntity.class, EntityDataSerializers.BLOCK_POS);
    /** The synchronized tier of the generator block. */
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(KiteEntity.class, EntityDataSerializers.INT);
    /** The synchronized slot of the kite at its block: 0, or 1 for the second kite of tier 4 and 5. */
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(KiteEntity.class, EntityDataSerializers.INT);
    /** Ticks between two checks that the block still has this kite. */
    private static final int CHECK_TICKS = 20;
    /** Distance in blocks from which the kite and its tether are drawn. */
    private static final double RENDER_DISTANCE = 256;

    /** The altitude above the top of the block; the server's truth (not synchronized, the position carries it to the client). */
    private double altitude;
    /** The altitude the kite flies to. */
    private double target;

    /**
     * Creates the kite; invoked by the entity type.
     *
     * @param type  the entity type
     * @param level the level
     */
    public KiteEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * Ties the kite to its block, directly above it.
     *
     * @param anchor the position of the generator block
     * @param tier   the tier of the block, 1..5
     * @param slot   0 for the first kite, 1 for the second
     */
    void attach(BlockPos anchor, int tier, int slot) {
        entityData.set(ANCHOR, anchor);
        entityData.set(TIER, tier);
        entityData.set(SLOT, slot);
        setAltitude(0);
    }

    /**
     * The position of the generator block, for the tether.
     *
     * @return the anchor
     */
    public BlockPos anchor() {
        return entityData.get(ANCHOR);
    }

    /**
     * The tier of the block, for the look and the speed.
     *
     * @return 1..5
     */
    public int tier() {
        return entityData.get(TIER);
    }

    /**
     * The slot of the kite at its block.
     *
     * @return 0 for the first kite, 1 for the second kite of tier 4 and 5
     */
    public int slot() {
        return entityData.get(SLOT);
    }

    /**
     * The altitude above the top of the block.
     *
     * @return the blocks between the top of the block and the kite; not limited by the build height
     */
    public double altitude() {
        return altitude;
    }

    /**
     * Puts the kite at an altitude at once (for the first placement and for tests).
     *
     * @param value the blocks above the top of the block
     */
    void setAltitude(double value) {
        altitude = Math.max(0, value);
        var anchor = anchor();
        setPos(anchor.getX() + 0.5, anchor.getY() + 1 + altitude, anchor.getZ() + 0.5);
    }

    /**
     * Tells the kite where to fly; it moves there by itself, up slowly (faster on a longer tether), down twice as fast.
     *
     * @param altitude the target altitude above the top of the block
     */
    void setTarget(double altitude) {
        target = Math.max(0, altitude);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHOR, BlockPos.ZERO);
        builder.define(TIER, 1);
        builder.define(SLOT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount % CHECK_TICKS == 0 && server.hasChunkAt(anchor())
                && !(server.getBlockEntity(anchor()) instanceof KiteBlockEntity be && be.owns(this))) {
            discard();
            return;
        }
        double speed = 0.25 + KiteMath.tether(tier()) / 1000.0;
        if (altitude < target) setAltitude(Math.min(target, altitude + speed));
        else if (altitude > target) setAltitude(Math.max(target, altitude - 2 * speed));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // not saved: see the class comment
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // not saved: see the class comment
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    /** The kite is drawn from far away, as long as the camera is near the kite or near its block (the tether is drawn with it). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < RENDER_DISTANCE * RENDER_DISTANCE;
    }

    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        var anchor = anchor();
        double dx = camX - (anchor.getX() + 0.5);
        double dy = camY - anchor.getY();
        double dz = camZ - (anchor.getZ() + 0.5);
        return shouldRenderAtSqrDistance(dx * dx + dy * dy + dz * dz) || super.shouldRender(camX, camY, camZ);
    }
}
