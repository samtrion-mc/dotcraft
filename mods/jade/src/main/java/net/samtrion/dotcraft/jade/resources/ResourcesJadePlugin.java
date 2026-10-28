package net.samtrion.dotcraft.jade.resources;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.resources.block.MaterialBlocks;
import net.samtrion.dotcraft.resources.common.Material;
import net.samtrion.dotcraft.resources.common.Materials;
import net.samtrion.dotcraft.resources.common.OreRichness;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade plugin of the resources mod; shows the richness (poor, normal, rich) of material ore blocks. Discovered by
 * Jade via {@link WailaPlugin}.
 */
@WailaPlugin
public class ResourcesJadePlugin implements IWailaPlugin, IBlockComponentProvider {
    /** Mod id of the optional resources mod. */
    private static final String RESOURCES = "dotcraftresources";
    private static final Identifier UID = Identifier.fromNamespaceAndPath(RESOURCES, "ore_richness");

    private Map<Block, OreRichness> richness;

    /**
     * Registers the ore richness tooltip for all blocks, but only when the resources mod is loaded; only ore blocks of materials get a line.
     *
     * @param registration the Jade client registration
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!ModList.get().isLoaded(RESOURCES)) return;
        registration.registerBlockComponent(this, Block.class);
    }

    /**
     * Adds the line {@code Richness: <poor|normal|rich>} when the block is a material ore.
     *
     * @param tooltip  the tooltip to append to
     * @param accessor the looked-at block
     * @param config   the plugin configuration
     */
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        OreRichness found = richness().get(accessor.getBlock());
        if (found == null) return;
        tooltip.add(Component.translatable("jade." + RESOURCES + ".ore_richness",
                Component.translatable("jade." + RESOURCES + ".ore_richness." + found.id())));
    }

    /** @return the richness of every material ore block, built on first use once all registries are filled */
    private Map<Block, OreRichness> richness() {
        if (richness == null) {
            Map<Block, OreRichness> map = new HashMap<>();
            for (Material material : Materials.all()) {
                for (Material.OreVariant v : material.oreVariants()) {
                    map.put(MaterialBlocks.ore(material, v.host(), v.richness()).get(), v.richness());
                }
            }
            richness = map;
        }
        return richness;
    }

    /**
     * {@inheritDoc}
     *
     * @return the identifier {@code dotcraftresources:ore_richness}
     */
    @Override
    public Identifier getUid() {
        return UID;
    }
}
