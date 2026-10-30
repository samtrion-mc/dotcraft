package net.samtrion.dotcraft.energy.kite;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.generator.GeneratorEnergy;

/**
 * The block entity of a kite wind generator. It remembers whether a kite has been put in (this is what is saved, with the FE), owns
 * the {@link KiteEntity kites} (one, or two from tier 4; not saved, see there) and respawns a missing one when its chunk ticks, reads the air every
 * {@link #RECOMPUTE_TICKS} ticks (staggered by position): the free altitude on the way up, the weather at the kite and the free
 * air around it. It sets the kite's target altitude from that and makes FE from the kites' <i>actual</i> altitudes (see
 * {@link KiteMath}; the kites of a pair share the peak). The FE is buffered and pushed into the neighbours on every side but the top; the capability can only be drained.
 * Nothing is read from a chunk that is not loaded, so the block never loads chunks.
 * <p>
 * <b>Wire.</b> The tether comes from the player: {@link #insertWire(int)} stores 1 wire = 1 block of tether (at most the tether of the
 * tier, saved, dropped when the block breaks). Without wire no kite is launched. <b>Distance.</b> Generators with a kite and wire are
 * kept in a small per-level registry of the loaded ones; every {@link #RECOMPUTE_TICKS} ticks (and on insert) a generator scans that
 * set once for a neighbour closer than {@link KiteMath#MIN_DISTANCE}; both are then blocked (kites discarded, output 0).
 * <b>Lightning.</b> Besides the rolled visual bolt a natural bolt within {@link KiteMath#THUNDER_RADIUS} of a kite or its tether is
 * credited once, see {@link #onBolt}. {@link #diagnostics()} is the read-only view for the overlay and for other mods.
 */
public class KiteBlockEntity extends BlockEntity {
    /** Ticks between two reads of the world. */
    static final int RECOMPUTE_TICKS = 20;
    /** Radii in blocks at which the air around the kite is sampled (in the eight horizontal directions). */
    private static final int[] RADII = {3, 6};

    /** The loaded generators that have a kite and wire (a tether), by level; only the server thread touches it. */
    private static final Map<ResourceKey<Level>, Set<KiteBlockEntity>> ACTIVE = new HashMap<>();
    /** The id of the wire item: iron wire of the resources mod, the tag {@code c:wires/iron} accepts it. */
    private static final Identifier WIRE_ITEM = Identifier.fromNamespaceAndPath("dotcraftresources", "iron_wire");

    /** The entity tag of the visual bolts a generator spawns itself, so the bolt event does not credit them a second time. */
    static final String OWN_BOLT = "dotcraftenergy_kite_bolt";

    /** What a generator is doing, the first reason that applies; read by the overlay and by other mods. */
    public enum Status {
        /** No kite has been put in. */
        NO_KITE,
        /** No wire: the tether is not there, no kite is launched. */
        NO_WIRE,
        /** Another kite generator is closer than the minimum distance: both are blocked. */
        TOO_CLOSE,
        /** The dimension has no sky: the kite is reeled in. */
        NO_SKY,
        /** A thunderstorm tears the tether of the tier: the kite is reeled in. */
        WEATHER_REELED,
        /** The kites fly (maybe limited, see {@link Diagnostics#limit()}). */
        FLYING
    }

    /**
     * A read-only snapshot of a generator, for the overlay and for Jade.
     *
     * @param status          what the generator is doing
     * @param wire            the wire stored, = blocks of tether
     * @param wireCapacity    the most wire it takes (the tether of the tier)
     * @param effectiveLength the altitude of the first kite in blocks, 0 if none flies
     * @param lengthFactor    the yield factor of that length, 0..1
     * @param output          the FE per tick now
     * @param minDistance     the minimum horizontal distance to other kite generators in blocks
     * @param nearestOffender the horizontal distance to the nearest generator that is too close, -1 if none
     * @param kites           how many kites fly now
     * @param limit           what keeps the output from its peak: {@code none}, {@code no_sky}, {@code storm}, {@code blocked},
     *                        {@code weather} or {@code crowded}
     */
    public record Diagnostics(Status status, int wire, int wireCapacity, double effectiveLength, double lengthFactor, double output,
            int minDistance, double nearestOffender, int kites, String limit) {}

    /** The buffer; it can only be drained from outside. */
    private final GeneratorEnergy energy;
    /** The energy handlers of the neighbours, one cache per side. */
    private final Map<Direction, BlockCapabilityCache<EnergyHandler, @Nullable Direction>> neighbours = new EnumMap<>(Direction.class);
    /** Whether a kite has been put in (saved). */
    private boolean hasKite;
    /** The kites that fly now, one per slot (see {@link KiteMath#kites(int)}); not saved, a missing one is spawned again. */
    private final @Nullable KiteEntity[] kites = new KiteEntity[KiteMath.MAX_KITES];
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
    /** The game time until which a kite of a slot is safe from lightning. */
    private final long[] strikeCooldownUntil = new long[KiteMath.MAX_KITES];
    /** The redstone signal that the neighbours last saw. */
    private int signal;
    /** The wire stored (saved), 1 per block of tether. */
    private int wire;
    /** The horizontal distance to the nearest other generator that is too close, -1 if none. */
    private double nearestOffender = -1;

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
     * The first kite that flies now (the one that makes most of the output).
     *
     * @return the entity, {@code null} if there is none (no kite, or it is not spawned yet)
     */
    public @Nullable KiteEntity kite() {
        return kites[0];
    }

    /**
     * The kite of a slot.
     *
     * @param slot 0 for the first kite, 1 for the second kite of tier 4 and 5
     * @return the entity, {@code null} if the slot is empty (no kite, not spawned yet, or the tier has no such slot)
     */
    public @Nullable KiteEntity kite(int slot) {
        return slot >= 0 && slot < kites.length ? kites[slot] : null;
    }

    /**
     * Whether an entity is one of the kites this block has; a kite that is not discards itself, so there is never a duplicate.
     *
     * @param entity the entity
     * @return {@code true} if it is in a slot
     */
    boolean owns(KiteEntity entity) {
        for (var k : kites) {
            if (k == entity) return true;
        }
        return false;
    }

    /**
     * The wire stored.
     *
     * @return 0..{@link #wireCapacity()}
     */
    public int wire() {
        return wire;
    }

    /**
     * The most wire the generator takes: the tether of its tier.
     *
     * @return the capacity in blocks
     */
    public int wireCapacity() {
        return KiteMath.tether(block().tier());
    }

    /**
     * Stores wire: 1 wire is 1 block of tether, up to the tether of the tier.
     *
     * @param amount the wire offered
     * @return how much was taken
     */
    public int insertWire(int amount) {
        int now = KiteMath.wireStored(block().tier(), wire, amount);
        int taken = now - wire;
        if (taken > 0) {
            wire = now;
            setChanged();
        }
        return taken;
    }

    /**
     * Whether the generator is blocked because another one is too close (as of the last read).
     *
     * @return {@code true} if both are blocked
     */
    public boolean tooClose() {
        return nearestOffender >= 0;
    }

    /**
     * The read-only state of the generator.
     *
     * @return what it does, its wire, its length and output, its distances and its kites
     */
    public Diagnostics diagnostics() {
        int tier = block().tier();
        Status status;
        if (!hasKite) status = Status.NO_KITE;
        else if (wire <= 0) status = Status.NO_WIRE;
        else if (tooClose()) status = Status.TOO_CLOSE;
        else if (!sky) status = Status.NO_SKY;
        else if (KiteMath.weatherFactor(tier, weather) == 0) status = Status.WEATHER_REELED;
        else status = Status.FLYING;
        int flying = 0;
        for (var k : kites) {
            if (k != null && !k.isRemoved()) flying++;
        }
        double length = kites[0] == null || kites[0].isRemoved() ? 0 : kites[0].altitude();
        return new Diagnostics(status, wire, wireCapacity(), length, KiteMath.lengthFactor(tier, length), output, KiteMath.MIN_DISTANCE,
                nearestOffender, flying, limit.name().toLowerCase(Locale.ROOT));
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
        if (canFly()) spawnMissing(level);
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

    /** Whether the kites may fly: a kite, wire and no generator that is too close (the distance is read now). */
    private boolean canFly() {
        scanNeighbours();
        return hasKite && wire > 0 && !tooClose();
    }

    /**
     * Looks for the nearest other generator (with a kite and wire) that tangles with this one. One pass over the small registry of
     * the loaded ones; a generator without a tether is not counted, so the rule is symmetric.
     * ponytail: linear scan per generator and second, fine for dozens of loaded generators; use a spatial hash for thousands.
     */
    private void scanNeighbours() {
        nearestOffender = -1;
        if (level == null || !hasKite || wire <= 0) return;
        int tier = block().tier();
        for (var other : ACTIVE.getOrDefault(level.dimension(), Set.of())) {
            if (other == this || other.isRemoved() || !other.hasKite || other.wire <= 0) continue;
            var o = other.worldPosition;
            int dx = o.getX() - worldPosition.getX();
            int dz = o.getZ() - worldPosition.getZ();
            if (!KiteMath.tooClose(dx, o.getY() - worldPosition.getY(), dz, tier, other.block().tier())) continue;
            double d = Math.hypot(dx, dz);
            if (nearestOffender < 0 || d < nearestOffender) nearestOffender = d;
        }
    }

    /** Spawns a kite in every slot of the tier that has none (or whose kite is gone); a kite starts on the ground. */
    private void spawnMissing(ServerLevel level) {
        int tier = block().tier();
        for (int slot = 0; slot < KiteMath.kites(tier); slot++) {
            var old = kites[slot];
            if (old != null && !old.isRemoved()) continue;
            var entity = new KiteEntity(KiteBlocks.KITE_ENTITY.get(), level);
            entity.attach(worldPosition, tier, slot);
            kites[slot] = entity;
            if (!level.addFreshEntity(entity)) kites[slot] = null;
        }
    }

    /** Removes the kite entities, if there are any. */
    private void discardKite() {
        for (int slot = 0; slot < kites.length; slot++) {
            if (kites[slot] != null) kites[slot].discard();
            kites[slot] = null;
        }
    }

    /** Whether every kite of the tier flies (is spawned and not removed). */
    private boolean allFly() {
        for (int slot = 0; slot < KiteMath.kites(block().tier()); slot++) {
            if (kites[slot] == null || kites[slot].isRemoved()) return false;
        }
        return true;
    }

    /** The FE per tick of all kites of the tier together; the shares of the slots add up to the base (see {@link KiteMath#slotShare}). */
    private double totalOutput(double base, int tier, long time) {
        double sum = 0;
        for (int slot = 0; slot < KiteMath.kites(tier); slot++) {
            var k = kites[slot];
            if (k != null && !k.isRemoved())
                sum += KiteMath.slotOutput(base, tier, slot, k.altitude(), openness, weather, sky, KiteMath.wind(time));
        }
        return sum;
    }

    /** Joins the registry of the loaded generators (server only). */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) ACTIVE.computeIfAbsent(server.dimension(), k -> new HashSet<>()).add(this);
    }

    /** Leaves the registry, so a removed or unloaded generator frees the ones it blocked. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            var set = ACTIVE.get(level.dimension());
            if (set != null) set.remove(this);
        }
    }

    /**
     * The loaded generators of a level that have a kite and wire, as a copy (the game tests use it to clear leftovers that would block a test).
     *
     * @param level the level
     * @return the generators
     */
    static List<KiteBlockEntity> loaded(ServerLevel level) {
        return List.copyOf(ACTIVE.getOrDefault(level.dimension(), Set.of()));
    }

    /** Forgets all generators, when a server stops. */
    static void clearRegistry() {
        ACTIVE.clear();
    }

    /**
     * A lightning bolt that joined a level: every generator in range whose kite stays up in the storm credits it once. A bolt we
     * spawned ourselves carries {@link #OWN_BOLT} and is ignored here, it was credited when it was rolled.
     *
     * @param level the level
     * @param bolt  the bolt
     */
    static void onBolt(ServerLevel level, LightningBolt bolt) {
        if (bolt.entityTags().contains(OWN_BOLT)) return;
        for (var be : List.copyOf(ACTIVE.getOrDefault(level.dimension(), Set.of()))) be.attract(level, bolt);
    }

    /** Credits a natural bolt if it is within {@link KiteMath#THUNDER_RADIUS} of a flying kite or its tether; once per bolt, with the cooldown. */
    private void attract(ServerLevel level, LightningBolt bolt) {
        int tier = block().tier();
        if (!hasKite || weather != KiteMath.Weather.THUNDER || KiteMath.weatherFactor(tier, KiteMath.Weather.THUNDER) <= 0) return;
        long now = level.getGameTime();
        double[] a = {worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5};
        double[] b = {bolt.getX(), bolt.getY(), bolt.getZ()};
        for (int slot = 0; slot < KiteMath.kites(tier); slot++) {
            var k = kites[slot];
            if (k == null || k.isRemoved() || strikeCooldownUntil[slot] > now) continue;
            if (!KiteMath.attracts(b, a, new double[] {k.getX(), k.getY(), k.getZ()})) continue;
            strikeCooldownUntil[slot] = now + KiteMath.LIGHTNING_COOLDOWN_TICKS;
            creditLightning(level, EnergyCommonConfig.kiteLightningEnergy());
            return;
        }
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
        if (level instanceof ServerLevel server && wire > 0) {
            var item = BuiltInRegistries.ITEM.getValue(WIRE_ITEM);
            if (item != Items.AIR) Block.popResource(server, pos, new ItemStack(item, wire));
            wire = 0;
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
        int tier = block().tier();
        output = hasKite ? totalOutput(config.peak(), tier, time) : 0;
        updateSignal(level, KiteMath.peak(config.peak(), tier));
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
        if (!canFly()) {
            discardKite();
            limit = KiteMath.Limit.NONE;
            return;
        }
        if (!allFly() && level.isPositionEntityTicking(worldPosition)) spawnMissing(level);
        var first = kites[0];
        if (first == null) {
            limit = KiteMath.Limit.NONE;
            return;
        }
        int tier = block.tier();
        sky = level.dimensionType().hasSkyLight();
        var cell = BlockPos.containing(first.position());
        weather = weather(level, cell);
        openness = openness(level, cell);
        int reach = KiteMath.reach(tier, h -> blockedAround(level, tier, h));
        boolean reeledIn = !sky || KiteMath.weatherFactor(tier, weather) == 0;
        boolean storm = weather == KiteMath.Weather.THUNDER;
        for (var k : kites) {
            if (k == null) continue;
            k.setTarget(reeledIn ? 0 : KiteMath.flightTarget(tier, wire, reach));
            k.setStorm(storm);
        }
        limit = KiteMath.limit(tier, sky, weather, reach, openness);
        rollLightning(level, tier);
    }

    /** One roll per second and kite: a kite that stays up in a thunderstorm may be struck (see {@link KiteMath#struck}). */
    private void rollLightning(ServerLevel level, int tier) {
        if (!sky || weather != KiteMath.Weather.THUNDER) return;
        long now = level.getGameTime();
        for (int slot = 0; slot < KiteMath.kites(tier); slot++) {
            var k = kites[slot];
            if (k == null || k.isRemoved()) continue;
            if (KiteMath.struck(tier, weather, strikeCooldownUntil[slot] - now, level.getRandom().nextDouble())) strike(level, slot);
        }
    }

    /**
     * Lets lightning strike the kite of a slot: a real but purely visual bolt (no fire, no damage) at the kite, which then credits the
     * energy to this block; the kite is safe from the next bolt for a while.
     *
     * @param level the level
     * @param slot  the slot of the kite
     * @return the share of the bolt, or {@code null} if the slot has no kite
     */
    KiteMath.@Nullable Credit strike(ServerLevel level, int slot) {
        var k = kite(slot);
        if (k == null || k.isRemoved()) return null;
        strikeCooldownUntil[slot] = level.getGameTime() + KiteMath.LIGHTNING_COOLDOWN_TICKS;
        var bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(k.getX(), k.getY(), k.getZ());
            bolt.setVisualOnly(true);
            bolt.addTag(OWN_BOLT);
            level.addFreshEntity(bolt);
        }
        return creditLightning(level, EnergyCommonConfig.kiteLightningEnergy());
    }

    /**
     * Credits the FE of a bolt: into the buffer first, the rest as a one-off burst into the neighbours (which ignores the push limit
     * per tick); what neither takes is lost.
     *
     * @param level  the level
     * @param energy the FE of the bolt
     * @return how the FE was shared out
     */
    KiteMath.Credit creditLightning(ServerLevel level, int amount) {
        int free = (int) Math.min(Integer.MAX_VALUE, energy.getCapacityAsLong() - energy.getAmountAsLong());
        int stored = energy.produce(Math.clamp(free, 0, amount));
        int rest = amount - stored;
        int pushed = 0;
        for (Direction d : Direction.values()) {
            if (rest <= pushed || d == Direction.UP) continue;
            var cache = neighbours.computeIfAbsent(d,
                    dir -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(dir), dir.getOpposite()));
            var target = cache.getCapability();
            if (target == null) continue;
            try (var tx = Transaction.openRoot()) {
                pushed += target.insert(rest - pushed, tx);
                tx.commit();
            }
        }
        return KiteMath.credit(amount, free, pushed);
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
        output.putInt("wire", wire);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        hasKite = input.getBooleanOr("kite", false);
        carry = input.getDoubleOr("carry", 0);
        wire = Math.clamp(input.getIntOr("wire", 0), 0, wireCapacity());
    }
}
