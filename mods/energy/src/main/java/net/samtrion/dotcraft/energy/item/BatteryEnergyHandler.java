package net.samtrion.dotcraft.energy.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The FE storage of a battery stack, kept in an integer data component. A single-use battery names a {@code spent}
 * item that replaces it when the last FE is drawn; a rechargeable one has none and stays what it is.
 */
public final class BatteryEnergyHandler extends ItemAccessEnergyHandler {
    private final @Nullable Item spent;

    /**
     * Creates the handler.
     *
     * @param itemAccess      the access to the battery stack
     * @param energyComponent the data component that holds the stored FE
     * @param capacity        the capacity in FE
     * @param maxInsert       the most FE accepted per insert, 0 for a battery that cannot be charged
     * @param maxExtract      the most FE given per extract
     * @param spent           the item the battery turns into when it is empty, {@code null} to stay as it is
     */
    public BatteryEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity,
            int maxInsert, int maxExtract, @Nullable Item spent) {
        super(itemAccess, energyComponent, capacity, maxInsert, maxExtract);
        this.spent = spent;
    }

    /**
     * The stored FE of a stack; a stack without the component is full, one that holds more than the capacity (after the
     * config was lowered) counts as full.
     *
     * @param accessResource the current resource
     * @return the stored FE, 0 for another item
     */
    @Override
    protected int getAmountFrom(ItemResource accessResource) {
        if (!accessResource.is(validItem)) return 0;
        return Math.min(capacity, accessResource.getOrDefault(energyComponent, capacity));
    }

    /**
     * The resource with the new amount, or the {@code spent} item once a single-use battery runs dry.
     *
     * @param accessResource the current resource
     * @param newAmount      the new amount of FE
     * @return the updated resource, or the plain spent item at 0 FE
     */
    @Override
    protected ItemResource update(ItemResource accessResource, int newAmount) {
        if (newAmount == 0 && spent != null) return ItemResource.of(spent);
        return super.update(accessResource, newAmount);
    }
}
