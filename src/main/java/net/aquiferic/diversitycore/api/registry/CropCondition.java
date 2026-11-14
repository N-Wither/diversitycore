package net.aquiferic.diversitycore.api.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Evaluated at the root of the plant, on both placement and subsequent updates. */
@FunctionalInterface
public interface CropCondition {
    boolean test(LevelReader level, BlockPos rootPos, BlockState rootState);
}
