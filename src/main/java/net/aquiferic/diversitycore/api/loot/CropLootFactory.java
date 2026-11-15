package net.aquiferic.diversitycore.api.loot;

import net.minecraft.world.level.storage.loot.LootTable;

/** Invoked during data generation, once per plant part; return a fresh builder each time. */
@FunctionalInterface
public interface CropLootFactory {
    LootTable.Builder create(CropLootContext context);
}
