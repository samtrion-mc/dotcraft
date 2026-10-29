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

    private final GeneratorEnergy energy;
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    private boolean hasKite;
    private @Nullable KiteEntity kite;
    private double output;
    private double carry;

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
     * The FE per tick made at the last read of the world.
     *
     * @return the output
     */
    public double output() {
        return output;
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

    private void spawnKite(ServerLevel level) {
        var entity = new KiteEntity(KiteBlocks.KITE_ENTITY.get(), level);
        entity.attach(worldPosition, block().tier());
        kite = entity;
        if (!level.addFreshEntity(entity)) kite = null;
    }

    private void discardKite() {
        if (kite != null) kite.discard();
        kite = null;
    }

    /** Gives the kite back as an item when the block is removed, and removes the entity. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && hasKite) {
            hasKite = false;
            discardKite();
            Block.popResource(server, pos, new ItemStack(KiteBlocks.KITE.get()));
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
        energy.produce(whole);
        push(level);
    }

    private void recompute(ServerLevel level) {
        var block = block();
        var config = block.config();
        energy.configure(config.capacity(), config.maxOutput());
        if (hasKite && (kite == null || kite.isRemoved())) spawnKite(level);
        double now = 0;
        if (hasKite && kite != null) {
            int tier = block.tier();
            boolean sky = level.dimensionType().hasSkyLight();
            var cell = BlockPos.containing(kite.position());
            var weather = weather(level, cell);
            boolean reeledIn = !sky || KiteMath.weatherFactor(tier, weather) == 0;
            kite.setTarget(reeledIn ? 0 : KiteMath.reach(tier, h -> blockedAround(level, h)));
            now = KiteMath.output(config.peak(), tier, kite.altitude(), openness(level, cell), weather, sky);
        }
        if (now != output) {
            output = now;
            setChanged();
        }
    }

    /** The weather at a position: a thunderstorm only where it rains at all, so a dry biome stays clear. */
    private static KiteMath.Weather weather(Level level, BlockPos pos) {
        if (!level.isRainingAt(pos)) return KiteMath.Weather.CLEAR;
        return level.isThundering() ? KiteMath.Weather.THUNDER : KiteMath.Weather.RAIN;
    }

    /** Whether a block is in the way {@code h} blocks above the top of the generator, in the cell or next to it. */
    private boolean blockedAround(Level level, int h) {
        var cell = worldPosition.above(h);
        return blocked(level, cell) || blocked(level, cell.north()) || blocked(level, cell.south()) || blocked(level, cell.east())
                || blocked(level, cell.west());
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
