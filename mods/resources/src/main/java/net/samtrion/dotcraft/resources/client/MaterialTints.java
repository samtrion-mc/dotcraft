package net.samtrion.dotcraft.resources.client;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Materials;

/**
 * Colors the grayscale templates of the material blocks in the world with the color of their material; the
 * models tint layer 0. The item side lives in the generated item definitions.
 */
@EventBusSubscriber(modid = CraftResources.MODID, value = Dist.CLIENT)
public final class MaterialTints {
    /** Not instantiable. */
    private MaterialTints() {}

    /**
     * Registers one constant tint source per material for all its blocks.
     *
     * @param event the block tint registration event
     */
    @SubscribeEvent
    static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        for (var material : Materials.all()) {
            Block[] blocks = MaterialBlocks.tintedBlocks(material).map(b -> (Block) b.get()).toArray(Block[]::new);
            if (blocks.length > 0) event.register(java.util.List.of(BlockTintSources.constant(material.color())), blocks);
        }
    }
}
