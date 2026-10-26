package net.samtrion.dotcraft.resources.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Compressed ice: melts like vanilla ice (by light and when broken without Silk Touch), but only one tier down
 * instead of straight into water, and when broken it always leaves the lower tier, with or without ground below; tier 1 melts into vanilla ice, which melts into water on its own.
 */
public class CompressedIceBlock extends HalfTransparentBlock implements Compressed {
    private final Block base;
    private final int tier;
    private final Supplier<BlockState> meltsInto;

    /**
     * Creates a compressed ice block.
     *
     * @param properties the block properties
     * @param base       the compressed vanilla ice
     * @param tier       the tier, {@code 1..MAX}
     * @param meltsInto  what the block melts into: the next lower tier, or the vanilla ice for tier 1
     */
    public CompressedIceBlock(Properties properties, Block base, int tier, Supplier<BlockState> meltsInto) {
        super(properties);
        this.base = base;
        this.tier = tier;
        this.meltsInto = meltsInto;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public MutableComponent getName() {
        return CompressedBlock.name(base, tier);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack destroyedWith) {
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        if (!EnchantmentHelper.hasTag(destroyedWith, EnchantmentTags.PREVENTS_ICE_MELTING)) {
            if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
                level.removeBlock(pos, false);
                return;
            }
            level.setBlockAndUpdate(pos, meltsInto.get());
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11 - state.getLightDampening()) {
            if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
                level.removeBlock(pos, false);
            } else {
                BlockState melted = meltsInto.get();
                level.setBlockAndUpdate(pos, melted);
                level.neighborChanged(pos, melted.getBlock(), null);
            }
        }
    }
}
