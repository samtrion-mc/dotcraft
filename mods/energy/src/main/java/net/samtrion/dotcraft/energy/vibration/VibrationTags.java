package net.samtrion.dotcraft.energy.vibration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The tags of the vibration generators, a data file so data packs can tune them. */
public final class VibrationTags {
    /**
     * The game events a vibration generator takes energy from: shocks that need work (blocks broken and placed, explosions, deaths,
     * damage, lightning, landings and steps). What is cheap to repeat is left out: containers, doors, levers, note blocks, pistons,
     * mounting, equipping, fluids, teleports, projectiles (shot and landed) and entities placed. A machine can still loop some of the
     * events that are in it (a dispenser breaking blocks, a grinder dealing damage, a pen of mobs walking), so the economy does not
     * rely on the tag: the {@link VibrationBucket} caps what any loop makes at {@link VibrationMath#duty} of the peak.
     */
    public static final TagKey<GameEvent> SOURCES = TagKey.create(Registries.GAME_EVENT, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "vibration_sources"));

    /**
     * The entity types whose game events are worth {@link VibrationMath#BOSS_FACTOR} times as much (the bosses: ender dragon, wither,
     * warden, elder guardian by default); a data file, so data packs can add or remove bosses. The event must be caused by the entity
     * (the source entity of the game event context). Boss entities are also heard when they dampen vibrations (the warden does).
     */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "vibration_boss"));

    /** Not instantiable. */
    private VibrationTags() {}
}
