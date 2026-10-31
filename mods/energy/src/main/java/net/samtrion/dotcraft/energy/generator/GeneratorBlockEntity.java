package net.samtrion.dotcraft.energy.generator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The block entity of every generator block, of every family and tier: it re-reads its output (zero while something stands on it) every {@link #RECOMPUTE_TICKS} ticks
 * (staggered by position), buffers what it makes and pushes FE into the neighbours on every side but the top. Generators that
 * touch each other sideways form a group (see {@link GeneratorGroup}) with one shared pool of FE: production that does not fit
 * into a block's own buffer goes to the others, and what a neighbour takes comes out of the whole pool, limited by the config of
 * the block it is taken from. The family and the tier come from the block, so there is one class and one block entity type per
 * family.
 */
public class GeneratorBlockEntity extends BlockEntity {
    /** The most generators one group (one shared pool of FE) holds. */
    public static final int MAX_GROUP = GeneratorGroup.MAX_SIZE;

    /** Ticks between two reads of the world. */
    static final int RECOMPUTE_TICKS = 20;

    private final GeneratorEnergy energy;
    private final Pool pool = new Pool();
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    private List<GeneratorBlockEntity> members = List.of();
    private int membersVersion = -1;
    private double output;
    private double carry;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link GeneratorBlock}
     */
    public GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = block().kind().configs().get(block().tier() - 1);
        energy = new GeneratorEnergy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    private GeneratorBlock block() {
        return (GeneratorBlock) getBlockState().getBlock();
    }

    /**
     * The FE of the whole group, for the capability: the sum of the buffers of all generators that touch this one sideways. Nothing
     * can be inserted into it, and one operation gives at most what this block's own tier is configured to give.
     *
     * @return the pool
     */
    public EnergyHandler energy() {
        return pool;
    }

    /**
     * Puts FE into the buffer of this block only.
     *
     * @param amount the FE offered
     * @return the FE that fitted
     */
    public int fill(int amount) {
        return energy.produce(amount);
    }

    /**
     * The FE per tick made at the last read of the world.
     *
     * @return the output
     */
    public double output() {
        return output;
    }

    /**
     * How many generators share this block's pool of FE, this block included.
     *
     * @return the size of the group, 1 for a block that touches no other generator
     */
    public int groupSize() {
        return members().size();
    }

    /**
     * The generators of this block's group, this block first; cached until a generator is placed or removed anywhere.
     *
     * @return the members
     */
    List<GeneratorBlockEntity> members() {
        if (membersVersion != GeneratorGroup.version() && level != null) {
            members = GeneratorGroup.members(level, worldPosition, null);
            membersVersion = GeneratorGroup.version();
        }
        return members.isEmpty() ? List.of(this) : members;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        GeneratorGroup.invalidate();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        GeneratorGroup.invalidate();
    }

    /**
     * Hands the FE of a removed block on: the groups its neighbours would be in without it get an equal part each, spread over their
     * blocks; what does not fit its group, and the remainder of the division, goes to the other groups; only what fits nowhere is lost.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        int stored = energy.getAmountAsInt();
        if (level == null || level.isClientSide() || stored <= 0) return;
        var groups = new ArrayList<List<GeneratorBlockEntity>>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockEntity(pos.relative(d)) instanceof GeneratorBlockEntity neighbour)) continue;
            if (groups.stream().anyMatch(g -> g.contains(neighbour))) continue;
            var group = GeneratorGroup.members(level, neighbour.getBlockPos(), pos);
            if (!group.isEmpty()) groups.add(group);
        }
        if (groups.isEmpty()) return;
        int share = stored / groups.size();
        int lost = stored - share * groups.size();
        for (var group : groups) {
            int left = share;
            for (var member : group) left -= member.fill(left);
            lost += left;
        }
        for (var group : groups) {
            for (var member : group) lost -= member.fill(lost);
        }
    }

    /**
     * One server tick: reads the world now and then, makes FE and pushes it.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        if ((level.getGameTime() + worldPosition.hashCode()) % RECOMPUTE_TICKS == 0) recompute(level);
        carry += output;
        int whole = (int) carry;
        carry -= whole;
        int left = whole - energy.produce(whole);
        if (left > 0) {
            for (var member : members()) {
                if (left <= 0) break;
                if (member != this) left -= member.fill(left);
            }
        }
        push(level);
    }

    private void recompute(ServerLevel level) {
        var block = block();
        var config = block.kind().configs().get(block.tier() - 1);
        energy.configure(config.capacity(), config.maxOutput());
        double now = isOccupied() ? 0
                : block.kind().output(level, worldPosition, block.tier()) * GeneratorMath.waterlogged(getBlockState().getValue(GeneratorBlock.WATERLOGGED));
        if (now != output) {
            output = now;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            setChanged();
        }
    }

    /**
     * Whether something stands on the panel: a living entity (a player, a mob, an animal) whose feet are on its top shades it,
     * so it makes nothing while it is there.
     *
     * @return {@code true} if a living entity is on the panel
     */
    public boolean isOccupied() {
        if (level == null) return false;
        double top = worldPosition.getY() + GeneratorBlock.HEIGHT;
        var above = new AABB(worldPosition.getX(), top, worldPosition.getZ(), worldPosition.getX() + 1, top + 0.5, worldPosition.getZ() + 1);
        return !level.getEntitiesOfClass(LivingEntity.class, above, e -> !e.isSpectator()).isEmpty();
    }

    private void push(ServerLevel level) {
        GeneratorEnergy.push(neighbours, level, worldPosition, pool, Direction.UP);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putDouble("output", this.output);
        output.putDouble("carry", carry);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        output = input.getDoubleOr("output", 0);
        carry = input.getDoubleOr("carry", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The pool of the group, seen from this block: its limit per operation is the one of this block. */
    private final class Pool implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            long sum = 0;
            for (var member : members()) sum += member.energy.getAmountAsLong();
            return sum;
        }

        @Override
        public long getCapacityAsLong() {
            long sum = 0;
            for (var member : members()) sum += member.energy.getCapacityAsLong();
            return sum;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            int left = Math.min(amount, energy.maxOutput());
            int taken = 0;
            for (var member : members()) {
                if (left <= 0) break;
                int got = member.energy.extractUnlimited(left, transaction);
                left -= got;
                taken += got;
            }
            return taken;
        }
    }
}
