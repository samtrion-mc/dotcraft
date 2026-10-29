package net.samtrion.dotcraft.energy.wheel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
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
 * The block entity of a mob wheel. Every {@link WheelMath#SCAN_TICKS} ticks (staggered by position) it scans the zone around the
 * wheel, a box that reaches {@link WheelMath#radius} blocks sideways, one block down and two blocks above the wheel. A mob counts
 * as a runner when
 * <ul>
 * <li>its type is in the tag {@link WheelTags#RUNNERS} (the tags {@link WheelTags#STRONG} and {@link WheelTags#WEAK} set its weight),</li>
 * <li>it is alive and no baby, stands on the ground (not in water, so no current can push it), rides nothing, and is no tamed animal that is told to sit,</li>
 * <li>it has at least half of its health (and counts by its health share), the welfare rule, and</li>
 * <li>it moved at least {@link WheelMath#MIN_MOVE} blocks since the scan before (a mob seen for the first time counts nothing yet) and
 * no other wheel counts it, see {@link WheelClaims}.</li>
 * </ul>
 * The weights go through {@link WheelMath#load}: the cap and the diminishing returns of the tier bound the output. The FE are
 * buffered and pushed into the neighbours on every side; the buffer can only be drained.
 * <p>
 * Open decisions, all the simplest robust option: a mob counts by its movement, not by an intention; there is no feed check (the
 * welfare rule is the health); a mob pushed along by a piston counts if it is on the ground when scanned. The scan has a one
 * second granularity: the output holds between two scans.
 */
public class WheelBlockEntity extends BlockEntity {
    /** One claim ledger for all wheels: a mob drives one wheel only. */
    private static final WheelClaims<LivingEntity, WheelBlockEntity> CLAIMS = new WheelClaims<>();

    /** The buffer; only the block entity fills it, neighbours can drain it. */
    private final Energy energy;
    /** The capability caches of the six neighbours, created when the wheel first pushes. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** The positions of the eligible mobs at the last scan, by UUID. */
    private Map<UUID, Vec3> seen = new HashMap<>();
    /** The FE per tick made since the last scan. */
    private double output;
    /** The share of the peak, 0..1, which the comparator shows. */
    private double load;
    /** The fraction of an FE that did not fit into this tick's whole FE. */
    private double carry;
    /** The number of counted runners at the last scan. */
    private int runners;

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
     * How many mobs ran in the zone at the last scan and counted.
     *
     * @return the number of runners
     */
    public int runners() {
        return runners;
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
     * Whether a mob may run a wheel at all, apart from its movement.
     *
     * @param mob the mob
     * @return {@code true} if it is a runner type, alive, grown up, on the ground, out of water and free to move
     */
    static boolean eligible(LivingEntity mob) {
        return mob.isAlive() && !mob.isBaby() && !mob.isInLiquid() && !mob.isPassenger() && mob.onGround()
                && !(mob instanceof TamableAnimal pet && pet.isOrderedToSit())
                && WheelTags.strength(mob.getType()) != WheelMath.Strength.NONE;
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
     * Reads the world: looks at the mobs in the zone and sets the output. The tick runs it every second; it is public for tests.
     *
     * @param level the level
     */
    public void scan(ServerLevel level) {
        int tier = tier();
        var config = config();
        energy.configure(config.capacity(), config.maxOutput());
        var zone = new AABB(worldPosition).inflate(WheelMath.radius(tier), 0, WheelMath.radius(tier)).expandTowards(0, 2, 0).expandTowards(0, -1, 0);
        var now = new HashMap<UUID, Vec3>();
        record Candidate(LivingEntity mob, double weight) {}
        var candidates = new ArrayList<Candidate>();
        for (var mob : level.getEntitiesOfClass(LivingEntity.class, zone, WheelBlockEntity::eligible)) {
            var position = mob.position();
            now.put(mob.getUUID(), position);
            var before = seen.get(mob.getUUID());
            if (before == null || !WheelMath.moved(position.x - before.x, position.z - before.z)) continue;
            double weight = WheelMath.weight(WheelTags.strength(mob.getType()), tier) * WheelMath.welfare(mob.getHealth(), mob.getMaxHealth());
            if (weight > 0) candidates.add(new Candidate(mob, weight));
        }
        seen = now;
        // the strongest first; only the mobs within the cap are claimed, so a cheap wheel never starves a stronger one with the rest
        candidates.sort(Comparator.comparingDouble(Candidate::weight).reversed());
        var weights = new ArrayList<Double>();
        for (var c : candidates) {
            if (weights.size() >= WheelMath.cap(tier)) break;
            if (CLAIMS.claim(c.mob(), this, level.getGameTime())) weights.add(c.weight());
        }
        double peak = config.peak();
        double target = peak * WheelMath.load(weights.stream().mapToDouble(Double::doubleValue).toArray(), tier);
        double next = WheelMath.coast(target, output, tier);
        int before = signal();
        runners = Math.min(weights.size(), WheelMath.cap(tier));
        output = next;
        load = peak > 0 ? Math.min(1, next / peak) : 0;
        if (signal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        setChanged();
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
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy.deserialize(in);
        output = in.getDoubleOr("output", 0);
        carry = in.getDoubleOr("carry", 0);
        load = in.getDoubleOr("load", 0);
        runners = in.getIntOr("runners", 0);
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
