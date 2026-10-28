package net.samtrion.dotcraft.resources.client;

import java.text.NumberFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.Compressed;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/**
 * Adds the total number of base blocks to the tooltip of compressed blocks, everywhere a tooltip is shown, including
 * recipe viewers.
 */
@EventBusSubscriber(modid = CraftResources.MODID, value = Dist.CLIENT)
public final class CompressedTooltips {
    /** Translation key of the tooltip line, argument: number of base blocks. */
    public static final String COUNT_KEY = "tooltip." + CraftResources.MODID + ".compressed_count";

    private CompressedTooltips() {}

    /**
     * Appends the base block count to the tooltip of a compressed block item.
     *
     * @param event the tooltip event
     */
    @SubscribeEvent
    static void addCount(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof BlockItem item && item.getBlock() instanceof Compressed block) {
            String count = NumberFormat.getIntegerInstance().format(CompressedTier.count(block.tier()));
            event.getToolTip().add(Component.translatable(COUNT_KEY, count).withStyle(ChatFormatting.GRAY));
        }
    }
}
