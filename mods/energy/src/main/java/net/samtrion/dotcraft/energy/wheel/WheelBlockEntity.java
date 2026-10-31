package net.samtrion.dotcraft.energy.wheel;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The block entity of a mob wheel. A wheel holds <b>one mob at a time</b>. Every {@link WheelMath#SCAN_TICKS} ticks (staggered by
 * position) a wheel without a mob looks at the mobs that stand on the running surface of the wheel, see {@link WheelPlate}, and
 * captures the one nearest to the middle of the plate; other mobs may stand beside it but are not counted and not captured while the
 * slot is taken. A mob can be captured when
 * <ul>
 * <li>it is a {@link Mob}: any mob, vanilla or modded, friendly or hostile (players never run a wheel); its weight comes from its maximum
 * health and base movement speed alone, see {@link WheelMath#weight},</li>
 * <li>its feet are on the plate (the footprint of the tread of the model, at the height of the collision shape),</li>
 * <li>it is alive and no baby, stands on the ground (not in water), rides nothing, is no tamed animal that is told to sit and is not
 * leashed to anything but a fence knot,</li>
 * <li>its type is not in the config list {@code wheel.blocked_mobs} ({@link EnergyCommonConfig#WHEEL_BLOCKED_MOBS}: the bosses by default;
 * a held mob that is added to the list is released at the next scan), and</li>
 * <li>no other wheel holds it, see {@link WheelClaims}.</li>
 * </ul>
 * A captured mob <b>cannot leave</b>: right after every tick of the mob the wheel cancels its horizontal movement, puts it back on its
 * spot on the plate and stops its navigation ({@link #hold}); its goals still run, so it still animates walking or fleeing, but its real
 * movement is zero. Nothing protects it: it can be hurt and killed (no healing, no health rule), and a dead or removed mob frees the
 * slot at the next scan. A captured mob never despawns: the wheel sets its persistence flag (the flag is saved on the mob) and puts the
 * flag back when it lets the mob go because the wheel was broken or the mob got away (a mob that dies is irrelevant). A wheel that
 * reloads picks the same mob up again from its saved id. The mob counts fully while it tries to move (a path to follow, a move wanted by
 * its goals or a walk animation, sampled every tick), else {@link WheelMath#STANDING_FACTOR} of its weight, see {@link WheelMath#share}.
 * The FE are buffered and pushed into the neighbours on every side; the buffer can only be drained.
 * <p>
 * Open decisions, all the simplest robust option: babies are never captured; there is no feed check; a mob that a piston or a
 * player pushes more than 1.5 blocks away or lifts a block high is let go. The scan has a one second granularity: the output holds
 * between two scans.
 */
public class WheelBlockEntity extends BlockEntity {
    /** One claim ledger for all wheels: a mob drives one wheel only. */
    private static final WheelClaims<Mob, WheelBlockEntity> CLAIMS = new WheelClaims<>();
    /** Walk animation speed above which a mob counts as moving its legs. */
    private static final float WALK_THRESHOLD = 0.05F;
    /** How far in blocks a held mob may be from the middle of the wheel before it is let go. */
    private static final double ESCAPE_DISTANCE = 1.5;
    /** The field of {@link Mob} that keeps it from despawning, to put it back; {@code null} if the field is not found. */
    private static final @Nullable Field PERSISTENCE = persistenceField();

    /** The buffer; only the block entity fills it, neighbours can drain it. */
    private final Energy energy;
    /** The capability caches of the six neighbours, created when the wheel first pushes. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** The mob on the wheel, {@code null} if the slot is free or the mob has not been found again after a load. */
    private @Nullable Mob held;
    /** The id of the held mob, saved so a reloaded wheel picks the same mob up again. */
    private @Nullable UUID heldId;
    /** Whether the held mob was persistent before the wheel captured it, to put it back. */
    private boolean wasPersistent;
    /** The spot of the held mob on the plate. */
    private Vec3 pin = Vec3.ZERO;
    /** Whether the held mob tried to move since the last scan. */
    private boolean trying;
    /** The FE per tick made since the last scan. */
    private double output;
    /** The share of the peak, 0..1, which the comparator shows. */
    private double load;
    /** The fraction of an FE that did not fit into this tick's whole FE. */
    private double carry;
    /** The number of mobs held at the last scan, 0 or 1. */
    private int runners;
    /** Whether the held mob counted as running at the last scan; not saved. */
    private boolean running;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link WheelBlock}
     */
    public WheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = config();
        energy = new Energy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    private int tier() {
        return ((WheelBlock) getBlockState().getBlock()).tier();
    }

    private EnergyCommonConfig.Generator config() {
        return EnergyCommonConfig.MOB_WHEELS.get(tier() - 1);
    }

    private static @Nullable Field persistenceField() {
        try {
            var field = Mob.class.getDeclaredField("persistenceRequired");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null; // ponytail: a released mob then stays persistent; an access transformer would make this a plain field write
        }
    }

    /**
     * The buffer, for the capability: nothing can be inserted, one operation gives at most the configured output.
     *
     * @return the buffer
     */
    public EnergyHandler energy() {
        return energy;
    }

    /**
     * The FE per tick made since the last scan.
     *
     * @return the output
     */
    public double output() {
        return output;
    }

    /**
     * How many mobs the wheel held at the last scan.
     *
     * @return 1 if a mob is on the wheel, else 0
     */
    public int runners() {
        return runners;
    }

    /**
     * The mob on the wheel.
     *
     * @return the held mob, {@code null} if the slot is free
     */
    public @Nullable Mob heldMob() {
        return held;
    }

    /**
     * Whether the held mob counted as running (full weight) at the last scan, for diagnostics.
     *
     * @return {@code true} if the mob tried to move, {@code false} if it stood or no mob is held
     */
    public boolean running() {
        return running;
    }

    /**
     * The load for a redstone comparator.
     *
     * @return 0 when the wheel stands still, 15 at the peak
     */
    public int signal() {
        return (int) Math.ceil(load * 15);
    }

    /**
     * Whether a mob may be captured at all, apart from where it stands.
     *
     * @param mob the mob
     * @return {@code true} if it is alive, grown up, on the ground, out of water, free to move and not led by a
     *         leash (a fence knot is fine: a leash to the fence next to the wheel keeps a mob on it) and not of a type in {@code wheel.blocked_mobs}
     */
    static boolean eligible(Mob mob) {
        return !blocked(mob) && mob.isAlive() && !mob.isBaby() && !mob.isInLiquid() && !mob.isPassenger() && mob.onGround() && free(mob);
    }

    /** Whether the type of the mob is in the config list {@code wheel.blocked_mobs}, which no wheel captures or keeps. */
    static boolean blocked(Mob mob) {
        return EnergyCommonConfig.WHEEL_BLOCKED_MOBS.filter().matches(mob.getType());
    }

    /** The rules that also keep a held mob: not led away by a leash, not told to sit. */
    private static boolean free(Mob mob) {
        return !(mob instanceof Leashable leashed && leashed.isLeashed() && !(leashed.getLeashHolder() instanceof LeashFenceKnotEntity))
                && !(mob instanceof TamableAnimal pet && pet.isOrderedToSit());
    }

    /** Whether the wheel still keeps its mob: not blocked (the list may have changed), alive, here, free and not lifted or pushed off. */
    private boolean keeps(ServerLevel level, Mob mob) {
        return !blocked(mob) && mob.isAlive() && !mob.isRemoved() && mob.level() == level && !mob.isPassenger() && !mob.isInLiquid() && free(mob)
                && Math.abs(mob.getX() - (worldPosition.getX() + 0.5)) <= ESCAPE_DISTANCE && Math.abs(mob.getZ() - (worldPosition.getZ() + 0.5)) <= ESCAPE_DISTANCE
                && Math.abs(mob.getY() - (worldPosition.getY() + WheelPlate.TOP)) <= 1;
    }

    /** Whether a mob tries to move: a path to follow, a move wanted by its goals (which leaves the forward impulse set after the tick) or a walk animation. */
    static boolean tries(Mob mob) {
        return mob.getNavigation().isInProgress() || mob.getMoveControl().hasWanted() || Math.abs(mob.zza) > WALK_THRESHOLD || Math.abs(mob.xxa) > WALK_THRESHOLD
                || mob.walkAnimation.speed() > WALK_THRESHOLD;
    }

    /**
     * Holds the mob on its spot; called by {@link WheelPins} right after every tick of the mob: notes whether it tried to move, stops its
     * navigation and cancels its movement, and puts it back on its spot if it was displaced.
     *
     * @param mob the held mob
     */
    void hold(Mob mob) {
        if (mob != held) return;
        trying |= tries(mob);
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, Math.min(0, mob.getDeltaMovement().y), 0);
        if (mob.getX() != pin.x || mob.getZ() != pin.z) mob.setPos(pin.x, mob.getY(), pin.z);
    }

    /**
     * One server tick: scans the zone now and then, makes FE and pushes it.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        if ((level.getGameTime() + worldPosition.hashCode()) % WheelMath.SCAN_TICKS == 0) scan(level);
        carry += output;
        int whole = (int) carry;
        carry -= whole;
        energy.produce(whole);
        push(level);
    }

    /**
     * Reads the world: keeps or frees the held mob, captures one if the slot is free, and sets the output. The tick runs it every
     * second; it is public for tests.
     *
     * @param level the level
     */
    public void scan(ServerLevel level) {
        int tier = tier();
        var config = config();
        energy.configure(config.capacity(), config.maxOutput());
        if (held != null && !keeps(level, held)) release();
        if (held == null) capture(level);
        double target = 0;
        if (held != null) {
            CLAIMS.claim(held, this, level.getGameTime());
            running = trying || tries(held);
            target = config.peak() * WheelMath.share(WheelMath.weight(held.getMaxHealth(), held.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), tier), running);
        }
        if (held == null) running = false;
        trying = false;
        double peak = config.peak();
        double next = WheelMath.coast(target, output, tier);
        int before = signal();
        runners = held != null ? 1 : 0;
        output = next;
        load = peak > 0 ? Math.min(1, next / peak) : 0;
        if (signal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        setChanged();
    }

    /** Captures the mob on the plate that is nearest to its middle (the one saved before a reload first); a mob another wheel holds is left alone. */
    private void capture(ServerLevel level) {
        // the search box only has to reach the bounding box of a mob on the plate; the plate decides
        var search = new AABB(worldPosition).inflate(1, 0, 1).expandTowards(0, 2, 0);
        boolean sideways = getBlockState().getValue(WheelBlock.FACING).getAxis() == Direction.Axis.X;
        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var mob : level.getEntitiesOfClass(Mob.class, search, WheelBlockEntity::eligible)) {
            var p = mob.position();
            if (!WheelPlate.carries(sideways, p.x - worldPosition.getX(), p.y - worldPosition.getY(), p.z - worldPosition.getZ())) continue;
            if (!CLAIMS.free(mob, this, level.getGameTime())) continue;
            double distance = mob.getUUID().equals(heldId) ? -1 : p.distanceToSqr(worldPosition.getX() + 0.5, p.y, worldPosition.getZ() + 0.5);
            if (distance < bestDistance) {
                best = mob;
                bestDistance = distance;
            }
        }
        if (best == null) return;
        held = best;
        heldId = best.getUUID();
        pin = best.position();
        wasPersistent = best.isPersistenceRequired();
        best.setPersistenceRequired();
        WheelPins.pin(best, this);
        setChanged();
    }

    /** Lets the held mob go: it is no longer pinned and gets its old persistence back (a mob that died is not touched). */
    private void release() {
        var mob = held;
        held = null;
        heldId = null;
        if (mob == null) return;
        WheelPins.unpin(mob, this);
        if (mob.isAlive() && !wasPersistent) clearPersistence(mob);
        setChanged();
    }

    /**
     * Takes the persistence flag off a mob again (the game only offers to set it), so it may despawn as before.
     *
     * @param mob the mob
     */
    static void clearPersistence(Mob mob) {
        if (PERSISTENCE == null) return;
        try {
            PERSISTENCE.setBoolean(mob, false);
        } catch (IllegalAccessException e) {
            // the mob stays persistent
        }
    }

    private void push(ServerLevel level) {
        if (energy.getAmountAsLong() <= 0) return;
        for (Direction d : Direction.values()) {
            var cache = neighbours.computeIfAbsent(d,
                    dir -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(dir), dir.getOpposite()));
            EnergyHandlerUtil.move(energy, cache.getCapability(), Integer.MAX_VALUE, null);
            if (energy.getAmountAsLong() <= 0) return;
        }
    }

    @Override
    public void setRemoved() {
        release();
        CLAIMS.release(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out);
        out.putDouble("output", output);
        out.putDouble("carry", carry);
        out.putDouble("load", load);
        out.putInt("runners", runners);
        if (heldId != null) {
            out.putString("held", heldId.toString());
            out.putBoolean("wasPersistent", wasPersistent);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy.deserialize(in);
        output = in.getDoubleOr("output", 0);
        carry = in.getDoubleOr("carry", 0);
        load = in.getDoubleOr("load", 0);
        runners = in.getIntOr("runners", 0);
        var id = in.getStringOr("held", "");
        try {
            heldId = id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            heldId = null;
        }
        wasPersistent = in.getBooleanOr("wasPersistent", false);
    }

    /** The buffer of a wheel: nothing can be inserted from outside, the block entity fills it; extraction is limited by the config. */
    private static final class Energy extends SimpleEnergyHandler {
        private final Runnable changed;

        /**
         * Creates an empty buffer.
         *
         * @param capacity  the capacity
         * @param maxOutput the most FE extracted per operation
         * @param changed   called when the stored FE changes
         */
        Energy(int capacity, int maxOutput, Runnable changed) {
            super(capacity, 0, maxOutput);
            this.changed = changed;
        }

        /** Applies the config values; the stored FE is capped to a smaller capacity. */
        void configure(int newCapacity, int newMaxOutput) {
            capacity = newCapacity;
            maxExtract = newMaxOutput;
            if (energy > capacity) set(capacity);
        }

        /** Adds produced FE up to the capacity; what does not fit is lost. */
        void produce(int amount) {
            if (amount <= 0 || energy >= capacity) return;
            set((int) Math.min(capacity, (long) energy + amount));
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            changed.run();
        }
    }
}
