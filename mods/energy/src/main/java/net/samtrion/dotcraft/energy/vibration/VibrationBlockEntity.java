package net.samtrion.dotcraft.energy.vibration;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.generator.GeneratorEnergy;

/**
 * The block entity of a vibration generator. It listens to the game events of the world like a sculk sensor does (it is the
 * {@link GameEventListener} of its block, see {@link Listener}), turns the ones of the {@link VibrationTags#SOURCES} tag it can hear
 * into FE (see {@link VibrationMath}) and pushes the FE into the neighbours on every side. The hard cap is the {@link VibrationBucket}: no
 * event, however many, makes more than the peak of the tier per tick on average. It keeps no record of the sources of the events.
 * <p>
 * Which events count, as the sculk sensor sees it: spectators are not heard, nor are entities that dampen vibrations (wearing wool),
 * nor are sneaking entities for the events the vanilla tag {@code ignore_vibrations_sneaking} lists, nor events on blocks that
 * dampen vibrations. Wool between the event and the generator stops the vibration up to tier 3; tiers 4 and 5 hear through it.
 * Events outside the radius of the tier, or below the frequency it hears, do not count. A generator starts with an empty bucket.
 */
public class VibrationBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationBlockEntity.Listener> {
    /** Ticks between two reads of the config. */
    static final int CONFIG_TICKS = 20;

    private final int tier;
    private final EnergyCommonConfig.Generator config;
    private final GeneratorEnergy energy;
    private final Listener listener = new Listener();
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    private VibrationBucket bucket = new VibrationBucket();

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
     * The bucket that limits the output, for tests and diagnostics.
     *
     * @return the bucket
     */
    public VibrationBucket bucket() {
        return bucket;
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
        if (energy.getAmountAsLong() <= 0) return;
        for (Direction d : Direction.values()) {
            var cache = neighbours.computeIfAbsent(d,
                    dir -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(dir), dir.getOpposite()));
            EnergyHandlerUtil.move(energy, cache.getCapability(), Integer.MAX_VALUE, null);
            if (energy.getAmountAsLong() <= 0) return;
        }
    }

    /**
     * Takes the FE of a game event: the filters first, then the bucket.
     *
     * @param level     the level
     * @param event     the game event
     * @param context   who or what caused it
     * @param source    where it happened
     * @return the FE made, 0 if the event does not count
     */
    long hear(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 source) {
        if (!event.is(VibrationTags.SOURCES)) return 0;
        int frequency = VibrationSystem.getGameEventFrequency(event);
        if (!VibrationMath.hears(tier, frequency)) return 0;
        var entity = context.sourceEntity();
        if (entity != null && (entity.isSpectator() || entity.dampensVibrations()
                || entity.isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING))) return 0;
        var affected = context.affectedState();
        if (affected != null && affected.is(BlockTags.DAMPENS_VIBRATIONS)) return 0;
        if (!VibrationMath.pierces(tier) && isOccluded(level, source, Vec3.atCenterOf(worldPosition))) return 0;
        int peak = config.peak();
        long granted = bucket.take(level.getGameTime(), VibrationMath.eventEnergy(peak, frequency), peak, VibrationMath.burst(peak));
        if (granted <= 0) return 0;
        // what does not fit into the buffer is lost, as the bucket has given it
        energy.produce((int) Math.min(granted, Integer.MAX_VALUE));
        setChanged();
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        bucket = new VibrationBucket(input.getLongOr("bucket", 0), input.getLongOr("bucketTick", -1));
    }

    /** The game event listener of the block: the radius is the one of the tier. */
    public final class Listener implements GameEventListener {
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
