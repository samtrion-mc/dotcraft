package net.samtrion.dotcraft.energy.melt;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The item of a melt generator. From tier 4 on its tooltip tells about the water tank and what a full tank costs. */
public class MeltGeneratorItem extends BlockItem {
    /** Lang key of the tank line; the argument is the capacity in buckets. */
    public static final String TANK_KEY = "tooltip." + EnergyMod.MODID + ".melt.tank";
    /** Lang key of the line about the full tank; the argument is the share of the output that is left, in percent. */
    public static final String FULL_KEY = "tooltip." + EnergyMod.MODID + ".melt.tank_full";

    private final int tier;

    /**
     * Creates the item.
     *
     * @param block      the generator block
     * @param properties the item properties
     * @param tier       the tier, 1..5
     */
    public MeltGeneratorItem(Block block, Item.Properties properties, int tier) {
        super(block, properties);
        this.tier = tier;
    }

    /**
     * Adds the lines about the water tank (tiers with water recovery only).
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
        if (!MeltMath.tier(tier).water()) return;
        builder.accept(Component.translatable(TANK_KEY, MeltMath.TANK_CAPACITY_MB / MeltMath.BUCKET_MB).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(FULL_KEY, Math.round(MeltMath.FULL_TANK_EFFICIENCY * 100)).withStyle(ChatFormatting.GRAY));
    }
}
