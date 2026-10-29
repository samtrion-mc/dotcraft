package net.samtrion.dotcraft.energy.kite.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.kite.KiteBlocks;

/** Client-only registration of the kite renderer; never loaded on a dedicated server. */
@EventBusSubscriber(modid = EnergyMod.MODID, value = Dist.CLIENT)
public final class KiteClient {
    /** Not instantiable. */
    private KiteClient() {}

    /**
     * Registers the renderer of the kite entity.
     *
     * @param event the renderer registration event
     */
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KiteBlocks.KITE_ENTITY.get(), KiteRenderer::new);
    }
}
