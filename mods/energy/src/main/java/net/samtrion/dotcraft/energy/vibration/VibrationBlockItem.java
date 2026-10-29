package net.samtrion.dotcraft.energy.vibration;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The item of a vibration generator; its tooltip says what the block needs and what its tier hears, as the block has no screen.
 */
public class VibrationBlockItem extends BlockItem {
    /** Lang key of the line with the peak and the average ceiling in FE/t. */
    public static final String PEAK_KEY = "tooltip." + EnergyMod.MODID + ".vibration.peak";
    /** Lang key of the line that names the radius and the lowest frequency heard. */
    public static final String HEARING_KEY = "tooltip." + EnergyMod.MODID + ".vibration.hearing";
    /** Lang key of the line that says the tier hears through wool. */
    public static final String PIERCE_KEY = "tooltip." + EnergyMod.MODID + ".vibration.pierce";
    /** Lang key of the line that says what makes FE. */
    public static final String NEEDS_KEY = "tooltip." + EnergyMod.MODID + ".vibration.needs";

    private final int tier;

    /**
     * Creates the item.
     *
     * @param block      the block
     * @param properties the item properties
     */
    public VibrationBlockItem(VibrationBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.tier();
    }

    @Override
    @SuppressWarnings("deprecation") // the vanilla items add their lines here as well
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
        builder.accept(Component.translatable(NEEDS_KEY).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(PEAK_KEY, peak, VibrationMath.refill(peak)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable(HEARING_KEY, VibrationMath.radius(tier), VibrationMath.minFrequency(tier)).withStyle(ChatFormatting.GRAY));
        if (VibrationMath.pierces(tier)) builder.accept(Component.translatable(PIERCE_KEY).withStyle(ChatFormatting.GRAY));
    }
}
