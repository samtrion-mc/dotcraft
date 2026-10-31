package net.samtrion.dotcraft.energy.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.EnergyStartupConfig;

/** Unit tests for the battery config defaults and the item bar of {@link BatteryItem}. */
class EnergyItemsTest {
    /** Without a loaded config the batteries keep the specified defaults and are enabled. */
    @Test
    void configDefaults() {
        var potato = EnergyCommonConfig.POTATO_BATTERY;
        var rechargeable = EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY;
        assertTrue(EnergyStartupConfig.enabled("potato_battery") && EnergyStartupConfig.enabled("rechargeable_potato_battery"));
        assertEquals(4000, potato.capacity());
        assertEquals(0, potato.maxInput());
        assertEquals(40, potato.maxOutput());
        assertEquals(20000, rechargeable.capacity());
        assertEquals(100, rechargeable.maxInput());
        assertEquals(100, rechargeable.maxOutput());
    }

    /** Tiers 1-5 scale the tier below by 2, 2.5, 7.5, 10 and 10 times (capacity and rates), and are enabled. */
    @Test
    void tierDefaults() {
        var tiers = new EnergyCommonConfig.Battery[] {EnergyCommonConfig.RECHARGEABLE_POTATO_BATTERY, EnergyCommonConfig.BATTERY_TIER_1,
                EnergyCommonConfig.BATTERY_TIER_2, EnergyCommonConfig.BATTERY_TIER_3, EnergyCommonConfig.BATTERY_TIER_4, EnergyCommonConfig.BATTERY_TIER_5};
        double[] factor = {2, 2.5, 7.5, 10, 10};
        for (int i = 1; i < tiers.length; i++) {
            assertEquals((long) (tiers[i - 1].capacity() * factor[i - 1]), tiers[i].capacity(), "capacity tier " + i);
            assertEquals((long) (tiers[i - 1].maxInput() * factor[i - 1]), tiers[i].maxInput(), "maxInput tier " + i);
            assertEquals((long) (tiers[i - 1].maxOutput() * factor[i - 1]), tiers[i].maxOutput(), "maxOutput tier " + i);
        }
        for (int i = 1; i <= 5; i++) assertTrue(EnergyStartupConfig.enabled("battery_tier_" + i));
    }

    /** The tooltip key lives in the namespace of the mod. */
    @Test
    void tooltipKey() {
        assertTrue(EnergyItems.TOOLTIP_KEY.startsWith("tooltip.dotcraftenergy."), EnergyItems.TOOLTIP_KEY);
    }

    /** The bar is empty at 0 FE, full at the capacity and in between proportional. */
    @Test
    void barWidth() {
        assertEquals(0, BatteryBar.width(0, 4000));
        assertEquals(13, BatteryBar.width(4000, 4000));
        assertEquals(7, BatteryBar.width(2000, 4000));
        assertEquals(13, BatteryBar.width(30000, 20000));
    }
}
