package net.aquiferic.diversitycore.api.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Implement on a Block or BlockEntity to opt into Diversity Core's Jade energy bar.
 * Called on the server; do not reference client or Jade classes here.
 * A block entity implementation takes precedence over its block implementation.
 */
public interface EnergyBarProvider {
    /** Returns an immutable snapshot, or null to retain Jade's default display. */
    @Nullable EnergyBarData getEnergyBar(Level level, BlockPos pos, BlockState state);
}
