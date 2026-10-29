package net.samtrion.dotcraft.energy.kite;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** The item of a kite wind generator. Its tooltip names the tether and the perk of the tier (the rules of {@link KiteMath}). */
public class KiteGeneratorItem extends BlockItem {
    /** The tier, 1..5. */
    private final int tier;

    /**
     * Creates the item.
     *
     * @param block      the generator block
     * @param properties the item properties
     * @param tier       the tier, 1..5
     */
    public KiteGeneratorItem(Block block, Item.Properties properties, int tier) {
        super(block, properties);
        this.tier = tier;
    }

    /**
     * Adds the tether length and the perk of the tier.
     *
     * @param stack   the stack
     * @param context the tooltip context
     * @param display the tooltip display
     * @param builder takes the lines
     * @param flag    the tooltip flag
     */
    @Override
    @SuppressWarnings("deprecation") // the vanilla items add their lines here as well
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("tooltip." + KiteBlocks.MODID + ".kite.tether", KiteMath.tether(tier)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip." + KiteBlocks.MODID + ".kite.perk." + tier).withStyle(ChatFormatting.GRAY));
    }
}
