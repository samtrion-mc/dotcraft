package net.samtrion.dotcraft.jade.energy;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.samtrion.dotcraft.energy.EnergyCommonConfig;
import net.samtrion.dotcraft.energy.flow.FlowGeneratorBlock;
import net.samtrion.dotcraft.energy.flow.FlowGeneratorBlockEntity;
import net.samtrion.dotcraft.energy.flow.FlowMath;
import net.samtrion.dotcraft.energy.flow.FlowScan;
import net.samtrion.dotcraft.energy.generator.GeneratorBlock;
import net.samtrion.dotcraft.energy.generator.GeneratorBlockEntity;
import net.samtrion.dotcraft.energy.generator.GeneratorMath;
import net.samtrion.dotcraft.energy.generator.SolarKind;
import net.samtrion.dotcraft.energy.kite.KiteBlockEntity;
import net.samtrion.dotcraft.energy.kite.KiteMath;
import net.samtrion.dotcraft.energy.kite.KiteWindBlock;
import net.samtrion.dotcraft.energy.melt.MeltBlock;
import net.samtrion.dotcraft.energy.melt.MeltBlockEntity;
import net.samtrion.dotcraft.energy.melt.MeltMath;
import net.samtrion.dotcraft.energy.thermo.ThermoBlock;
import net.samtrion.dotcraft.energy.thermo.ThermoBlockEntity;
import net.samtrion.dotcraft.energy.thermo.ThermoMath;
import net.samtrion.dotcraft.energy.vibration.VibrationBlock;
import net.samtrion.dotcraft.energy.vibration.VibrationBlockEntity;
import net.samtrion.dotcraft.energy.vibration.VibrationMath;
import net.samtrion.dotcraft.energy.wheel.WheelBlock;
import net.samtrion.dotcraft.energy.wheel.WheelBlockEntity;
import net.samtrion.dotcraft.energy.wheel.WheelMath;
import snownee.jade.api.ITooltip;

/**
 * The Jade providers of the energy mod's generator families, one per family. Links the energy mod, so it is only
 * loaded when that mod is present. Each provider reads only public accessors and static helpers of the energy mod.
 */
final class EnergyProviders {
    /** Not instantiable. */
    private EnergyProviders() {}

    /** @return one provider per family, in the order of {@link JadeKeys#FAMILIES} */
    static List<FamilyProvider<?>> all() {
        return List.of(new Panel(), new Flow(), new Vibration(), new Thermo(), new Melt(), new Wheel(), new Kite());
    }

    /** Solar and lunar panels: sun or moon, weather, the group and its pool, shading, water. */
    static final class Panel extends FamilyProvider<GeneratorBlockEntity> {
        Panel() {
            super("panel", GeneratorBlockEntity.class, GeneratorBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, GeneratorBlockEntity be) {
            var state = be.getBlockState();
            var block = (GeneratorBlock) state.getBlock();
            int tier = block.tier();
            var above = be.getBlockPos().above();
            float sun = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, be.getBlockPos());
            boolean solar = block.kind() instanceof SolarKind;
            boolean raining = level.isRainingAt(above);
            var weather = !raining ? GeneratorMath.Weather.CLEAR : level.isThundering() ? GeneratorMath.Weather.THUNDER : GeneratorMath.Weather.RAIN;
            putEnergy(data, be.energy());
            putOutput(data, be.output(), block.kind().configs().get(tier - 1).peak());
            data.putBoolean("solar", solar);
            data.putFloat("height", GeneratorMath.tracked(solar ? GeneratorMath.daylight(sun) : GeneratorMath.moonlight(sun), tier));
            data.putBoolean("sky", level.dimensionType().hasSkyLight() && level.canSeeSky(above));
            data.putString("weather", weather.name());
            data.putFloat("weather_share", GeneratorMath.weather(tier, weather));
            data.putInt("group", be.groupSize());
            data.putBoolean("occupied", be.isOccupied());
            data.putBoolean("water", state.getValue(GeneratorBlock.WATERLOGGED));
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            showOutput(tooltip, data);
            if (!data.getBooleanOr("sky", false)) {
                tooltip.add(Component.translatable(JadeKeys.PANEL_NO_SKY));
            } else {
                tooltip.add(Component.translatable(data.getBooleanOr("solar", true) ? JadeKeys.PANEL_SUN : JadeKeys.PANEL_MOON, percent(data.getFloatOr("height", 0))));
                var share = percent(data.getFloatOr("weather_share", 1));
                switch (GeneratorMath.Weather.valueOf(data.getStringOr("weather", "CLEAR"))) {
                    case CLEAR -> tooltip.add(Component.translatable(JadeKeys.PANEL_WEATHER_CLEAR));
                    case RAIN -> tooltip.add(Component.translatable(JadeKeys.PANEL_WEATHER_RAIN, share));
                    case THUNDER -> tooltip.add(Component.translatable(JadeKeys.PANEL_WEATHER_THUNDER, share));
                }
            }
            int group = data.getIntOr("group", 1);
            if (group > 1) {
                tooltip.add(Component.translatable(JadeKeys.PANEL_GROUP, group, number(data.getLongOr("fe_stored", 0)), number(data.getLongOr("fe_capacity", 0))));
            }
            if (data.getBooleanOr("occupied", false)) tooltip.add(Component.translatable(JadeKeys.PANEL_OCCUPIED).withStyle(ChatFormatting.RED));
            if (data.getBooleanOr("water", false)) tooltip.add(Component.translatable(JadeKeys.PANEL_WATERLOGGED, percent(GeneratorMath.WATERLOGGED_FACTOR)));
        }
    }

    /** Flow generators: water share, biome, fauna and whether the axis is free. */
    static final class Flow extends FamilyProvider<FlowGeneratorBlockEntity> {
        Flow() {
            super("flow", FlowGeneratorBlockEntity.class, FlowGeneratorBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, FlowGeneratorBlockEntity be) {
            var state = be.getBlockState();
            int tier = ((FlowGeneratorBlock) state.getBlock()).tier();
            var pos = be.getBlockPos();
            var reading = FlowScan.scan(level, pos, tier);
            putEnergy(data, be.energy());
            putOutput(data, be.output(), EnergyCommonConfig.FLOW_GENERATORS.get(tier - 1).peak() * (1 + FlowMath.FAUNA_MAX));
            data.putBoolean("wet", state.getValue(FlowGeneratorBlock.WATERLOGGED));
            data.putBoolean("blocked", FlowScan.axisBlocked(level, pos, state.getValue(FlowGeneratorBlock.FACING)));
            data.putDouble("share", FlowMath.waterShare(reading.sources(), reading.flowing(), tier));
            data.putInt("sources", reading.sources());
            data.putInt("flowing", reading.flowing());
            data.putFloat("biome", FlowMath.biome(tier, reading.primeBiome()));
            data.putInt("fauna", reading.faunaTypes());
            data.putFloat("bonus", FlowMath.fauna(reading.faunaTypes()));
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            showOutput(tooltip, data);
            if (!data.getBooleanOr("wet", false)) tooltip.add(Component.translatable(JadeKeys.FLOW_DRY).withStyle(ChatFormatting.RED));
            if (data.getBooleanOr("blocked", false)) tooltip.add(Component.translatable(JadeKeys.FLOW_BLOCKED).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(JadeKeys.FLOW_WATER, percent(data.getDoubleOr("share", 0)), data.getIntOr("sources", 0), data.getIntOr("flowing", 0)));
            tooltip.add(Component.translatable(JadeKeys.FLOW_BIOME, percent(data.getFloatOr("biome", 1))));
            tooltip.add(Component.translatable(JadeKeys.FLOW_FAUNA, data.getIntOr("fauna", 0), percent(data.getFloatOr("bonus", 0))));
        }
    }

    /** Vibration generators: the bucket, the peak and the average ceiling, the hearing, the last counted event. */
    static final class Vibration extends FamilyProvider<VibrationBlockEntity> {
        Vibration() {
            super("vibration", VibrationBlockEntity.class, VibrationBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, VibrationBlockEntity be) {
            int tier = be.tier();
            int peak = EnergyCommonConfig.VIBRATION_GENERATORS.get(tier - 1).peak();
            putEnergy(data, be.energy());
            data.putLong("tokens", be.bucket().tokens());
            data.putLong("burst", VibrationMath.burst(peak));
            data.putInt("peak", peak);
            data.putDouble("average", peak * VibrationMath.duty(tier));
            data.putInt("radius", VibrationMath.radius(tier));
            data.putInt("frequency", VibrationMath.minFrequency(tier));
            var last = be.lastEvent();
            if (last != null) {
                data.putString("last_event", last);
                data.putLong("last_event_age", Math.max(0, level.getGameTime() - be.lastEventTick()));
            }
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable(JadeKeys.VIBRATION_PEAK, number(data.getIntOr("peak", 0)), number(data.getDoubleOr("average", 0))));
            tooltip.add(Component.translatable(JadeKeys.VIBRATION_BUCKET, number(data.getLongOr("tokens", 0)), number(data.getLongOr("burst", 0))));
            tooltip.add(Component.translatable(JadeKeys.VIBRATION_HEARING, data.getIntOr("radius", 0), data.getIntOr("frequency", 0)));
            var last = data.getStringOr("last_event", "");
            if (last.isEmpty()) tooltip.add(Component.translatable(JadeKeys.VIBRATION_LAST_EVENT_NONE));
            else tooltip.add(Component.translatable(JadeKeys.VIBRATION_LAST_EVENT, last, data.getLongOr("last_event_age", 0) / 20));
        }
    }

    /** Thermoelectric generators: heat and cold level, the difference and its share of the peak. */
    static final class Thermo extends FamilyProvider<ThermoBlockEntity> {
        Thermo() {
            super("thermo", ThermoBlockEntity.class, ThermoBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, ThermoBlockEntity be) {
            int tier = ((ThermoBlock) be.getBlockState().getBlock()).tier();
            putEnergy(data, be.energy());
            putOutput(data, be.output(), EnergyCommonConfig.THERMO_GENERATORS.get(tier - 1).peak());
            data.putInt("heat", be.heatLevel());
            data.putInt("cold", be.coldLevel());
            data.putInt("delta", ThermoMath.delta(be.heatLevel(), be.coldLevel()));
            data.putInt("ceiling", ThermoMath.ceiling(tier));
            data.putFloat("share", be.share());
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            showOutput(tooltip, data);
            if (data.getIntOr("heat", 0) == 0 || data.getIntOr("cold", 0) == 0) {
                tooltip.add(Component.translatable(JadeKeys.THERMO_NONE));
                return;
            }
            tooltip.add(Component.translatable(JadeKeys.THERMO_LEVELS, data.getIntOr("heat", 0), data.getIntOr("cold", 0), data.getIntOr("delta", 0),
                    data.getIntOr("ceiling", 0), percent(data.getFloatOr("share", 0))));
        }
    }

    /** Melt generators: the ice reservoir, the water tank and the efficiency. */
    static final class Melt extends FamilyProvider<MeltBlockEntity> {
        Melt() {
            super("melt", MeltBlockEntity.class, MeltBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, MeltBlockEntity be) {
            int tier = ((MeltBlock) be.getBlockState().getBlock()).tier();
            int peak = EnergyCommonConfig.MELT_GENERATORS.get(tier - 1).peak();
            putEnergy(data, be.energy());
            data.putInt("peak", peak);
            data.putLong("reservoir", be.pending());
            data.putLong("reservoir_max", MeltMath.reserve(peak));
            data.putBoolean("has_tank", MeltMath.tier(tier).water());
            data.putInt("water", be.water());
            data.putBoolean("full", be.tankFull());
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable(JadeKeys.PEAK, number(data.getIntOr("peak", 0))));
            tooltip.add(Component.translatable(JadeKeys.MELT_RESERVOIR, number(data.getLongOr("reservoir", 0)), number(data.getLongOr("reservoir_max", 0))));
            if (!data.getBooleanOr("has_tank", false)) return;
            tooltip.add(Component.translatable(JadeKeys.MELT_TANK, number(data.getIntOr("water", 0)), number(MeltMath.TANK_CAPACITY_MB)));
            if (data.getBooleanOr("full", false)) {
                tooltip.add(Component.translatable(JadeKeys.MELT_EFFICIENCY_FULL, percent(MeltMath.FULL_TANK_EFFICIENCY)).withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable(JadeKeys.MELT_EFFICIENCY, percent(1)));
            }
        }
    }

    /** Mob wheels: whether the wheel turns, its output, the captured mob, its weight and whether it runs or stands. */
    static final class Wheel extends FamilyProvider<WheelBlockEntity> {
        Wheel() {
            super("wheel", WheelBlockEntity.class, WheelBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, WheelBlockEntity be) {
            int tier = ((WheelBlock) be.getBlockState().getBlock()).tier();
            putEnergy(data, be.energy());
            putOutput(data, be.output(), EnergyCommonConfig.MOB_WHEELS.get(tier - 1).peak());
            var mob = be.heldMob();
            if (mob != null) {
                data.putString("mob", mob.getType().getDescriptionId());
                data.putDouble("weight", WheelMath.weight(mob.getMaxHealth(), mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), tier));
                data.putBoolean("trying", be.running());
            }
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            showOutput(tooltip, data);
            tooltip.add(Component.translatable(data.getDoubleOr("now", 0) > 0 ? JadeKeys.WHEEL_RUNNING : JadeKeys.WHEEL_IDLE));
            var mob = data.getStringOr("mob", "");
            if (mob.isEmpty()) {
                tooltip.add(Component.translatable(JadeKeys.WHEEL_NO_MOB));
                return;
            }
            tooltip.add(Component.translatable(JadeKeys.WHEEL_MOB, Component.translatable(mob), number(Math.round(data.getDoubleOr("weight", 0) * 100) / 100.0)));
            tooltip.add(data.getBooleanOr("trying", false) ? Component.translatable(JadeKeys.WHEEL_ACTIVITY_RUNNING)
                    : Component.translatable(JadeKeys.WHEEL_ACTIVITY_STANDING, percent(WheelMath.STANDING_FACTOR)));
        }
    }

    /** Kite wind generators: the status, wire, length factor, and the distance to a kite generator that is too close. */
    static final class Kite extends FamilyProvider<KiteBlockEntity> {
        Kite() {
            super("kite", KiteBlockEntity.class, KiteWindBlock.class);
        }

        @Override
        void collect(CompoundTag data, ServerLevel level, KiteBlockEntity be) {
            var d = be.diagnostics();
            int tier = ((KiteWindBlock) be.getBlockState().getBlock()).tier();
            putEnergy(data, be.energy());
            // the best case is the base times the factor of the full tether
            putOutput(data, d.output(), EnergyCommonConfig.KITE_GENERATORS.get(tier - 1).peak() * (double) KiteMath.tether(tier) / KiteMath.REFERENCE_LENGTH);
            data.putString("status", d.status().name().toLowerCase(Locale.ROOT));
            data.putInt("wire", d.wire());
            data.putInt("wire_capacity", d.wireCapacity());
            data.putDouble("length", d.effectiveLength());
            data.putDouble("factor", d.lengthFactor());
            data.putInt("min_distance", d.minDistance());
            data.putDouble("offender", d.nearestOffender());
            data.putInt("kites", d.kites());
            data.putString("limit", d.limit());
        }

        @Override
        void show(ITooltip tooltip, CompoundTag data) {
            var status = data.getStringOr("status", "no_kite");
            int minDistance = data.getIntOr("min_distance", KiteMath.MIN_DISTANCE);
            double offender = data.getDoubleOr("offender", -1);
            if (status.equals("too_close")) {
                var line = offender >= 0 ? Component.translatable(JadeKeys.KITE_TOO_CLOSE, number(Math.round(offender * 10) / 10.0), minDistance)
                        : Component.translatable(JadeKeys.KITE_TOO_CLOSE_UNKNOWN, minDistance);
                tooltip.add(line.withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                var line = Component.translatable(JadeKeys.kiteStatus(status));
                tooltip.add(status.equals("no_wire") || status.equals("weather_reeled") || status.equals("no_sky") ? line.withStyle(ChatFormatting.YELLOW) : line);
            }
            showOutput(tooltip, data);
            tooltip.add(Component.translatable(JadeKeys.KITE_WIRE, data.getIntOr("wire", 0), data.getIntOr("wire_capacity", 0)));
            if (!status.equals("flying")) return;
            tooltip.add(Component.translatable(JadeKeys.KITE_LENGTH, number(Math.round(data.getDoubleOr("length", 0) * 10) / 10.0), percent(data.getDoubleOr("factor", 0))));
            tooltip.add(Component.translatable(JadeKeys.KITE_KITES, data.getIntOr("kites", 0)));
            var limit = data.getStringOr("limit", "none");
            if (JadeKeys.KITE_LIMITS.contains(limit)) tooltip.add(Component.translatable(JadeKeys.KITE_LIMIT, Component.translatable(JadeKeys.kiteLimit(limit))));
        }
    }
}
