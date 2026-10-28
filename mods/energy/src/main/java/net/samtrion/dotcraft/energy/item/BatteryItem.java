package net.samtrion.dotcraft.energy.item;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
     * A battery in the inventory of a player charges the item in their main hand, or else in their off hand, as far as that
     * item takes FE and needs it; that includes a rechargeable battery, but not a single-use one, which takes none.
     *
     * @param stack the battery stack
     * @param level the level
     * @param owner the holder
     * @param slot  the equipment slot of the stack, if any
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player) || energy(stack) == 0) return;
        var inventory = player.getInventory();
        int source = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                source = i;
                break;
            }
        }
        if (source < 0) return;
        EnergyHandler from = ItemAccess.forPlayerSlot(player, source).getCapability(Capabilities.Energy.ITEM);
        if (from == null) return;
        for (int target : new int[] { inventory.getSelectedSlot(), Inventory.SLOT_OFFHAND }) {
            var held = inventory.getItem(target);
            if (target == source || held.isEmpty()) continue;
            EnergyHandler to = ItemAccess.forPlayerSlot(player, target).getCapability(Capabilities.Energy.ITEM);
            if (to != null && transfer(from, to)) return;
        }
    }

    /**
     * Moves as much FE as the target takes and the source gives.
     *
     * @param from the battery
     * @param to   the item to charge
     * @return whether any FE moved
     */
    private static boolean transfer(EnergyHandler from, EnergyHandler to) {
        int accepted;
        try (var probe = Transaction.openRoot()) { // rolled back: how much the target takes of what the battery gives
            accepted = to.insert(from.extract(Integer.MAX_VALUE, probe), probe);
        }
        if (accepted <= 0) return false;
        try (var tx = Transaction.openRoot()) {
            if (from.extract(accepted, tx) != accepted || to.insert(accepted, tx) != accepted) return false;
            tx.commit();
            return true;
        }
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
