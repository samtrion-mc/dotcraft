package net.samtrion.dotcraft.energy.wheel;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * The mobs held by a mob wheel. A held mob cannot leave the plate: right after the mob has ticked (so after its own goals, its pushes
 * and its knockback have moved it) the wheel that holds it puts it back, see {@link WheelBlockEntity#hold}. Server thread only; the mobs
 * are held weakly, a wheel removes its mob itself.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
final class WheelPins {
    private static final Map<Mob, WheelBlockEntity> HELD = new WeakHashMap<>();

    /** Not instantiable. */
    private WheelPins() {}

    /**
     * Marks a mob as held by a wheel.
     *
     * @param mob   the mob
     * @param wheel the wheel that holds it
     */
    static void pin(Mob mob, WheelBlockEntity wheel) {
        HELD.put(mob, wheel);
    }

    /**
     * Lets a mob go, if the given wheel holds it.
     *
     * @param mob   the mob
     * @param wheel the wheel that held it
     */
    static void unpin(Mob mob, WheelBlockEntity wheel) {
        HELD.remove(mob, wheel);
    }

    /**
     * Holds the mob after its tick.
     *
     * @param event the tick event
     */
    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) {
            var wheel = HELD.get(mob);
            if (wheel != null) wheel.hold(mob);
        }
    }
}
