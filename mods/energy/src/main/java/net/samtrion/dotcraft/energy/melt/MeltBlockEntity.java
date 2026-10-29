package net.samtrion.dotcraft.energy.melt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The block entity of a melt generator. Every {@link MeltMath.Tier#interval()} ticks (staggered by position) it checks whether a
 * block of the {@link MeltTags#HEAT} tag touches it; if one does, it melts up to {@link MeltMath.Tier#slots()} blocks of ice that touch
 * it on the other sides (see {@link MeltIce}): they turn to air; on tier 4 and 5 their water (one bucket, on tier 5 all the water they are worth, see {@link MeltMath#waterBlocks}) goes into the
 * water tank of {@link MeltMath#TANK_CAPACITY_MB} mB, what does not fit is lost; their FE goes into the heat reservoir, a quarter of it while the tank is full
 * (see {@link MeltMath#effectiveYield}).
 * Every tick the reservoir releases at most the peak into the buffer (see {@link MeltMath}), and the buffer is pushed into the
 * neighbours that take FE. When the block is removed, the FE of the reservoir and the buffer is handed to the neighbours that take FE; what they do not
 * take is lost. A neighbour in a chunk that is not loaded is never touched, so a generator never loads chunks.
 */
public class MeltBlockEntity extends BlockEntity {
    private final MeltEnergy energy;
    private final MeltTank tank = new MeltTank(this::setChanged);
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    private long pending;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link MeltBlock}
     */
    public MeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = config(state);
        energy = new MeltEnergy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    private static EnergyCommonConfig.Generator config(BlockState state) {
        return EnergyCommonConfig.MELT_GENERATORS.get(((MeltBlock) state.getBlock()).tier() - 1);
    }

    /**
     * The buffer, for the capability: nothing can be inserted, and one operation gives at most the configured output.
     *
     * @return the buffer
     */
    public EnergyHandler energy() {
        return energy;
    }

    /**
     * The water tank, for the capability: nothing can be inserted, it can only be drained.
     *
     * @return the tank
     */
    public ResourceHandler<FluidResource> tank() {
        return tank;
    }

    /**
     * The water in the tank.
     *
     * @return the amount in mB
     */
    public int water() {
        return tank.amount();
    }

    /**
     * Whether the tank is full, which cuts the FE of the ice to a quarter.
     *
     * @return {@code true} if the tank is full
     */
    public boolean tankFull() {
        return tank.isFull();
    }

    /**
     * The FE in the buffer.
     *
     * @return the FE
     */
    public int stored() {
        return energy.getAmountAsInt();
    }

    /**
     * The FE of melted ice that has not been released into the buffer yet.
     *
     * @return the FE of the heat reservoir
     */
    public long pending() {
        return pending;
    }

    /**
     * Whether a heat source touches the generator.
     *
     * @param level the level
     * @param pos   the position of the generator
     * @return {@code true} if one of the six neighbours is in {@link MeltTags#HEAT} and, if it can burn out, is lit
     */
    public static boolean isHeated(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (!level.isLoaded(pos.relative(d))) continue;
            var state = level.getBlockState(pos.relative(d));
            if (state.is(MeltTags.HEAT) && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))) return true;
        }
        return false;
    }

    /**
     * One server tick: melts now and then, releases the reservoir, shows the glow and pushes FE.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        var state = getBlockState();
        var config = config(state);
        var tier = MeltMath.tier(((MeltBlock) state.getBlock()).tier());
        energy.configure(config.capacity(), config.maxOutput());
        if ((level.getGameTime() + worldPosition.hashCode()) % tier.interval() == 0) melt(level, tier, config.peak());
        int released = MeltMath.release(pending, config.peak(), energy.room());
        if (released > 0) {
            pending -= released;
            energy.produce(released);
            setChanged();
        }
        boolean lit = pending > 0;
        if (lit != state.getValue(MeltBlock.LIT)) level.setBlock(worldPosition, state.setValue(MeltBlock.LIT, lit), 2);
        push(level);
    }

    /**
     * One pulse: melts the ice next to the generator if it is heated.
     *
     * @param level the level
     * @param tier  the perks of the tier
     * @param peak  the peak in FE/t
     */
    private void melt(ServerLevel level, MeltMath.Tier tier, int peak) {
        if (!isHeated(level, worldPosition)) return;
        int melted = 0;
        for (Direction d : Direction.values()) {
            if (melted >= tier.slots()) break;
            var at = worldPosition.relative(d);
            if (!level.isLoaded(at)) continue;
            var ice = MeltIce.of(level.getBlockState(at));
            if (ice == null) continue;
            int water = MeltMath.waterBlocks(tier, level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, at),
                    ice.yield(), MeltIce.ICE.yield());
            int yield = MeltMath.effectiveYield(ice.yield(), tank.isFull());
            if (!MeltMath.mayMelt(pending, yield, peak)) continue;
            level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            tank.fill(water * MeltMath.BUCKET_MB);
            level.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
            level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.01);
            pending += yield;
            melted++;
        }
        if (melted > 0) setChanged();
    }

    /**
     * The comparator signal: how full the reservoir and the buffer are together.
     *
     * @return 0..15
     */
    public int signal() {
        var config = config(getBlockState());
        return MeltMath.signal(pending + energy.getAmountAsLong(), MeltMath.reserve(config.peak()) + config.capacity());
    }

    /** Hands the FE of the reservoir and the buffer to the neighbours that take it; the rest is lost. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        int total = (int) Math.min(Integer.MAX_VALUE, pending + energy.getAmountAsLong());
        if (total <= 0) return;
        var targets = new ArrayList<EnergyHandler>();
        for (Direction d : Direction.values()) {
            var at = pos.relative(d);
            if (!level.isLoaded(at)) continue;
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, at, d.getOpposite());
            if (handler != null) targets.add(handler);
        }
        MeltEnergy.handOff(total, targets);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putLong("pending", pending);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        pending = input.getLongOr("pending", 0);
        input.child("tank").ifPresent(tank::deserialize);
    }
}
