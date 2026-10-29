package net.samtrion.dotcraft.energy.kite;

import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
 * A kite wind generator of one tier: a winch with a spool. Right-click with a kite item puts the kite in, and a {@link KiteEntity}
 * flies up from the block on the tether; right-click with an empty hand shows the altitude and the output, and sneaking with an empty
 * hand reels the kite in (the item comes back). Breaking the block drops the kite as an item. This is deliberately <b>not</b> a
 * {@code GeneratorBlock}: it does not join the pool of the solar and lunar panels.
 */
public class KiteWindBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(3, 8, 3, 13, 13, 13),
            Block.box(7, 13, 7, 9, 15, 9));

    private final int tier;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
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
        if (!be.hasKite()) {
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.none"));
        } else if (player.isShiftKeyDown()) {
            if (be.takeKite()) {
                var kite = new ItemStack(KiteBlocks.KITE.get());
                if (!player.hasInfiniteMaterials() && !player.getInventory().add(kite)) player.drop(kite, false);
            }
        } else {
            double altitude = be.kite() == null ? 0 : be.kite().altitude();
            player.sendOverlayMessage(Component.translatable("message." + KiteBlocks.MODID + ".kite.status", String.format(Locale.ROOT, "%.0f", altitude),
                    KiteMath.tether(tier), String.format(Locale.ROOT, "%.1f", be.output())));
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
