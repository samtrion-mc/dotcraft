package net.samtrion.dotcraft.jade.energy;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Base of the Jade providers of the generator families: this class is the server half and reads the block entity into a
 * tag; {@link #client()} is the separate client half that turns the tag into lines (Jade rejects a data provider that is
 * also a component provider). Everything the client shows is therefore a value the server computed, so the
 * lines are right in multiplayer too. The common lines (output and stored energy) are written here.
 *
 * @param <E> the block entity class of the family
 */
abstract class FamilyProvider<E extends BlockEntity> implements IServerDataProvider<BlockAccessor> {
    private final Identifier uid;
    private final Class<E> entity;
    private final Class<? extends Block> block;

    /**
     * Creates a provider.
     *
     * @param family one of {@link JadeKeys#FAMILIES}, the path of the UID
     * @param entity the block entity class the provider reads
     * @param block  the block class whose blocks get the lines (all tiers)
     */
    FamilyProvider(String family, Class<E> entity, Class<? extends Block> block) {
        this.uid = Identifier.fromNamespaceAndPath(JadeKeys.ENERGY, family);
        this.entity = entity;
        this.block = block;
    }

    /** @return the block entity class this provider is registered for */
    final Class<E> entityClass() {
        return entity;
    }

    /** @return the block class the lines are shown for */
    final Class<? extends Block> blockClass() {
        return block;
    }

    @Override
    public final Identifier getUid() {
        return uid;
    }

    /**
     * Reads the block entity on the server; skips a block entity of another class.
     *
     * @param data     the tag sent to the client
     * @param accessor the looked-at block
     */
    @Override
    public final void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel level) || !entity.isInstance(accessor.getBlockEntity())) return;
        collect(data, level, entity.cast(accessor.getBlockEntity()));
    }

    /** @return the client half, registered on the block; shares the UID so the config toggle covers both halves */
    final IBlockComponentProvider client() {
        return new Client();
    }

    /** The client half: adds the lines of the tag the server sent. */
    private final class Client implements IBlockComponentProvider {
        @Override
        public Identifier getUid() {
            return uid;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            var data = accessor.getServerData();
            if (data.isEmpty() || !data.contains("fe_stored")) return;
            boolean details = accessor.showDetails();
            show(tooltip, data, details);
            tooltip.add(Component.translatable(JadeKeys.STORED, number(data.getLongOr("fe_stored", 0)), number(data.getLongOr("fe_capacity", 0))));
            if (!details) tooltip.add(Component.translatable(JadeKeys.DETAILS_HINT, Component.translatable("key.jade.show_details")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Reads the block entity into the tag.
     *
     * @param data  the tag
     * @param level the level
     * @param be    the block entity
     */
    abstract void collect(CompoundTag data, ServerLevel level, E be);

    /**
     * Adds the lines of the family; the stored energy and, without the details, the hint are added after them. Only
     * what matters at a glance (output now, the one reason that limits the block) is added always; every other
     * line is added only when {@code details} is set.
     *
     * @param tooltip the tooltip
     * @param data    the tag {@link #collect} wrote
     * @param details whether Jade's details key is held ({@link BlockAccessor#showDetails()})
     */
    abstract void show(ITooltip tooltip, CompoundTag data, boolean details);

    /**
     * Writes the stored energy and the capacity of a buffer.
     *
     * @param data   the tag
     * @param energy the buffer
     */
    static void putEnergy(CompoundTag data, EnergyHandler energy) {
        data.putLong("fe_stored", energy.getAmountAsLong());
        data.putLong("fe_capacity", energy.getCapacityAsLong());
    }

    /**
     * Writes the output now and at most.
     *
     * @param data the tag
     * @param now  the FE/t now
     * @param max  the FE/t at most
     */
    static void putOutput(CompoundTag data, double now, double max) {
        data.putDouble("now", now);
        data.putDouble("max", max);
    }

    /**
     * Adds the output line from {@link #putOutput}: the output now always, the peak only with the details.
     *
     * @param tooltip the tooltip
     * @param data    the tag
     * @param details whether the details are shown
     */
    static void showOutput(ITooltip tooltip, CompoundTag data, boolean details) {
        tooltip.add(Component.translatable(JadeKeys.OUTPUT, number(data.getDoubleOr("now", 0))));
        if (details) tooltip.add(Component.translatable(JadeKeys.OUTPUT_MAX, number(data.getDoubleOr("max", 0))));
    }

    /**
     * Formats a number: whole numbers without a decimal, others with one.
     *
     * @param value the value
     * @return the text
     */
    static String number(double value) {
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%,.0f", value) : String.format(Locale.ROOT, "%,.1f", value);
    }

    /**
     * Formats a share as a percentage.
     *
     * @param share 1 is 100 %
     * @return the text
     */
    static String percent(double share) {
        return Math.round(share * 100) + "%";
    }
}
