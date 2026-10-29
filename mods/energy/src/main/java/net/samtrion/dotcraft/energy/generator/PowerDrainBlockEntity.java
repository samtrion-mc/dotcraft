package net.samtrion.dotcraft.energy.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The block entity of the {@link PowerDrainBlock}: every tick it pulls what the six neighbours give, limited by their own config, and destroys it. */
public class PowerDrainBlockEntity extends BlockEntity {
    /**
     * Creates the block entity.
     *
     * @param type  its type
     * @param pos   the position
     * @param state the state
     */
    public PowerDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * One server tick: drains every neighbour that has an FE capability on its side towards the drain.
     *
     * @param level the level
     */
    void serverTick(ServerLevel level) {
        for (Direction d : Direction.values()) {
            PowerDrain.pull(level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(d), d.getOpposite()));
        }
    }
}
