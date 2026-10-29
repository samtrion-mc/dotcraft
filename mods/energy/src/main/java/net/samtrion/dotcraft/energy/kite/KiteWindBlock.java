package net.samtrion.dotcraft.energy.kite;

import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * A kite wind generator of one tier: a winch with a spool. Right-click with a kite item puts the kite in, right-click with iron wire
 * stores it as tether (1 wire = 1 block, up to the tether of the tier; shift is not needed), and a {@link KiteEntity} flies up from the
 * block on that tether; right-click with an empty hand shows the diagnostics (see {@link KiteBlockEntity#diagnostics()}), and sneaking
 * with an empty hand reels the kite in (the item comes back). Breaking the block drops the kite and the wire as items. This is deliberately <b>not</b> a
 * {@code GeneratorBlock}: it does not join the pool of the solar and lunar panels.
 */
public class KiteWindBlock extends Block implements EntityBlock {
    /** The shape: the base, the spool and the mast. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(3, 8, 3, 13, 13, 13),
            Block.box(7, 13, 7, 9, 15, 9));

    /** The tier, 1..5. */
    private final int tier;
    /** The block entity type, resolved late as it is registered after the blocks. */
    private final Supplier<? extends BlockEntityType<KiteBlockEntity>> type;

    /**
     * Creates the block.
     *
     * @param properties the block properties
     * @param tier       the tier, 1..5
     * @param type       the block entity type
     */
    public KiteWindBlock(Properties properties, int tier, Supplier<? extends BlockEntityType<KiteBlockEntity>> type) {
        super(properties);
        this.tier = tier;
        this.type = type;
    }

    /**
     * The tier.
     *
     * @return 1..5
     */
    public int tier() {
        return tier;
    }

    /**
     * The config of the tier.
     *
     * @return the peak, the buffer and the push rate
     */
    public EnergyCommonConfig.Generator config() {
        return EnergyCommonConfig.KITE_GENERATORS.get(tier - 1);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** The comparator signal: 0 for no output, else 1..15 by the share of the peak. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof KiteBlockEntity be ? be.signal() : 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (stack.is(KiteBlocks.IRON_WIRE)) {
            if (!(level.getBlockEntity(pos) instanceof KiteBlockEntity be) || be.wire() >= be.wireCapacity()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int taken = be.insertWire(stack.getCount());
            if (!player.hasInfiniteMaterials()) stack.shrink(taken);
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.wire", be.wire(), be.wireCapacity()));
            return InteractionResult.SUCCESS;
        }
        if (!stack.is(KiteBlocks.KITE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof KiteBlockEntity be && be.insertKite((ServerLevel) level)) {
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof KiteBlockEntity be)) return InteractionResult.PASS;
        var d = be.diagnostics();
        if (!be.hasKite()) {
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.none"));
        } else if (player.isShiftKeyDown()) {
            if (be.takeKite()) {
                var kite = new ItemStack(KiteBlocks.KITE.get());
                if (!player.hasInfiniteMaterials() && !player.getInventory().add(kite)) player.drop(kite, false);
            }
        } else if (d.status() == KiteBlockEntity.Status.NO_WIRE || d.status() == KiteBlockEntity.Status.TOO_CLOSE) {
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.state." + d.status().name().toLowerCase(Locale.ROOT),
                    d.wire(), d.wireCapacity(), d.minDistance(), String.format(Locale.ROOT, "%.1f", d.nearestOffender())));
        } else {
            var limit = Component.translatable("message." + KiteBlocks.MODID + ".kite.limit." + d.limit());
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.status", String.format(Locale.ROOT, "%.0f", d.effectiveLength()),
                    d.wire(), String.format(Locale.ROOT, "%.1f", d.output()), limit, d.wireCapacity()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KiteBlockEntity(type.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by the type comparison
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
        if (level.isClientSide() || given != type.get()) return null;
        return (l, p, s, be) -> ((KiteBlockEntity) be).serverTick((ServerLevel) l);
    }
}
