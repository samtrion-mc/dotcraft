/**
 * The kite wind generator: a winch block (tiers 1-5) and a kite item. A kite put into the winch flies as a real entity on a tether;
 * the output grows with its altitude above the block, the free air around it and the weather (see {@code KiteMath}).
 *
 * <h2>Spike: what the entity and render API of Minecraft 26.1 allows (read from the sources)</h2>
 * <ul>
 * <li><b>Height.</b> Entities are not limited by the build height. {@code Entity.checkBelowWorld} only removes an entity below
 * {@code minY - 64}; {@code setPos} clamps the y coordinate only at +-2e7; entity sections are keyed by section coordinates without
 * a range check. So a kite can hang above the top of the world. Entity ticking and tracking depend on the chunk column, not on y,
 * which is why the kite stays in the x/z column of its block. A game test places the kite above {@code getMaxY()} and checks that
 * it lives, ticks and gives FE.</li>
 * <li><b>Persistence.</b> The entity type is {@code noSave}; the block entity saves a flag and respawns the kite when its chunk
 * ticks. So a save/load, a chunk unload or a crash can never leave a stray kite, and a kite whose block is gone (or that is not the
 * block's current kite) discards itself within a second. Removing the block drops the kite item (and discards the entity).</li>
 * <li><b>Chunks.</b> No world read loads a chunk: every sample is guarded by {@code hasChunkAt}; an unloaded position counts as free.</li>
 * <li><b>Rendering.</b> An {@code EntityRenderer} with an own render state; the quad and the tether are submitted as custom geometry
 * ({@code SubmitNodeCollector.submitCustomGeometry}, render types {@code entityCutout} and {@code lines}, as the fishing hook does);
 * {@code affectedByCulling} is off and {@code Entity.shouldRender} is widened, so the tether does not vanish when the kite is far
 * above the player. The renderer is registered in a {@code Dist.CLIENT} subscriber only.</li>
 * <li><b>Thunder.</b> No lightning is ever spawned; a kite that cannot fly in a thunderstorm is reeled in.</li>
 * </ul>
 */
@NullMarked
package net.samtrion.dotcraft.energy.kite;

import org.jspecify.annotations.NullMarked;
