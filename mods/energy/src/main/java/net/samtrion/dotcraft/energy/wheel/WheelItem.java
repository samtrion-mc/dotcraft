package net.samtrion.dotcraft.energy.wheel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The item of a mob wheel. It explains the rules in its tooltip, as the wheel has no screen: the peak, the zone, the cap, the
 * perk of the tier and what the comparator reads.
 */
public class WheelItem extends BlockItem {
    /** The tooltip key of the stats line: peak FE/t, radius and cap are its arguments. */
    public static final String STATS_KEY = "tooltip." + EnergyMod.MODID + ".wheel.stats";
    /** The tooltip key prefix of the perk lines; the tier is appended. */
    public static final String PERK_KEY = "tooltip." + EnergyMod.MODID + ".wheel.perk.";
    /** The tooltip key of the line that explains the rules and the comparator. */
    public static final String RULES_KEY = "tooltip." + EnergyMod.MODID + ".wheel.rules";

    private final WheelBlock wheel;

    /**
     * Creates the item.
     *
     * @param block      the wheel
     * @param properties the item properties
     */
    public WheelItem(WheelBlock block, Properties properties) {
        super(block, properties);
        this.wheel = block;
    }

    /**
     * Adds the stats, the perk and the rules.
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
        int tier = wheel.tier();
        builder.accept(Component.translatable(STATS_KEY, EnergyCommonConfig.MOB_WHEELS.get(tier - 1).peak(), WheelMath.radius(tier), WheelMath.cap(tier))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(PERK_KEY + tier).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(RULES_KEY).withStyle(ChatFormatting.DARK_GRAY));
    }
}
