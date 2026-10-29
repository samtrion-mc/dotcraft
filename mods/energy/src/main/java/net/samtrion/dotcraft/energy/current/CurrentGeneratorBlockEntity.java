package net.samtrion.dotcraft.energy.current;

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
import net.samtrion.dotcraft.energy.generator.GeneratorEnergy;

/**
 * The block entity of every current generator. It re-reads the surroundings (see {@link CurrentScan}) every
 * {@link #RECOMPUTE_TICKS} ticks, staggered by position so a farm of generators does not scan in the same tick, and once on its
 * first tick, buffers what it makes and pushes FE into the neighbours on every side. Unlike the panels it has no pool: every
 * generator is on its own, since each needs its own water. Its capability can only be drained.
 */
public class CurrentGeneratorBlockEntity extends BlockEntity {
    /** Ticks between two scans of the surroundings. */
    static final int RECOMPUTE_TICKS = 30;

    private final GeneratorEnergy energy;
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    private boolean scanned;
    private double output;
    private double carry;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link CurrentGeneratorBlock}
     */
    public CurrentGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = CurrentBlocks.CONFIGS.get(tier(state) - 1);
        energy = new GeneratorEnergy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    private static int tier(BlockState state) {
        return ((CurrentGeneratorBlock) state.getBlock()).tier();
    }

    /**
     * The FE buffer; nothing can be inserted into it, and one operation gives at most what the tier is configured to give.
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
     * The FE per tick made at the last scan.
     *
     * @return the output
     */
    public double output() {
        return output;
    }

    /**
     * One server tick: scans the world now and then, makes FE and pushes it.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        if (!scanned || Math.floorMod(level.getGameTime() + worldPosition.hashCode(), RECOMPUTE_TICKS) == 0) recompute(level);
        carry += output;
        int whole = (int) carry;
        carry -= whole;
        energy.produce(whole); // what does not fit into a full buffer is lost
        push(level);
    }

    /**
     * Re-reads the config and the surroundings now; a change of the output goes to the clients and the comparators. The server tick
     * calls it every {@link #RECOMPUTE_TICKS} ticks; it is public for the game tests.
     *
     * @param level the level
     */
    public void recompute(ServerLevel level) {
        scanned = true;
        int tier = tier(getBlockState());
        var config = CurrentBlocks.CONFIGS.get(tier - 1);
        energy.configure(config.capacity(), config.maxOutput());
        boolean submerged = getBlockState().getValue(CurrentGeneratorBlock.WATERLOGGED);
        double now = submerged ? CurrentScan.scan(level, worldPosition, tier).output(tier, true) * config.peak() : 0;
        if (now != output) {
            output = now;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            setChanged();
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
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out);
        out.putDouble("output", output);
        out.putDouble("carry", carry);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy.deserialize(in);
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
