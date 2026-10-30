package net.samtrion.dotcraft.energy.kite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.samtrion.dotcraft.energy.EnergyMod;

/**
 * Game events of the kite generators: a natural lightning bolt that lands within {@link KiteMath#THUNDER_RADIUS} blocks of a kite or
 * its tether is credited to the generator, and the registry of loaded generators is forgotten when a server stops.
 */
@EventBusSubscriber(modid = EnergyMod.MODID)
public final class KiteEvents {
    /** Not instantiable. */
    private KiteEvents() {}

    /**
     * A bolt joins a level; this fires once per bolt, so a bolt is credited once. Bolts of our own (tagged) are skipped.
     *
     * @param event the event
     */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level) KiteBlockEntity.onBolt(level, bolt);
    }

    /**
     * A server stopped: forget the loaded generators.
     *
     * @param event the event
     */
    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        KiteBlockEntity.clearRegistry();
    }
}
