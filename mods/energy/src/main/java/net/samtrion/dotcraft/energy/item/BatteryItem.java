package net.samtrion.dotcraft.energy.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/**
 * A battery that stores FE in the {@link EnergyItems#ENERGY} component of its stack, see {@link BatteryEnergyHandler}.
 * It shows the charge as the item bar and as a tooltip line. A stack without the component is full.
 */
public final class BatteryItem extends Item {
    private final EnergyCommonConfig.Battery config;

    /**
     * Creates the item.
     *
     * @param properties the properties
     * @param config     the config of the battery, read for the capacity whenever it is needed
     */
    public BatteryItem(Properties properties, EnergyCommonConfig.Battery config) {
        super(properties);
        this.config = config;
    }

    /**
     * The capacity of the battery.
     *
     * @return the FE the battery stores
     */
    private int capacity() {
        return config.capacity();
    }

    /**
     * The stored FE of a battery stack.
     *
     * @param stack the stack
     * @return the stored FE, the capacity where the stack has no component
     */
    private int energy(ItemStack stack) {
        return Math.min(capacity(), stack.getOrDefault(EnergyItems.ENERGY.get(), capacity()));
    }

    /**
     * The bar is shown while the battery is not full.
     *
     * @param stack the stack
     * @return whether the stored FE is below the capacity
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return energy(stack) < capacity();
    }

    /**
     * The bar fills with the charge.
     *
     * @param stack the stack
     * @return the width in pixels, see {@link BatteryBar#width}
     */
    @Override
    public int getBarWidth(ItemStack stack) {
        return BatteryBar.width(energy(stack), capacity());
    }

    /**
     * The bar is red when nearly empty and turns green with the charge.
     *
     * @param stack the stack
     * @return the bar color
     */
    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) energy(stack) / capacity() / 3.0F, 1.0F, 1.0F);
    }

    /**
     * Adds the {@code x / y FE} line.
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
        builder.accept(Component.translatable(EnergyItems.TOOLTIP_KEY, energy(stack), capacity())
                .withStyle(ChatFormatting.GRAY));
    }
}
