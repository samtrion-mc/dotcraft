package net.samtrion.dotcraft.energy.kite;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
 * The block entity of a kite wind generator. It remembers whether a kite has been put in (this is what is saved, with the FE), owns
 * the {@link KiteEntity} (which is not saved, see there) and respawns it when its chunk ticks and none is there, reads the air every
 * {@link #RECOMPUTE_TICKS} ticks (staggered by position): the free altitude on the way up, the weather at the kite and the free
 * air around it. It sets the kite's target altitude from that and makes FE from the kite's <i>actual</i> altitude (see
 * {@link KiteMath}). The FE is buffered and pushed into the neighbours on every side but the top; the capability can only be drained.
 * Nothing is read from a chunk that is not loaded, so the block never loads chunks.
 */
public class KiteBlockEntity extends BlockEntity {
    /** Ticks between two reads of the world. */
    static final int RECOMPUTE_TICKS = 20;
    /** Radii in blocks at which the air around the kite is sampled (in the eight horizontal directions). */
    private static final int[] RADII = {3, 6};

    /** The buffer; it can only be drained from outside. */
    private final GeneratorEnergy energy;
    /** The energy handlers of the neighbours, one cache per side. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** Whether a kite has been put in (saved). */
    private boolean hasKite;
    /** The kite that flies now; not saved, spawned again when it is missing. */
    private @Nullable KiteEntity kite;
    /** The FE per tick made in the last tick. */
    private double output;
    /** The fraction of an FE that is not yet produced (saved), so a fractional output is not lost. */
    private double carry;
    /** The weather at the kite at the last read of the world. */
    private KiteMath.Weather weather = KiteMath.Weather.CLEAR;
    /** The free share of the air around the kite at the last read of the world. */
    private double openness = 1;
    /** Whether the dimension has a sky, at the last read of the world. */
    private boolean sky = true;
    /** What keeps the output from the peak, at the last read of the world. */
    private KiteMath.Limit limit = KiteMath.Limit.NONE;
    /** The redstone signal that the neighbours last saw. */
    private int signal;

    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state; its block must be a {@link KiteWindBlock}
     */
    public KiteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        var config = block().config();
        energy = new GeneratorEnergy(config.capacity(), config.maxOutput(), this::setChanged);
    }

    /** The block of this block entity. */
    private KiteWindBlock block() {
        return (KiteWindBlock) getBlockState().getBlock();
    }

    /**
     * The buffer, for the capability: nothing can be inserted, a drain is limited by the config of the tier.
     *
     * @return the buffer
     */
    public EnergyHandler energy() {
        return energy;
    }

    /**
     * Whether a kite has been put in; it flies as soon as the chunk ticks.
     *
     * @return {@code true} if the generator has a kite
     */
    public boolean hasKite() {
        return hasKite;
    }

    /**
     * The kite that flies now.
     *
     * @return the entity, {@code null} if there is none (no kite, or it is not spawned yet)
     */
    public @Nullable KiteEntity kite() {
        return kite;
    }

    /**
     * The FE per tick made in the last tick.
     *
     * @return the output
     */
    public double output() {
        return output;
    }

    /**
     * What keeps the kite from its peak, for the status message.
     *
     * @return the limit at the last read of the world
     */
    KiteMath.Limit limit() {
        return limit;
    }

    /**
     * The redstone signal for a comparator.
     *
     * @return 0 for no output, else 1..15 by the share of the peak
     */
    int signal() {
        return signal;
    }

    /**
     * Puts a kite in, which flies up from the block.
     *
     * @param level the level
     * @return {@code false} if there is a kite already
     */
    boolean insertKite(ServerLevel level) {
        if (hasKite) return false;
        hasKite = true;
        spawnKite(level);
        setChanged();
        return true;
    }

    /**
     * Takes the kite out: it is removed from the world.
     *
     * @return {@code true} if there was a kite (the caller hands the item back)
     */
    boolean takeKite() {
        if (!hasKite) return false;
        hasKite = false;
        output = 0;
        discardKite();
        setChanged();
        return true;
    }

    /** Spawns a kite at the block; it starts on the ground. */
    private void spawnKite(ServerLevel level) {
        var entity = new KiteEntity(KiteBlocks.KITE_ENTITY.get(), level);
        entity.attach(worldPosition, block().tier());
        kite = entity;
        if (!level.addFreshEntity(entity)) kite = null;
    }

    /** Removes the kite entity, if there is one. */
    private void discardKite() {
        if (kite != null) kite.discard();
        kite = null;
    }

    /** Gives the kite back as an item when the block is removed, removes the entity and hands the buffered FE to the neighbours. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && hasKite) {
            hasKite = false;
            discardKite();
            Block.popResource(server, pos, new ItemStack(KiteBlocks.KITE.get()));
        }
        if (level instanceof ServerLevel server) push(server);
    }

    /**
     * One server tick: reads the world now and then, makes FE and pushes it.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        long time = level.getGameTime() + worldPosition.hashCode();
        if (Math.floorMod(time, RECOMPUTE_TICKS) == 0) recompute(level);
        var config = block().config();
        output = hasKite && kite != null
                ? KiteMath.output(config.peak(), block().tier(), kite.altitude(), openness, weather, sky, KiteMath.wind(time)) : 0;
        updateSignal(level, config.peak());
        carry += output;
        int whole = (int) carry;
        carry -= whole;
        energy.produce(whole);
        push(level);
    }

    /** Tells the neighbours (comparators) when the signal changes. */
    private void updateSignal(ServerLevel level, double peak) {
        int now = KiteMath.signal(output, peak);
        if (now == signal) return;
        signal = now;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /**
     * Reads the world: the weather and the openness at the kite, the free way up. It sets the target of the kite and remembers what
     * the output of every tick is made of. A kite is only spawned where entities tick, so it is never a kite that stands still.
     */
    private void recompute(ServerLevel level) {
        var block = block();
        var config = block.config();
        energy.configure(config.capacity(), config.maxOutput());
        if (hasKite && (kite == null || kite.isRemoved()) && level.isPositionEntityTicking(worldPosition)) spawnKite(level);
        if (!hasKite || kite == null) {
            limit = KiteMath.Limit.NONE;
            return;
        }
        int tier = block.tier();
        sky = level.dimensionType().hasSkyLight();
        var cell = BlockPos.containing(kite.position());
        weather = weather(level, cell);
        openness = openness(level, cell);
        int reach = KiteMath.reach(tier, h -> blockedAround(level, tier, h));
        boolean reeledIn = !sky || KiteMath.weatherFactor(tier, weather) == 0;
        kite.setTarget(reeledIn ? 0 : reach);
        limit = KiteMath.limit(tier, sky, weather, reach, openness);
    }

    /**
     * The weather at a position. Rain and snow both count, and the temperature of the biome (which falls with the height) does not
     * matter, so the kite in a snowy biome or high up is as wet as the ground (see {@link KiteMath#weather}).
     */
    private static KiteMath.Weather weather(Level level, BlockPos pos) {
        boolean exposed = level.canSeeSky(pos) && level.getBiome(pos).value().hasPrecipitation();
        return KiteMath.weather(level.isRaining(), level.isThundering(), exposed);
    }

    /** Whether a block is in the way {@code h} blocks above the top of the generator: the cell, and for a wide line its neighbours. */
    private boolean blockedAround(Level level, int tier, int h) {
        var cell = worldPosition.above(h);
        if (blocked(level, cell)) return true;
        return KiteMath.wide(tier) && (blocked(level, cell.north()) || blocked(level, cell.south()) || blocked(level, cell.east())
                || blocked(level, cell.west()));
    }

    /** The free share of the air around a cell: the cells at {@link #RADII} in the eight horizontal directions. */
    private static double openness(Level level, BlockPos center) {
        int free = 0;
        int total = 0;
        for (int radius : RADII) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    total++;
                    if (!blocked(level, center.offset(dx * radius, 0, dz * radius))) free++;
                }
            }
        }
        return KiteMath.openness(free, total);
    }

    /** A block that stops a kite; a position in a chunk that is not loaded counts as free, it is never loaded for this. */
    private static boolean blocked(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).blocksMotion();
    }

    /** Pushes the buffer into the neighbours on every side but the top. */
    private void push(ServerLevel level) {
        if (energy.getAmountAsLong() <= 0) return;
        for (Direction d : Direction.values()) {
            if (d == Direction.UP) continue;
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
        output.putBoolean("kite", hasKite);
        output.putDouble("carry", carry);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        hasKite = input.getBooleanOr("kite", false);
        carry = input.getDoubleOr("carry", 0);
    }
}
