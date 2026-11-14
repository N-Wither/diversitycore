package net.aquiferic.diversitycore.api.registry;

import java.util.Optional;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Frozen runtime configuration shared by the root and its optional upper block. */
public record CropSettings(int stages, int upperStages, Optional<HarvestMode> harvestMode,
        boolean shallowWater, Optional<TagKey<Block>> substrate,
        CropCondition survivalCondition, CropCondition growthCondition) {
}
