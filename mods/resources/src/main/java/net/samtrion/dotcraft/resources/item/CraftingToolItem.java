package net.samtrion.dotcraft.resources.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A tool such as the hammer that stays in the crafting grid: every craft that uses it gives it back with one more
 * point of damage, and the last point breaks it. The recipes only name its tag, nothing else is needed to use it up
 * slowly. The durability is a property of the item, see {@link Item.Properties#durability(int)}.
 */
public final class CraftingToolItem extends Item {
    /**
     * Creates the item.
     *
     * @param properties the properties, with the maximum damage
     */
    public CraftingToolItem(Properties properties) {
        super(properties);
    }

    /**
     * The damaged tool that a craft leaves in the grid.
     *
     * @param instance the tool that took part in the craft; a template (a recipe display) is shown unchanged
     * @return the tool with one more damage, {@code null} where that was the last point and it breaks
     */
    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        if (!(instance instanceof ItemStack stack)) return ItemStackTemplate.fromNonEmptyStack(new ItemStack(this));
        if (stack.getDamageValue() + 1 >= stack.getMaxDamage()) return null;
        ItemStack worn = stack.copyWithCount(1);
        worn.setDamageValue(stack.getDamageValue() + 1);
        return ItemStackTemplate.fromNonEmptyStack(worn);
    }
}
