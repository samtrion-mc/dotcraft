package net.samtrion.dotcraft.energy.vibration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gameevent.GameEvent;
import net.samtrion.dotcraft.energy.EnergyMod;

/** The tags of the vibration generators, a data file so data packs can tune them. */
public final class VibrationTags {
    /**
     * The game events a vibration generator takes energy from: shocks that need work (blocks broken and placed, explosions, deaths,
     * damage, lightning, landings and steps). What is cheap to repeat or can be looped by a machine is left out: containers, doors,
     * levers, note blocks, pistons, mounting, equipping, fluids, teleports, projectiles shot and entities placed.
     */
    public static final TagKey<GameEvent> SOURCES = TagKey.create(Registries.GAME_EVENT, Identifier.fromNamespaceAndPath(EnergyMod.MODID, "vibration_sources"));

    /** Not instantiable. */
    private VibrationTags() {}
}
