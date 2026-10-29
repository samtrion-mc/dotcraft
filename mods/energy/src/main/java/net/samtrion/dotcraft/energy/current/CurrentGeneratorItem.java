package net.samtrion.dotcraft.energy.current;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The item of a current generator. It only adds the tooltip, since the pod has no screen: the formula of the output, the flow axis and the perk of
 * the tier.
 */
public class CurrentGeneratorItem extends BlockItem {
    /** Lang key of the one-line formula. */
    public static final String FORMULA_KEY = "tooltip." + EnergyMod.MODID + ".current.formula";
    /** Lang key of the line about the flow axis. */
    public static final String AXIS_KEY = "tooltip." + EnergyMod.MODID + ".current.axis";
    /** Prefix of the lang keys of the perks; the tier is appended. */
    public static final String PERK_KEY = "tooltip." + EnergyMod.MODID + ".current.perk_";

    private final int tier;

    /**
     * Creates the item.
     *
     * @param block      the block
     * @param properties the item properties
     */
    public CurrentGeneratorItem(CurrentGeneratorBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.tier();
    }

    /**
     * Adds the formula and the perk of the tier.
     *
     * @param stack   the stack
     * @param context the tooltip context
     * @param display the tooltip display
     * @param builder receives the lines
     * @param flag    the tooltip flag
     */
    @Override
    @SuppressWarnings("deprecation") // the vanilla items add their lines here as well
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(FORMULA_KEY).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(AXIS_KEY).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(PERK_KEY + tier).withStyle(ChatFormatting.BLUE));
    }
}
