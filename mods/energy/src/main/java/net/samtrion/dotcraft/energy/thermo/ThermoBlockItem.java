package net.samtrion.dotcraft.energy.thermo;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The item of a thermoelectric generator; its tooltip tells the peak and the temperature difference of the tier, as the block has no GUI. */
public class ThermoBlockItem extends BlockItem {
    /** The translation key of the peak line, with the peak in FE/t and the difference for the peak. */
    public static final String TOOLTIP_KEY = "tooltip." + EnergyMod.MODID + ".thermo";
    /** The translation key of the line that says what the block needs. */
    public static final String HINT_KEY = "tooltip." + EnergyMod.MODID + ".thermo.hint";

    /** The tier, 1..5. */
    private final int tier;

    /**
     * Creates the item.
     *
     * @param block      the generator block
     * @param tier       its tier, 1..5
     * @param properties the item properties
     */
    public ThermoBlockItem(ThermoBlock block, int tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    /**
     * Adds the peak, the difference that gives it and what the block needs.
     *
     * @param stack   the stack
     * @param context the tooltip context
     * @param display the tooltip display
     * @param builder receives the lines
     * @param flag    the tooltip flag
     */
    @Override
    @SuppressWarnings("deprecation") // the vanilla items add their lines here as well
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(TOOLTIP_KEY, EnergyCommonConfig.THERMO_GENERATORS.get(tier - 1).peak(), ThermoMath.ceiling(tier))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(HINT_KEY).withStyle(ChatFormatting.DARK_GRAY));
    }
}
