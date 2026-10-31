package net.samtrion.dotcraft.jei.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.wheel.WheelBlocks;
import net.samtrion.dotcraft.energy.wheel.WheelMath;
import net.samtrion.dotcraft.jei.JeiMod;

/**
 * The JEI category "Mob wheel yield": one page per mob type that a mob wheel can hold, with its weight and the FE/t every wheel tier
 * makes of it while the mob runs and while it stands. The wheels are the catalysts, so the page opens from the wheel ("uses") and
 * from the spawn egg of the mob ("recipes"). The numbers come from {@link WheelMath} and the peaks of the config, through
 * {@link WheelYield}. This class links the energy mod and is only loaded when that mod is present.
 */
final class WheelYieldCategory implements IRecipeCategory<WheelYield.Entry> {
    /** The recipe type of the category. */
    static final IRecipeType<WheelYield.Entry> TYPE = IRecipeType.create(JeiMod.MODID, "wheel_yield", WheelYield.Entry.class);

    private static final int WIDTH = 170;
    private static final int HEIGHT = 84;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;

    /**
     * Creates the category.
     *
     * @param gui the JEI drawing helpers
     */
    WheelYieldCategory(IGuiHelper gui) {
        var wheel = WheelBlocks.items().findFirst();
        this.icon = gui.createDrawableItemStack(wheel.isPresent() ? new ItemStack(wheel.get().get()) : new ItemStack(net.minecraft.world.item.Items.SADDLE));
    }

    /**
     * Builds the entries of every mob type that is eligible for a wheel: it has default attributes of a {@code Mob} (the follow range
     * tells a mob from an armor stand or a player), is not a player and is not in the config list {@code wheel.blocked_mobs}. The list
     * falls back to its defaults when the config is not loaded.
     *
     * @return the entries, heaviest first
     */
    static List<WheelYield.Entry> entries() {
        var mobs = new ArrayList<WheelYield.MobStats>();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var stats = stats(type);
            if (stats != null) mobs.add(stats);
        }
        var peaks = EnergyCommonConfig.MOB_WHEELS.stream().map(g -> g.peak()).toList();
        return WheelYield.entries(mobs, peaks, WheelMath::weight, WheelMath::share);
    }

    @SuppressWarnings("unchecked")
    private static WheelYield.@Nullable MobStats stats(EntityType<?> type) {
        if (type == EntityType.PLAYER || !DefaultAttributes.hasSupplier((EntityType<? extends net.minecraft.world.entity.LivingEntity>) type)) return null;
        var supplier = DefaultAttributes.getSupplier((EntityType<? extends net.minecraft.world.entity.LivingEntity>) type);
        if (!supplier.hasAttribute(Attributes.FOLLOW_RANGE) || EnergyCommonConfig.WHEEL_BLOCKED_MOBS.filter().matches(type)) return null;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return new WheelYield.MobStats(id.toString(), supplier.getBaseValue(Attributes.MAX_HEALTH), supplier.getBaseValue(Attributes.MOVEMENT_SPEED));
    }

    /** The wheel items, the catalysts of the category. */
    static Stream<ItemStack> wheels() {
        return WheelBlocks.items().map(i -> new ItemStack(i.get()));
    }

    private static @Nullable EntityType<?> type(WheelYield.Entry entry) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entry.id())).orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    public IRecipeType<WheelYield.Entry> getRecipeType() {
        return TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public Component getTitle() {
        return Component.translatable(WheelYield.TITLE_KEY);
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    /** {@inheritDoc} */
    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    /** The spawn egg of the mob is its input, so "recipes" on the egg opens the page; a mob without an egg has the page only on the wheel. */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WheelYield.Entry entry, IFocusGroup focuses) {
        var type = type(entry);
        var egg = type == null ? null : SpawnEggItem.byId(type);
        if (egg != null && egg.isPresent()) builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).add(VanillaTypes.ITEM_STACK, new ItemStack(egg.get()));
    }

    /** The name of the mob, its weight and the yield of the five tiers while it runs and while it stands. */
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, WheelYield.Entry entry, IFocusGroup focuses) {
        var type = type(entry);
        var name = type == null ? Component.literal(entry.id()) : type.getDescription();
        var weights = entry.weights();
        // tier 1 and 2 share one weight; tiers 3 and up lift light mobs to the reference weight, shown only when it differs
        var lines = new ArrayList<net.minecraft.network.chat.FormattedText>();
        lines.add(Component.translatable(WheelYield.WEIGHT_KEY, String.format(java.util.Locale.ROOT, "%.2f", weights.get(0))));
        if (Math.abs(weights.get(WheelMath.SMALL_FULL_TIER - 1) - weights.get(0)) > 1e-9) {
            lines.add(Component.translatable(WheelYield.WEIGHT_LIGHT_KEY, String.format(java.util.Locale.ROOT, "%.2f", weights.get(WheelMath.SMALL_FULL_TIER - 1))));
        }
        lines.add(Component.translatable(WheelYield.RUNNING_KEY, WheelYield.perTier(entry.running())));
        lines.add(Component.translatable(WheelYield.STANDING_KEY, WheelYield.perTier(entry.standing())));
        builder.addText(name, WIDTH - 24, 12).setPosition(24, 2).setColor(TEXT_COLOR).setShadow(false);
        builder.addText(lines, WIDTH, HEIGHT - 22).setPosition(0, 22).setColor(TEXT_COLOR).setShadow(false);
    }
}
