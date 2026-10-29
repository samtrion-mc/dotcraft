package net.samtrion.dotcraft.energy.vibration;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.generator.GeneratorEnergy;

/**
 * The block entity of a vibration generator. It listens to the game events of the world like a sculk sensor does (it is the
 * {@link GameEventListener} of its block, see {@link Listener}), turns the ones of the {@link VibrationTags#SOURCES} tag it can hear
 * into FE (see {@link VibrationMath}) and pushes the FE into the neighbours on every side. The hard cap is the {@link VibrationBucket}: no
 * event, however many, makes more than the duty of the tier ({@link VibrationMath#duty}) of the peak per tick on average. Events caused by
 * a boss (config list {@code vibration.bosses}) are worth {@link VibrationMath#BOSS_FACTOR} times as much; what the bucket cannot pay for them comes
 * from a second, bonus bucket, so a boss fight may exceed the duty cap but both buckets together never pass the peak per tick on average.
 * <p>
 * Which events count, as the sculk sensor sees it: spectators are not heard, nor are entities that dampen vibrations (wearing wool),
 * nor are sneaking entities for the events the vanilla tag {@code ignore_vibrations_sneaking} lists, nor events on blocks that
 * dampen vibrations. Wool between the event and the generator stops the vibration up to tier 3; tiers 4 and 5 hear through it.
 * Events outside the radius of the tier, or below the frequency it hears, do not count. A generator starts with a full bucket. A heard event shows as sparks above the block, a comparator reads the fill of the buffer.
 */
public class VibrationBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationBlockEntity.Listener> {
    /** Ticks between two reads of the config. */
    static final int CONFIG_TICKS = 20;

    /** The tier, 1..5. */
    private final int tier;
    /** The config of the tier: peak, buffer and output. */
    private final EnergyCommonConfig.Generator config;
    /** The FE buffer; it can only be drained from outside. */
    private final GeneratorEnergy energy;
    /** The game event listener of the block. */
    private final Listener listener = new Listener();
    /** The capability caches of the neighbours, by side. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** The hard cap of the output. */
    private VibrationBucket bucket = new VibrationBucket();
    /** The extra cap that only the events of bosses use, on top of the bucket. */
    private VibrationBucket bonus = new VibrationBucket();

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link VibrationBlock}
     */
    public VibrationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tier = ((VibrationBlock) state.getBlock()).tier();
        config = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1);
        energy = new GeneratorEnergy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    /**
     * The FE buffer, for the capability: it can only be drained.
     *
     * @return the buffer
     */
    public EnergyHandler energy() {
        return energy;
    }

    /**
     * The tier of the block.
     *
     * @return 1..5
     */
    public int tier() {
        return tier;
    }

    /**
     * The fill of the buffer as a comparator signal.
     *
     * @return 0 for an empty buffer, 15 for a full one, at least 1 for any FE in it
     */
    public int signal() {
        long capacity = energy.getCapacityAsLong();
        long amount = energy.getAmountAsLong();
        return capacity <= 0 || amount <= 0 ? 0 : (int) Math.max(1, amount * 15 / capacity);
    }

    /**
     * The bucket that limits the output, for tests and diagnostics.
     *
     * @return the bucket
     */
    public VibrationBucket bucket() {
        return bucket;
    }

    /**
     * The bonus bucket of boss events, for tests and diagnostics.
     *
     * @return the bucket
     */
    public VibrationBucket bonusBucket() {
        return bonus;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    /**
     * One server tick: reads the config now and then and pushes the FE into the neighbours; events are handled when they come.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        if ((level.getGameTime() + worldPosition.hashCode()) % CONFIG_TICKS == 0) energy.configure(config.capacity(), config.maxOutput());
        GeneratorEnergy.push(neighbours, level, worldPosition, energy, null);
    }

    /**
     * Takes the FE of a game event: the filters first, then the bucket.
     *
     * @param level   the level
     * @param event   the game event
     * @param context who or what caused it
     * @param source  where it happened
     * @return the FE made, 0 if the event does not count
     */
    long hear(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 source) {
        if (!event.is(VibrationTags.SOURCES)) return 0;
        int frequency = VibrationSystem.getGameEventFrequency(event);
        if (!VibrationMath.hears(tier, frequency)) return 0;
        var entity = context.sourceEntity();
        boolean boss = entity != null && EnergyCommonConfig.VIBRATION_BOSSES.filter().matches(entity.getType());
        if (entity != null && (entity.isSpectator() || entity.dampensVibrations() && !boss
                || entity.isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING))) return 0;
        var affected = context.affectedState();
        if (affected != null && affected.is(BlockTags.DAMPENS_VIBRATIONS)) return 0;
        if (!VibrationMath.pierces(tier) && isOccluded(level, source, Vec3.atCenterOf(worldPosition))) return 0;
        // a full buffer takes nothing, so the bucket keeps its FE and nothing is saved
        if (energy.getAmountAsLong() >= energy.getCapacityAsLong()) return 0;
        int peak = config.peak();
        // a boss event may exceed the duty cap: what the bucket cannot pay comes from the bonus bucket, which refills with the rest of the peak
        long granted = VibrationMath.grant(tier, peak, level.getGameTime(), VibrationMath.eventEnergy(peak, frequency), boss, bucket, bonus);
        if (granted <= 0) return 0;
        // what does not fit into the buffer is lost, as the bucket has given it
        energy.produce((int) Math.min(granted, Integer.MAX_VALUE));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0.02);
        return granted;
    }

    /** Whether a block that occludes vibration signals (wool) is in the way; the same rule as of the sculk sensor. */
    private static boolean isOccluded(Level level, Vec3 origin, Vec3 dest) {
        Vec3 from = new Vec3(Mth.floor(origin.x) + 0.5, Mth.floor(origin.y) + 0.5, Mth.floor(origin.z) + 0.5);
        Vec3 to = new Vec3(Mth.floor(dest.x) + 0.5, Mth.floor(dest.y) + 0.5, Mth.floor(dest.z) + 0.5);
        for (Direction direction : Direction.values()) {
            Vec3 nudged = from.relative(direction, 1.0E-5F);
            if (level.isBlockInLine(new ClipBlockStateContext(nudged, to, state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putLong("bucket", bucket.tokens());
        output.putLong("bucketTick", bucket.lastTick());
        output.putLong("bonus", bonus.tokens());
        output.putLong("bonusTick", bonus.lastTick());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        bucket = new VibrationBucket(input.getLongOr("bucket", 0), input.getLongOr("bucketTick", -1));
        bonus = new VibrationBucket(input.getLongOr("bonus", 0), input.getLongOr("bonusTick", -1));
    }

    /** The game event listener of the block: the radius is the one of the tier. */
    public final class Listener implements GameEventListener {
        /** The position of the block. */
        private final PositionSource source = new BlockPositionSource(worldPosition);

        @Override
        public PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return VibrationMath.radius(tier);
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 sourcePosition) {
            return hear(level, event, context, sourcePosition) > 0;
        }
    }
}
