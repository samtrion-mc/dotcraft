package net.samtrion.dotcraft.energy.thermo;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * The block entity of every thermoelectric generator: it reads its six neighbours every {@link #RECOMPUTE_TICKS} ticks (staggered
 * by position), looks at the three pairs of opposite faces (a heat source on one face and a cold source on the other), takes the
 * pair with the best temperature difference and derives its output from it (see {@link ThermoMath}), buffers what it makes and pushes FE into every neighbour with an FE capability. The sources are only read,
 * never consumed. The buffer can only be drained from outside.
 */
public class ThermoBlockEntity extends BlockEntity {
    /** Ticks between two reads of the neighbours. */
    static final int RECOMPUTE_TICKS = 20;

    /** The FE buffer. */
    private final ThermoEnergy energy;
    /** The FE capabilities of the neighbours, created on demand. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** The heat level of the best pair of opposite faces at the last read, 0 if none. */
    private int heat;
    /** The cold level of the best pair of opposite faces at the last read, 0 if none. */
    private int cold;
    /** The FE per tick made at the last read. */
    private double output;
    /** The fraction of an FE that is not yet made. */
    private double carry;
    /** Whether the stored FE changed since the chunk was last marked unsaved. */
    private boolean dirty;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link ThermoBlock}
     */
    public ThermoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = config();
        energy = new ThermoEnergy(config.capacity(), config.maxOutput(), () -> dirty = true);
    }

    /**
     * The config of this generator's tier.
     *
     * @return the FE values
     */
    private EnergyCommonConfig.Generator config() {
        return EnergyCommonConfig.THERMO_GENERATORS.get(tier() - 1);
    }

    /**
     * The tier of this generator's block.
     *
     * @return 1..5
     */
    private int tier() {
        return ((ThermoBlock) getBlockState().getBlock()).tier();
    }

    /**
     * The buffer, for the capability: it can be drained but not charged.
     *
     * @return the buffer
     */
    public EnergyHandler energy() {
        return energy;
    }

    /**
     * Puts FE into the buffer.
     *
     * @param amount the FE offered
     * @return the FE that fitted
     */
    public int fill(int amount) {
        return energy.produce(amount);
    }

    /**
     * The FE per tick made at the last read of the neighbours.
     *
     * @return the output
     */
    public double output() {
        return output;
    }

    /**
     * The heat level of the best pair of opposite faces at the last read.
     *
     * @return 0 if there is no heat source, else 1..{@link ThermoMath#MAX_LEVEL}
     */
    public int heatLevel() {
        return heat;
    }

    /**
     * The cold level of the best pair of opposite faces at the last read.
     *
     * @return 0 if there is no cold source, else 1..{@link ThermoMath#MAX_LEVEL}
     */
    public int coldLevel() {
        return cold;
    }

    /**
     * The share of the peak made at the last read.
     *
     * @return 0..1
     */
    public float share() {
        return ThermoMath.share(tier(), heat, cold);
    }

    /**
     * The comparator signal.
     *
     * @return 0..15
     */
    public int signal() {
        return ThermoMath.signal(share());
    }

    /**
     * One server tick: reads the neighbours now and then, makes FE and pushes it.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        if ((level.getGameTime() + worldPosition.hashCode()) % RECOMPUTE_TICKS == 0) recompute(level);
        carry += output;
        int whole = (int) carry;
        carry -= whole;
        energy.produce(whole);
        push(level);
        if (dirty && level.getGameTime() % RECOMPUTE_TICKS == 0) {
            dirty = false;
            level.blockEntityChanged(worldPosition); // saves the chunk without a comparator update
        }
    }

    /** Reads the three pairs of opposite neighbours and the config; tells the comparators and the client when the output changed. */
    private void recompute(ServerLevel level) {
        var config = config();
        energy.configure(config.capacity(), config.maxOutput());
        var best = ThermoMath.NONE;
        for (var axis : Direction.Axis.values()) {
            var face = Direction.get(Direction.AxisDirection.POSITIVE, axis);
            var a = worldPosition.relative(face);
            var b = worldPosition.relative(face.getOpposite());
            if (!level.isLoaded(a) || !level.isLoaded(b)) continue; // never force-load a chunk at a border
            var stateA = level.getBlockState(a);
            var stateB = level.getBlockState(b);
            best = ThermoMath.best(best, ThermoMath.axis(ThermoTags.heat(stateA), ThermoTags.cold(stateA), ThermoTags.heat(stateB), ThermoTags.cold(stateB)));
        }
        int bestHeat = best.heat();
        int bestCold = best.cold();
        double now = config.peak() * (double) ThermoMath.share(tier(), bestHeat, bestCold);
        boolean changed = now != output || bestHeat != heat || bestCold != cold;
        heat = bestHeat;
        cold = bestCold;
        output = now;
        if (changed) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            setChanged();
        }
    }

    /** Offers the buffer to the neighbours on every side; all sides together get at most the max output per tick. */
    private void push(ServerLevel level) {
        if (energy.getAmountAsLong() <= 0) return;
        int budget = config().maxOutput();
        for (Direction d : Direction.values()) {
            var cache = neighbours.computeIfAbsent(d,
                    dir -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(dir), dir.getOpposite()));
            budget -= EnergyHandlerUtil.move(energy, cache.getCapability(), budget, null);
            if (budget <= 0 || energy.getAmountAsLong() <= 0) return;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out);
        out.putInt("heat", heat);
        out.putInt("cold", cold);
        out.putDouble("output", output);
        out.putDouble("carry", carry);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy.deserialize(in);
        heat = in.getIntOr("heat", 0);
        cold = in.getIntOr("cold", 0);
        output = in.getDoubleOr("output", 0);
        carry = in.getDoubleOr("carry", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
