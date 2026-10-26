package net.samtrion.dotcraft.resources.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.samtrion.dotcraft.resources.CraftResources;
import net.samtrion.dotcraft.resources.common.CompressedBase;
import net.samtrion.dotcraft.resources.common.CompressedBases;
import net.samtrion.dotcraft.resources.block.ResourceBlocks;
import net.samtrion.dotcraft.resources.common.CompressedLook;
import net.samtrion.dotcraft.resources.common.CompressedTier;

/**
 * Colors the tinted layers of grass-like compressed blocks in the world with the fixed tint of their base. The
 * item side lives in the generated item definitions.
 */
@EventBusSubscriber(modid = CraftResources.MODID, value = Dist.CLIENT)
public final class CompressedTints {
    private CompressedTints() {}

    /**
     * Registers one constant tint source per tint color for all blocks of the grass-like bases.
     *
     * @param event the block tint registration event
     */
    @SubscribeEvent
    static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        Map<Integer, List<Block>> byColor = CompressedBases.enabled()
                .filter(base -> base.look == CompressedLook.GRASS)
                .collect(Collectors.groupingBy(CompressedBase::tint, Collectors.flatMapping(
                        base -> java.util.stream.IntStream.rangeClosed(1, CompressedTier.MAX)
                                .mapToObj(tier -> (Block) ResourceBlocks.block(base, tier).get()),
                        Collectors.toList())));
        byColor.forEach((color, blocks) ->
                event.register(List.of(BlockTintSources.constant(color)), blocks.toArray(Block[]::new)));
    }
}
